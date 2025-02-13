package io.core.common.util.tools

import android.os.Build
import androidx.annotation.RequiresApi
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

object AsyncUtils {
    // 默认线程池配置
    private val DEFAULT_THREAD_POOL by lazy {
        Executors.newCachedThreadPool { r ->
            Thread(r, "AsyncUtils-${POOL_COUNTER.getAndIncrement()}").apply {
                isDaemon = true
            }
        }
    }
    private val POOL_COUNTER = AtomicInteger(1)

    // 可自定义线程池
    @Volatile
    var threadPool: ExecutorService = DEFAULT_THREAD_POOL
        private set

    // 调度线程池（用于定时任务）
    private val scheduler by lazy { Executors.newScheduledThreadPool(4) }

    /**
     * 初始化时自定义线程池（必须在第一次使用前调用）
     */
    @Synchronized
    @JvmStatic
    fun configThreadPool(executor: ExecutorService) {
        if (threadPool !== DEFAULT_THREAD_POOL) {
            throw IllegalStateException("Thread pool already customized")
        }
        threadPool = executor
    }

    // region 基础异步操作
    /**
     * 执行异步任务（无返回值）
     */
    @JvmStatic
    fun runAsync(task: Runnable): CompletableFuture<Void> {
        return CompletableFuture.runAsync(task, threadPool)
    }

    /**
     * 执行异步任务（有返回值）
     */
    @JvmStatic
    fun <T> supplyAsync(task: Callable<T>): CompletableFuture<T> {
        return CompletableFuture.supplyAsync({ task.call()}, threadPool)
    }
    // endregion

    // region 并发工具封装
    /**
     * 使用 CountDownLatch 等待多个任务完成
     */
    @JvmStatic
    fun countDownAwait(
        count: Int,
        awaitTimeout: Long = Long.MAX_VALUE,
        unit: TimeUnit = TimeUnit.MILLISECONDS,
        onFinish: () -> Unit
    ): CountDownLatch {
        val latch = CountDownLatch(count)
        threadPool.submit {
            try {
                if (latch.await(awaitTimeout, unit)) {
                    onFinish()
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        return latch
    }

    /**
     * 使用 CyclicBarrier 协调多个任务
     */
    @JvmStatic
    fun cyclicBarrier(
        parties: Int,
        action: Runnable? = null
    ): CyclicBarrier {
        return CyclicBarrier(parties, action)
    }

    /**
     * 使用 Semaphore 控制并发量
     */
    @JvmStatic
    fun <T> withSemaphore(
        semaphore: Semaphore,
        timeout: Long = Long.MAX_VALUE,
        unit: TimeUnit = TimeUnit.MILLISECONDS,
        action: () -> T
    ): T {
        if (!semaphore.tryAcquire(timeout, unit)) {
            throw TimeoutException("Acquire semaphore timeout")
        }
        try {
            return action()
        } finally {
            semaphore.release()
        }
    }
    // endregion

    // region 定时任务
    /**
     * 延迟执行任务
     */
    @JvmStatic
    fun schedule(
        delay: Long,
        unit: TimeUnit,
        task: Runnable
    ): ScheduledFuture<*> {
        return scheduler.schedule(task, delay, unit)
    }

    /**
     * 固定频率定时任务
     */
    @JvmStatic
    fun scheduleAtFixedRate(
        initialDelay: Long,
        period: Long,
        unit: TimeUnit,
        task: Runnable
    ): ScheduledFuture<*> {
        return scheduler.scheduleWithFixedDelay(task, initialDelay, period, unit)
    }
    // endregion

    // region CompletableFuture 扩展
    /**
     * 在主线程（或其他指定线程池）继续执行
     */
    @JvmStatic
    fun <T> CompletableFuture<T>.thenRunOnMain(
        executor: Executor = ForkJoinPool.commonPool(),
        action: (T) -> Unit
    ): CompletableFuture<Void> {
        return this.thenAcceptAsync(action, executor)
    }

    /**
     * 异常处理扩展
     */
    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun <T> CompletableFuture<T>.exceptionally(handler: (Throwable) -> T): CompletableFuture<T> {
        return this.exceptionallyAsync(handler, threadPool)
    }
    // endregion

    /**
     * 关闭所有资源（谨慎使用）
     */
    @JvmStatic
    @Synchronized
    fun shutdown() {
        if (threadPool !== DEFAULT_THREAD_POOL) {
            threadPool.shutdown()
        }
        scheduler.shutdown()
        DEFAULT_THREAD_POOL.shutdown()
    }
}