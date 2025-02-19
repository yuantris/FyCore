package io.core.common.helper

import io.core.common.util.tools.runOnUI
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

// ========================= 增强版并发处理器 =========================
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
                            Result.Failure(index, ConcurrentTimeoutException("Task $index timed out", e))
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
    fun <T> executeForJava(
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
                        runOnUI {
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
                runOnUI {
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
                        runOnUI {
                            callback.onPartialComplete(resultMap)
                            callback.onError(ConcurrentAggregateException(errors.toList()))
                        }
                    }
                    else -> runOnUI { callback.onComplete(resultMap) }
                }
            } catch (e: InterruptedException) {
                runOnUI { callback.onError(ConcurrentException("Interrupted", e)) }
            }
        }

        timeoutFuture?.let { /* 清理逻辑 */ }
    }

    fun <T> executeForJava(
        tasks: List<ProcessorTask<T>>,
        callback: ConcurrentCallback<T>,
        executor: ExecutorService? = null
    ) {
        executeForJava(tasks, callback, 0, TimeUnit.MILLISECONDS, executor)
    }

    // ====================== 数据结构 & 异常 ======================
    sealed class Result<out T> {
        data class Success<out T>(val index: Int, val value: T) : Result<T>()
        data class Failure<out T>(val index: Int, val exception: Throwable) : Result<T>()
    }

    interface ProcessorTask<T> {
        @Throws(Exception::class)
        fun process(): T
    }

    interface ConcurrentCallback<T> {
        fun onComplete(results: SortedMap<Int, T>)
        fun onPartialComplete(partialResults: SortedMap<Int, T>) {}
        fun onError(e: Throwable)
        fun onProgress(completed: Int, total: Int) {}
        fun onEachResult(result: T, index: Int) {}
    }

    open class ConcurrentException(message: String, cause: Throwable? = null) : Exception(message, cause)
    class ConcurrentTimeoutException(message: String, cause: Throwable? = null) : ConcurrentException(message, cause)
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

        fun get(): TaskExecutor = instance ?: synchronized(this) {
            instance ?: TaskExecutor(
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
                Executors.newScheduledThreadPool(2)
            ).also { instance = it }
        }

        fun newInstance(
            threadPoolSize: Int = Runtime.getRuntime().availableProcessors(),
            scheduledThreads: Int = 2
        ): TaskExecutor = TaskExecutor(
            Executors.newFixedThreadPool(threadPoolSize),
            Executors.newScheduledThreadPool(scheduledThreads)
        )
    }
}