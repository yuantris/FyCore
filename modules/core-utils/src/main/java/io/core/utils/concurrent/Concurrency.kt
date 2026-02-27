package io.core.utils.concurrent

import io.core.common.helper.coroutine.info.GlobalCoroutine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 并发编程工具类，提供线程池管理、异步任务执行、超时控制、协程与Java Future互操作等功能�?
 *
 * 主要功能包括�?
 * 1. 线程池管理：内置固定大小的线程池和调度线程池，自动根据CPU核心数调整大�?
 * 2. CompletableFuture增强：提供带超时、异常处理等增强功能
 * 3. CountDownLatch工具：简化CountDownLatch的使�?
 * 4. 定时任务调度：支持延迟执行和周期性任�?
 * 5. Kotlin协程桥接：实现协程与CompletableFuture之间的互操作
 *
 * 使用示例�?
 * ```
 * // 线程池使�?
 * val executor = Concurrency.executors
 *
 * // CompletableFuture增强
 * val future = Concurrency.supplyAsync { computeResult() }
 * val futureWithTimeout = Concurrency.withTimeout(future, 5, TimeUnit.SECONDS)
 * val combinedFuture = Concurrency.allOf(future1, future2, future3)
 *
 * // CountDownLatch工具
 * val latch = CountDownLatch(3)
 * Concurrency.awaitLatch(latch, 1, TimeUnit.SECONDS)
 * Concurrency.wrapWithLatch(latch, future).thenAccept { result -> ... }
 * Concurrency.countDown(latch)
 *
 * // 定时任务调度
 * val scheduled = Concurrency.schedule({ println("Delayed task") }, 1, TimeUnit.SECONDS)
 * val periodic = Concurrency.scheduleAtFixedRate({ println("Periodic task") }, 0, 1, TimeUnit.SECONDS)
 * Concurrency.cancelFuture(scheduled)
 *
 * // 协程桥接
 * val result = Concurrency.awaitCompletable(future)
 * val suspendResult = Concurrency.completableToSuspend(future)
 * val futureFromCoroutine = Concurrency.suspendToCompletable { fetchData() }
 *
 * // 关闭线程�?
 * Concurrency.shutdown()
 * ```
 *
 * 线程池配置：
 * - 工作线程池：固定大小，最�?个线程，使用"async-worker"前缀命名
 * - 调度线程池：固定大小，最�?个线程，使用"async-scheduler"前缀命名，守护线�?
 *
 * 注意�?
 * - 所有线程池会在JVM关闭时自动关�?
 * - 默认使用CPU调度器执行协程任�?
 *
 *  CompletableFuture处理集合
 */
class Concurrency private constructor() {
    companion object {
        @JvmStatic
        val executors: ExecutorService by lazy {
            Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors().coerceAtLeast(4),
                NamedThreadFactory("async-worker")
            ).also { pool ->
                Runtime.getRuntime().addShutdownHook(Thread {
                    pool.shutdownNow()
                })
            }
        }

        private val scheduler: ScheduledExecutorService by lazy {
            Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
            ) { r ->
                Thread(r).apply {
                    name = "async-scheduler-${AtomicInteger(0).incrementAndGet()}"
                    isDaemon = true
                }
            }.also { pool ->
                Runtime.getRuntime().addShutdownHook(Thread {
                    pool.shutdownNow()
                })
            }
        }

        // region CompletableFuture 增强
        @JvmStatic
        @JvmOverloads
        fun <T> supplyAsync(
            supplier: () -> T,
            executor: Executor = executors
        ): CompletableFuture<T> {
            return CompletableFuture.supplyAsync(supplier, executor)
        }

        @JvmStatic
        fun <T> withTimeout(
            future: CompletableFuture<T>,
            timeout: Long,
            unit: TimeUnit,
            exception: TimeoutException = TimeoutException("Operation timed out after ${unit.toMillis(timeout)}ms")
        ): CompletableFuture<T> {
            return future.apply {
                scheduler.schedule({
                    if (!isDone) completeExceptionally(exception)
                }, timeout, unit)
            }
        }

        @JvmStatic
        fun <T> allOf(vararg futures: CompletableFuture<T>): CompletableFuture<List<T>> {
            return CompletableFuture.allOf(*futures)
                .thenApply { futures.map { it.get() } }
        }
        // endregion

        // region CountDownLatch 增强
        @JvmStatic
        fun awaitLatch(
            latch: CountDownLatch,
            timeout: Long,
            unit: TimeUnit
        ): Boolean {
            return latch.await(timeout, unit)
        }

        @JvmStatic
        fun <T> wrapWithLatch(
            latch: CountDownLatch,
            future: CompletableFuture<T>
        ): CompletableFuture<T> {
            return future.whenComplete { _, _ -> latch.countDown() }
        }

        @JvmStatic
        fun countDown(latch: CountDownLatch) {
            latch.countDown()
        }
        // endregion

        // region ScheduledFuture 增强
        @JvmStatic
        fun schedule(
            command: Runnable,
            delay: Long,
            unit: TimeUnit
        ): ScheduledFuture<*> {
            return scheduler.schedule(command, delay, unit)
        }

        @JvmStatic
        fun scheduleAtFixedRate(
            command: Runnable,
            initialDelay: Long,
            period: Long,
            unit: TimeUnit
        ): ScheduledFuture<*> {
            return scheduler.scheduleAtFixedRate(command, initialDelay, period, unit)
        }

        @JvmStatic
        fun cancelFuture(future: ScheduledFuture<*>) {
            future.cancel(true)
        }
        // endregion

        // region Kotlin协程桥接
        @JvmStatic
        fun <T> awaitCompletable(future: CompletableFuture<T>): T {
            return future.get()
        }

        @JvmStatic
        fun <T> completableToSuspend(future: CompletableFuture<T>): T {
            return runBlocking { future.await() }
        }

        @JvmStatic
        fun <T> suspendToCompletable(
            block: suspend () -> T,
            dispatcher: CoroutineDispatcher = Dispatchers.Default
        ): CompletableFuture<T> {
            val future = CompletableFuture<T>()
            GlobalCoroutine.launch(dispatcher) {
                try {
                    future.complete(block())
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
            return future
        }

        private suspend fun <T> CompletableFuture<T>.await(): T = suspendCoroutine { cont ->
            whenComplete { result, exception ->
                if (exception == null) {
                    cont.resume(result)
                } else {
                    cont.resumeWithException(exception)
                }
            }
        }
        // endregion
    }

    private class NamedThreadFactory(
        private val prefix: String,
        private val daemon: Boolean = false
    ) : ThreadFactory {
        private val counter = AtomicInteger(0)

        override fun newThread(r: Runnable): Thread {
            return Thread(r, "$prefix-${counter.incrementAndGet()}").apply {
                isDaemon = daemon
                priority = Thread.NORM_PRIORITY
            }
        }
    }
}