package io.core.common.helper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

/**
 * 调用协调器：
 * - single-flight：相同 key 的请求同时只执行一次，其他调用等待并复用结果
 * - non-reentrant：可选的非重入（Drop 丢弃内层 或 Wait 串行）
 * - rate-window：短时间窗口内的触发直接复用/合并（默认 500ms，可配置）
 *
 * 重入控制与并发合并的区别：
 * | 特性 | 重入控制 | 并发合并 |
 * |-----|---------|---------|
 * | 主要目的 | 防止同一操作重复执行 | 合并相同请求减少资源消耗 |
 * | 触发条件 | 同一操作未完成时再次调用 | 多个相同请求同时发生 |
 * | 处理方式 | 拒绝或排队新请求 | 只执行一次，共享结果 |
 * | 相关方法 | nonReentrant, guardedSingleFlight, guardedSingleFlightAsync | singleFlight, singleFlightAsync |
 */
object CallCoordinator {

    // ================= 全局配置与监控 =================

    data class CoordinatorConfig(
        val defaultRateWindowMs: Long = 500L,
        val cleanupTtlMs: Long = 5 * 60 * 1000L, // 5 分钟未使用则可清理
        val cleanupIntervalMs: Long = 60 * 1000L, // 1 分钟执行一次清理
        val javaDefaultExecutor: Executor = ForkJoinPool.commonPool()
    )

    @Volatile
    var config: CoordinatorConfig = CoordinatorConfig()

    data class KeyStats(
        val key: String,
        val inFlightKotlin: Int,
        val inFlightJava: Int,
        val lastInvokedAtMs: Long
    )

    @JvmStatic
    fun snapshotStats(): List<KeyStats> {
        val keys = HashSet<String>()
        keys.addAll(flights.keys)
        keys.addAll(javaFlights.keys)
        keys.addAll(lastInvokedAt.keys)
        return keys.map { k ->
            KeyStats(
                key = k,
                inFlightKotlin = if (flights.containsKey(k)) 1 else 0,
                inFlightJava = if (javaFlights.containsKey(k)) 1 else 0,
                lastInvokedAtMs = lastInvokedAt[k]?.get() ?: javaLastInvokedAt[k]?.get() ?: 0L
            )
        }
    }

    @JvmStatic
    fun resetKey(key: String) {
        flights.remove(key)
        semaphores.remove(key)
        lastInvokedAt.remove(key)
        javaFlights.remove(key)
        javaLocks.remove(key)
        javaSerialExecutors.remove(key)?.shutdownNow()
        javaLastInvokedAt.remove(key)
    }

