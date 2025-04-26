package io.core.common.util.concurrent

import io.core.common.util.extensions.cool.runMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.SortedMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * 增强型并发任务执行器，提供协程和线程两种模式的并发任务执行能力。
 *
 * 主要功能：
 * 1. 协程模式：支持suspend函数的并发执行，提供进度回调、超时控制等功能
 * 2. 线程模式：兼容Java线程模型的并发执行，适用于非协程环境
 * 3. 统一的异常处理和结果收集机制
 *
 * 使用示例：
 * ```
 * // 获取单例实例
 * val manager = TaskExecutor.get()
 *
 * // 协程模式示例
 * launch {
 *     manager.executeConcurrent(
 *         tasks = listOf({ fetchData1() }, { fetchData2() }),
 *         onComplete = { results -> /* 处理结果 */ },
 *         onError = { e, _ -> /* 处理错误 */ }
 *     )
 * }
 *
 * // 线程模式示例
 * manager.execute(
 *     tasks = listOf(ProcessorTask { computeResult() }),
 *     callback = object : ConcurrentCallback<Result> {
 *         override fun onComplete(results: SortedMap<Int, Result>) { /* 处理结果 */ }
 *         override fun onError(e: Throwable) { /* 处理错误 */ }
 *     }
 * )
 * ```
 */
