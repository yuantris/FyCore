package io.core.common.util.tools

import io.core.common.helper.coroutine.info.GlobalScopeManager
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


@Suppress("UNUSED", "MemberVisibilityCanBePrivate")
class AsyncUtils private constructor() {
    companion object {
        @JvmStatic
        val executors: ExecutorService = ForkJoinPool.commonPool()

        private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
        ) { r ->
            Thread(r).apply {
                name = "async-scheduler-${AtomicInteger(0).incrementAndGet()}"
                isDaemon = true
            }
        }

        // region CompletableFuture 增强
        @JvmStatic
        @JvmOverloads
        fun <T> supplyAsync(
            supplier: () -> T,
            executor: Executor = ForkJoinPool.commonPool()
        ): CompletableFuture<T> {
            return CompletableFuture.supplyAsync(supplier, executor)
        }

        @JvmStatic
        fun <T> withTimeout(
            future: CompletableFuture<T>,
            timeout: Long,
            unit: TimeUnit,
            exception: TimeoutException = TimeoutException("Operation timed out")
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
        fun <T> suspendToCompletable(block: suspend () -> T): CompletableFuture<T> {
            val future = CompletableFuture<T>()
            GlobalScopeManager.launch {
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

        @JvmStatic
        fun shutdown() {
            scheduler.shutdownNow()
            ForkJoinPool.commonPool().shutdown()
        }
    }
}