    // 轻量定时清理器
    private val cleaner: ScheduledExecutorService by lazy {
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "CallCoordinator-Cleaner").apply { isDaemon = true }
        }.also { exec ->
            exec.scheduleWithFixedDelay({
                try {
                    doCleanup()
                } catch (_: Throwable) {
                }
            }, config.cleanupIntervalMs, config.cleanupIntervalMs, TimeUnit.MILLISECONDS)
        }
    }

    // 启动清理器（触发 lazy 初始化与调度）
    init {
        cleaner
    }

    private fun doCleanup() {
        val ttl = config.cleanupTtlMs
        val now = System.currentTimeMillis()

        fun isExpired(last: Long?): Boolean = last == null || (now - last) > ttl

        // Kotlin 路径
        lastInvokedAt.entries.removeIf { (k, v) ->
            val expired = isExpired(v.get())
            if (expired && !flights.containsKey(k)) {
                semaphores.remove(k)
                true
            } else false
        }

        // Java 路径
        javaLastInvokedAt.entries.removeIf { (k, v) ->
            val expired = isExpired(v.get())
            if (expired && !javaFlights.containsKey(k)) {
                javaLocks.remove(k)
                javaSerialExecutors.remove(k)?.shutdownNow()
                true
            } else false
        }
        // 兜底：无 last 的孤儿结构
        semaphores.keys.removeIf { k -> lastInvokedAt[k] == null && !flights.containsKey(k) }
        javaLocks.keys.removeIf { k -> javaLastInvokedAt[k] == null && !javaFlights.containsKey(k) }
    }

    // ============= Kotlin 协程实现部分 =============

    private val flights = ConcurrentHashMap<String, CompletableDeferred<Any?>>()
    private val semaphores = ConcurrentHashMap<String, Semaphore>()
    private val lastInvokedAt = ConcurrentHashMap<String, AtomicLong>()

    // 节流用时间戳（独立于 singleFlight 的 lastInvokedAt）
    private val lastThrottledAt = ConcurrentHashMap<String, AtomicLong>()

    @JvmStatic
    val DEFAULT_RATE_WINDOW_MS: Long
        get() = config.defaultRateWindowMs

    private fun semaphoreFor(key: String): Semaphore =
        semaphores.computeIfAbsent(key) { Semaphore(1) }

    /**
     * singleFlight（Kotlin suspend）
     * - 相同 key 的并发调用只会执行一次 block，其它协程等待并复用结果
     * - rateWindowMs：默认使用全局配置；设为 0 关闭
     * - timeoutMs：可选；>0 时为执行超时
     * - retry：可选重试次数（失败后按 backoffMs 线性退避）
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> singleFlight(
        key: String,
        rateWindowMs: Long = DEFAULT_RATE_WINDOW_MS,
        timeoutMs: Long = 0L,
        retry: Int = 0,
        backoffMs: Long = 0L,
        block: suspend () -> T
    ): T {
        if (rateWindowMs > 0) {
            val now = System.currentTimeMillis()
            val last = lastInvokedAt.computeIfAbsent(key) { AtomicLong(0) }
            val prev = last.get()
            if (now - prev < rateWindowMs) {
                flights[key]?.let { return it.await() as T }
            } else {
                last.set(now)
            }
        }

        flights[key]?.let { return it.await() as T }

        val deferred = CompletableDeferred<Any?>()
        val existing = flights.putIfAbsent(key, deferred)
        if (existing != null) {
            return existing.await() as T
        }

        lastInvokedAt.computeIfAbsent(key) { AtomicLong(0) }.set(System.currentTimeMillis())

        try {
            var attempts = 0
            while (true) {
                try {
                    val result: T =
                        if (timeoutMs > 0) withTimeout(timeoutMs) { block() } else block()
                    deferred.complete(result)
                    return result
                } catch (t: Throwable) {
                    if (attempts >= retry) {
                        deferred.completeExceptionally(t)
                        throw t
                    } else {
                        attempts++
                        if (backoffMs > 0) {
                            try {
                                kotlinx.coroutines.delay(backoffMs * attempts)
                            } catch (ce: CancellationException) {
                                deferred.completeExceptionally(ce)
                                throw ce
                            }
                        }
                    }
                }
            }
        } finally {
            flights.remove(key, deferred)
        }
    }

    enum class ReenterPolicy { Drop, Wait }

    @JvmStatic
    suspend fun <T> nonReentrant(
        key: String,
        policy: ReenterPolicy = ReenterPolicy.Drop,
        block: suspend () -> T
    ): T? {
        val sem = semaphoreFor(key)
        return when (policy) {
            ReenterPolicy.Drop -> {
                if (!sem.tryAcquire()) return null
                try {
                    block()
                } finally {
                    sem.release()
                }
            }

            ReenterPolicy.Wait -> {
                sem.withPermit { block() }
            }
        }
    }

    @JvmStatic
    suspend fun <T> guardedSingleFlight(
        key: String,
        rateWindowMs: Long = DEFAULT_RATE_WINDOW_MS,
        reenterPolicy: ReenterPolicy = ReenterPolicy.Drop,
        timeoutMs: Long = 0L,
        retry: Int = 0,
        backoffMs: Long = 0L,
        block: suspend () -> T
    ): T? {
        return nonReentrant(key, reenterPolicy) {
            singleFlight(key, rateWindowMs, timeoutMs, retry, backoffMs, block)
        }
    }

    /**
     * throttleFirstSuspend：在窗口期内只允许首次调用执行，后续触发直接丢弃（返回 null）
     * - 适用于“我不关心这段时间内的重复触发，只想执行一次”
     */
    @JvmStatic
    suspend fun <T> throttleFirstSuspend(
        key: String,
        windowMs: Long = DEFAULT_RATE_WINDOW_MS,
        block: suspend () -> T
    ): T? {
        if (windowMs <= 0L) return block()
        val now = System.currentTimeMillis()
        val last = lastThrottledAt.computeIfAbsent(key) { AtomicLong(0) }
        val prev = last.get()
        if (now - prev < windowMs) {
            // 窗口内：丢弃（也可以改为返回上次结果，需增加缓存）
            return null
        }
        if (!last.compareAndSet(prev, now)) {
            // 并发竞争下，只有一个能赢，其它视为窗口内触发
            return null
        }
        return try {
            block()
        } finally {
            // 不回退 last；保持“窗口起始点”为本次执行时间
        }
    }

    // ============= Java 友好 API（CompletableFuture） =============

    private val javaFlights = ConcurrentHashMap<String, CompletableFuture<Any?>>()
    private val javaLastInvokedAt = ConcurrentHashMap<String, AtomicLong>()
    private val javaLocks = ConcurrentHashMap<String, ReentrantLock>()
    private val javaSerialExecutors = ConcurrentHashMap<String, ExecutorService>()

    private fun javaLockFor(key: String): ReentrantLock =
        javaLocks.computeIfAbsent(key) { ReentrantLock() }

    private fun javaSerialExecutorFor(key: String): ExecutorService =
        javaSerialExecutors.computeIfAbsent(key) {
            Executors.newSingleThreadExecutor { r ->
                Thread(r, "CallCoordinator-serial-$key").apply { isDaemon = true }
            }
        }

    // 兼容性超时包装（避免使用 API 31 的 orTimeout）
    private fun <T> withTimeoutCompat(
        f: CompletableFuture<T>,
        timeoutMs: Long
    ): CompletableFuture<T> {
        if (timeoutMs <= 0L) return f
        val scheduled = cleaner.schedule({
            f.completeExceptionally(java.util.concurrent.TimeoutException("Future timeout after ${timeoutMs}ms"))
        }, timeoutMs, TimeUnit.MILLISECONDS)
        f.whenComplete { _, _ -> scheduled.cancel(false) }
        return f
    }

    /**
     * singleFlightAsync（Java/Kotlin 皆可用）
     * - 允许自定义 executor（否则使用全局默认）
     * - 允许超时（内部通过 withTimeoutCompat 实现）
     */
    @JvmStatic
    @JvmOverloads
    fun <T> singleFlightAsync(
        key: String,
        rateWindowMs: Long = DEFAULT_RATE_WINDOW_MS,
        supplier: Callable<T>,
        executor: Executor = config.javaDefaultExecutor,
        timeoutMs: Long = 0L
    ): CompletableFuture<T> {
        if (rateWindowMs > 0) {
            val now = System.currentTimeMillis()
            val last = javaLastInvokedAt.computeIfAbsent(key) { AtomicLong(0) }
            val prev = last.get()
            if (now - prev < rateWindowMs) {
                @Suppress("UNCHECKED_CAST")
                javaFlights[key]?.let {
                    return withTimeoutCompat(it, timeoutMs)
                        .thenApply { v -> v as T }
                }
            } else {
                last.set(now)
            }
        }

        @Suppress("UNCHECKED_CAST")
        javaFlights[key]?.let {
            return withTimeoutCompat(it, timeoutMs)
                .thenApply { v -> v as T }
        }

        val gate = CompletableFuture<Any?>()
        val existing = javaFlights.putIfAbsent(key, gate)
        if (existing != null) {
            @Suppress("UNCHECKED_CAST")
            return withTimeoutCompat(existing, timeoutMs)
                .thenApply { v -> v as T }
        }

        javaLastInvokedAt.computeIfAbsent(key) { AtomicLong(0) }.set(System.currentTimeMillis())

        CompletableFuture.supplyAsync({
            supplier.call()
        }, executor).whenComplete { result, error ->
            try {
                if (error != null) {
                    gate.completeExceptionally(error)
                } else {
                    gate.complete(result)
                }
            } finally {
                javaFlights.remove(key, gate)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return withTimeoutCompat(gate, timeoutMs)
            .thenApply { v -> v as T }
    }

    enum class JavaReenterPolicy { Drop, Wait }

    @JvmStatic
    @JvmOverloads
    fun <T> guardedSingleFlightAsync(
        key: String,
        rateWindowMs: Long = DEFAULT_RATE_WINDOW_MS,
        reenterPolicy: JavaReenterPolicy = JavaReenterPolicy.Drop,
        supplier: Callable<T>,
        executor: Executor = config.javaDefaultExecutor,
        timeoutMs: Long = 0L
    ): CompletableFuture<T?> {
        return when (reenterPolicy) {
            JavaReenterPolicy.Drop -> {
                val lock = javaLockFor(key)
                if (!lock.tryLock()) {
                    CompletableFuture.completedFuture(null)
                } else {
                    try {
                        singleFlightAsync(key, rateWindowMs, supplier, executor, timeoutMs)
                            .thenApply { it }
                    } finally {
                        lock.unlock()
                    }
                }
            }

            JavaReenterPolicy.Wait -> {
                val exec = javaSerialExecutorFor(key)
                CompletableFuture.supplyAsync({
                    singleFlightAsync(key, rateWindowMs, supplier, executor, timeoutMs).join()
                }, exec)
            }
        }
    }

    /**
     * 取消支持（Java 路径，尽力而为）：
     * - 尝试取消当前在飞的 gate（等待方会收到异常）
     * - 无法强制中止已经运行的 supplier.call()，这取决于 supplier 的可取消性
     */
    @JvmStatic
    @JvmOverloads
    fun cancelJavaFlight(key: String, mayInterruptIfRunning: Boolean = true): Boolean {
        val f = javaFlights[key] ?: return false
        return f.cancel(mayInterruptIfRunning)
    }
}
