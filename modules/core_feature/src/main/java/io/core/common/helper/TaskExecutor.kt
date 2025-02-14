package io.core.common.helper

import io.core.common.util.tools.runOnUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.SortedMap
import java.util.concurrent.*
import kotlin.coroutines.cancellation.CancellationException

// ========================= 增强版并发处理器 =========================
class TaskExecutor private constructor() {

    // ====================== Kotlin 协程版本 ======================
    /**
     * 增强型并发执行模板（协程版）
     * @param tasks 任务集合（每个任务必须是 suspend 函数）
     * @param onEachComplete 单个任务完成回调（携带结果和索引）
     * @param onComplete 全部完成回调（主线程）
     * @param onError 异常回调（主线程）
     * @param timeoutMillis 整体超时时间（null 表示无超时）
     * @param progressCallback 进度回调（完成数，总数）
     */
    suspend fun <T> executeConcurrent(
        tasks: List<suspend () -> T>,
        onEachComplete: ((T, Int) -> Unit)? = null,
        onComplete: (List<T>) -> Unit,
        onError: (Throwable) -> Unit = {},
        timeoutMillis: Long? = null,
        progressCallback: ((completed: Int, total: Int) -> Unit)? = null
    ) = coroutineScope {
        try {
            val totalTasks = tasks.size
            val results = mutableListOf<T>()

            val deferredResults = tasks.mapIndexed { index, task ->
                async(Dispatchers.IO) {
                    try {
                        val result = if (timeoutMillis != null) {
                            withTimeout(timeoutMillis) { task() }
                        } else {
                            task()
                        }

                        // 主线程回调
                        withContext(Dispatchers.Main) {
                            onEachComplete?.invoke(result, index)
                            progressCallback?.invoke(index + 1, totalTasks)
                        }
                        result
                    } catch (e: Exception) {
                        if (e is CancellationException) {
                            throw ConcurrentTimeoutException("Task $index timed out", e)
                        }
                        throw e
                    }
                }
            }

            // 处理整体超时
            val completedResults = timeoutMillis?.let {
                withTimeout(it) { deferredResults.awaitAll() }
            } ?: deferredResults.awaitAll()

            results.addAll(completedResults)
            withContext(Dispatchers.Main) { onComplete(results) }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError(e) }
        }
    }

    // ====================== Java 兼容版本 ======================
    private val javaThreadPool = Executors.newCachedThreadPool()
    private val scheduledExecutor = Executors.newScheduledThreadPool(2)

    /**
     * 无超时版本（方法重载）
     */
    fun <T> executeForJava(
        tasks: List<ProcessorTask<T>>,
        callback: ConcurrentCallback<T>
    ) = executeForJava(tasks, callback, 0, TimeUnit.MILLISECONDS)

    /**
     * 增强型并发执行模板（Java 版）
     * @param tasks 任务集合
     * @param callback 带丰富状态的回调接口
     * @param timeout 整体超时时间（<=0 表示无超时）
     * @param timeUnit 时间单位
     */
    fun <T> executeForJava(
        tasks: List<ProcessorTask<T>>,
        callback: ConcurrentCallback<T>,
        timeout: Long = 0,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS
    ) {
        val executor = if (timeout > 0) javaThreadPool else ForkJoinPool.commonPool()

        executor.execute {
            try {
                val taskPairs = tasks.mapIndexed { index, task ->
                    index to Callable<T> { task.process() }
                }

                val futures = if (timeout > 0) {
                    javaThreadPool.invokeAll(
                        taskPairs.map { it.second },
                        timeout,
                        timeUnit
                    )
                } else {
                    javaThreadPool.invokeAll(taskPairs.map { it.second })
                }

                val resultMap = ConcurrentHashMap<Int, T>()
                val errors = ConcurrentLinkedQueue<Throwable>()

                // 并行处理结果（保留原始顺序）
                futures.forEachIndexed { futureIndex, future ->
                    try {
                        if (future.isDone) {
                            val result = future.get()
                            val originalIndex = taskPairs[futureIndex].first
                            resultMap[originalIndex] = result
                            runOnUI {
                                callback.onEachResult(result, originalIndex)
                                callback.onProgress(resultMap.size, tasks.size)
                            }
                        }
                    } catch (e: InterruptedException) {
                        errors.add(ConcurrentException("Task $futureIndex interrupted", e))
                    } catch (e: ExecutionException) {
                        errors.add(ConcurrentException("Task $futureIndex failed", e.cause))
                    } catch (e: CancellationException) {
                        errors.add(ConcurrentTimeoutException("Task $futureIndex timed out"))
                    }
                }

                when {
                    errors.isNotEmpty() -> {
                        val sortedMap = resultMap.toSortedMap()
                        runOnUI {
                            callback.onPartialComplete(sortedMap)
                            callback.onError(ConcurrentAggregateException(errors.toList()))
                        }
                    }

                    resultMap.size == tasks.size -> {
                        runOnUI { callback.onComplete(resultMap.toSortedMap()) }
                    }

                    else -> {
                        val sortedMap = resultMap.toSortedMap()
                        runOnUI {
                            callback.onPartialComplete(sortedMap)
                            callback.onError(ConcurrentIncompleteException("Partial completion"))
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUI { callback.onError(ConcurrentException("Execution failed", e)) }
            }
        }

        // 定时取消任务（仅限带超时）
        if (timeout > 0) {
            scheduledExecutor.schedule({
                executor.shutdownNow()
                runOnUI { callback.onError(ConcurrentTimeoutException("Overall timeout reached")) }
            }, timeout, timeUnit)
        }
    }

    // ====================== 增强回调接口 ======================
    interface ConcurrentCallback<T> {
        /**
         * 全部成功时回调（有序 Map）
         * @param results 按任务原始顺序排序的结果集
         */
        fun onComplete(results: SortedMap<Int, T>)

        /**
         * 部分完成时回调
         * @param partialResults 已完成的結果（保留原始顺序）
         */
        fun onPartialComplete(partialResults: SortedMap<Int, T>) {}

        fun onError(e: Throwable)
        fun onProgress(completed: Int, total: Int) {}
        fun onEachResult(result: T, index: Int) {}
    }


    // ====================== 数据结构定义 ======================
    interface ProcessorTask<T> {
        @Throws(Exception::class)
        fun process(): T
    }


    // ====================== 异常体系 ======================
    open class ConcurrentException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

    class ConcurrentTimeoutException(message: String, cause: Throwable? = null) :
        ConcurrentException(message, cause)

    class ConcurrentAggregateException(val causes: List<Throwable>) :
        ConcurrentException("Multiple errors occurred")

    class ConcurrentIncompleteException(message: String) : ConcurrentException(message)

    // ====================== 资源管理 ======================
    fun shutdown() {
        javaThreadPool.shutdownNow()
        scheduledExecutor.shutdownNow()
    }

    // ====================== 单例实现 ======================
    companion object {
        @Volatile
        private var instance: TaskExecutor? = null

        fun get(): TaskExecutor = instance ?: synchronized(this) {
            instance ?: TaskExecutor().also { instance = it }
        }
    }
}