class TaskExecutor private constructor(
    private val javaThreadPool: ExecutorService,
    private val scheduledExecutor: ScheduledExecutorService
) {

    // ====================== Kotlin 协程版本 ======================
    /**
     * 增强型并发执行模板（协程版）
     * @param tasks 任务集合（每个任务必须是 suspend 函数）
     * @param context 协程上下文，默认使用 Dispatchers.IO
     * @param onEachComplete 单个任务完成回调（主线程）
     * @param onComplete 全部成功回调（主线程）
     * @param onPartialComplete 部分完成回调（主线程）
     * @param onError 异常回调（主线程，携带部分结果）
     * @param timeoutMillis 整体超时时间（null 表示无超时）
     * @param progressCallback 进度回调（主线程）
     */
    suspend fun <T> executeConcurrent(
        tasks: List<suspend () -> T>,
        context: CoroutineContext = Dispatchers.IO,
        onEachComplete: ((T, Int) -> Unit)? = null,
        onComplete: (List<T>) -> Unit,
        onPartialComplete: ((List<T>) -> Unit)? = null,
        onError: (Throwable, List<T>?) -> Unit = { _, _ -> },
        timeoutMillis: Long? = null,
        progressCallback: ((completed: Int, total: Int) -> Unit)? = null
    ) = supervisorScope {
        try {
            val totalTasks = tasks.size
            val completedCount = AtomicInteger(0)

            val deferredResults = tasks.mapIndexed { index, task ->
                async(context) {
                    try {
                        val result = timeoutMillis?.let {
                            withTimeout(it) { task() }
                        } ?: task()

                        withContext(Dispatchers.Main) {
                            onEachComplete?.invoke(result, index)
                            progressCallback?.invoke(completedCount.incrementAndGet(), totalTasks)
                        }
                        Result.Success(index, result)
                    } catch (e: Exception) {
                        if (e is CancellationException) {
                            Result.Failure(
                                index,
                                ConcurrentTimeoutException("Task $index timed out", e)
                            )
                        } else {
                            Result.Failure(index, e)
                        }
                    }
                }
            }

            val collectedResults = if (timeoutMillis != null) {
                withTimeout(timeoutMillis) { deferredResults.awaitAll() }
            } else {
                deferredResults.awaitAll()
            }

            // 按原始顺序排序结果和错误
            val successResults = collectedResults
                .filterIsInstance<Result.Success<T>>()
                .sortedBy { it.index }
                .map { it.value }

            val errors = collectedResults
                .filterIsInstance<Result.Failure<T>>()
                .map { it.exception }

            if (errors.isEmpty()) {
                withContext(Dispatchers.Main) { onComplete(successResults) }
            } else {
                withContext(Dispatchers.Main) {
                    onPartialComplete?.invoke(successResults)
                    onError(
                        ConcurrentAggregateException(errors).apply {
                            partialResults = successResults
                        },
                        successResults
                    )
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError(e, null) }
        }
    }

    // ====================== Java 兼容版本 ======================
    /**
     * 增强型并发执行模板（Java 版）
     * @param tasks 任务集合
     * @param callback 回调接口
     * @param timeout 整体超时时间（<=0 表示无超时）
     * @param timeUnit 时间单位
     * @param executor 自定义线程池（可选）
     */
    @JvmOverloads
    fun <T> execute(
        tasks: List<ProcessorTask<T>>,
        callback: ConcurrentCallback<T>,
        timeout: Long = 0,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        executor: ExecutorService? = null
    ) {
        val actualExecutor = executor ?: javaThreadPool
        val resultMap = ConcurrentSkipListMap<Int, T>()
        val errors = ConcurrentLinkedQueue<Throwable>()
        val latch = CountDownLatch(tasks.size)

        tasks.forEachIndexed { index, task ->
            CompletableFuture.supplyAsync(
                {
                    try {
                        task.process()
                    } catch (e: Exception) {
                        throw ConcurrentException("Task $index failed", e)
                    }
                },
                actualExecutor
            ).whenComplete { result, throwable ->
                try {
                    if (throwable != null) {
                        errors.add(throwable.cause ?: throwable)
                    } else {
                        resultMap[index] = result
                        runMain {
                            callback.onEachResult(result, index) // 即时回调
                            callback.onProgress(resultMap.size, tasks.size)
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        // 超时控制
        val timeoutFuture = if (timeout > 0) {
            scheduledExecutor.schedule({
                actualExecutor.shutdownNow()
                runMain {
                    callback.onPartialComplete(resultMap)
                    callback.onError(ConcurrentTimeoutException("Overall timeout"))
                }
            }, timeout, timeUnit)
        } else null

        // 最终完成检查
        actualExecutor.execute {
            try {
                latch.await()

                when {
                    errors.isNotEmpty() -> {
                        runMain {
                            callback.onPartialComplete(resultMap)
                            callback.onError(ConcurrentAggregateException(errors.toList()))
                        }
                    }

                    else -> runMain { callback.onComplete(resultMap) }
                }
            } catch (e: InterruptedException) {
                runMain { callback.onError(ConcurrentException("Interrupted", e)) }
            }
        }

        timeoutFuture?.let { /* 清理逻辑 */ }
    }

    // ====================== 数据结构 & 异常 ======================
    sealed class Result<out T> {
        data class Success<out T>(val index: Int, val value: T) : Result<T>()
        data class Failure<out T>(val index: Int, val exception: Throwable) : Result<T>()
    }

    interface ProcessorTask<T> {
        /**
         * 执行任务并返回结果
         * @throws Exception 可能抛出任何异常
         */
        @Throws(Exception::class)
        fun process(): T
    }

    interface ConcurrentCallback<T> {
        /**
         * 所有任务成功完成时回调
         * @param results 按任务索引排序的结果集合
         */
        fun onComplete(results: SortedMap<Int, T>)

        /**
         * 部分任务完成时回调（可选）
         * @param partialResults 已完成的任务结果
         */
        fun onPartialComplete(partialResults: SortedMap<Int, T>) {}
        fun onError(e: Throwable)

        /**
         * 进度更新回调（可选）
         * @param completed 已完成任务数
         * @param total 总任务数
         */
        fun onProgress(completed: Int, total: Int) {}

        /**
         * 单个任务完成时回调（可选）
         * @param result 任务结果
         * @param index 任务索引
         */
        fun onEachResult(result: T, index: Int) {}
    }

    open class ConcurrentException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

    class ConcurrentTimeoutException(message: String, cause: Throwable? = null) :
        ConcurrentException(message, cause)

    class ConcurrentAggregateException(
        val causes: List<Throwable>,
        var partialResults: Any? = null
    ) : ConcurrentException("Multiple errors (${causes.size}) occurred")

    // ====================== 资源管理 ======================
    fun shutdown() {
        javaThreadPool.shutdownNow()
        scheduledExecutor.shutdownNow()
    }

    companion object {
        @Volatile
        private var instance: TaskExecutor? = null

        /**
         * 获取单例实例
         *
         * 示例：
         * ```
         * val manager = TaskExecutor.get()
         * ```
         */
        @JvmStatic
        fun get(): TaskExecutor = instance ?: synchronized(this) {
            instance ?: TaskExecutor(
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).apply {
                    Runtime.getRuntime().addShutdownHook(Thread { shutdownExecutor(this) })
                },
                Executors.newScheduledThreadPool(2).apply {
                    Runtime.getRuntime().addShutdownHook(Thread { shutdownExecutor(this) })
                }
            ).also { instance = it }
        }

        /**
         * 创建新实例（非单例）
         *
         * @param threadPoolSize 线程池大小，默认为CPU核心数
         * @param scheduledThreads 调度线程数，默认为2
         *
         * 示例：
         * ```
         * // 创建自定义大小的线程池
         * val manager = TaskExecutor.newInstance(threadPoolSize = 8, scheduledThreads = 4)
         * ```
         */
        @JvmStatic
        fun newInstance(
            threadPoolSize: Int = Runtime.getRuntime().availableProcessors(),
            scheduledThreads: Int = 2
        ): TaskExecutor = TaskExecutor(
            Executors.newFixedThreadPool(threadPoolSize).apply {
                Runtime.getRuntime().addShutdownHook(Thread { shutdownExecutor(this) })
            },
            Executors.newScheduledThreadPool(scheduledThreads).apply {
                Runtime.getRuntime().addShutdownHook(Thread { shutdownExecutor(this) })
            }
        )

        private fun shutdownExecutor(executor: ExecutorService) = runCatching {
            executor.shutdownNow()
        }

    }
}