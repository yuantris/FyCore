package io.core.utils.concurrent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext

class TaskExecutorV2(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineContext = Dispatchers.Default
) {
    // region 内部数据结构
    private data class TaskConfig(
        val block: suspend () -> Any?,
        var priority: Int = 0,
        var depends: List<Int> = emptyList(),
        var maxRetries: Int = 0,
        var retryCondition: (Throwable) -> Boolean = { true }
    )

    private val tasks = mutableListOf<TaskConfig>()
    private var timeoutMillis: Long? = null
    private var progressChannel: Channel<ProgressEvent>? = null

    private var onSuccess: (List<ResultWithIndex>) -> Unit = {}
    private var onError: (Throwable) -> Unit = {}
    private var onComplete: () -> Unit = {}

    // endregion

    // region 公共配置方法（链式调用）
    /**
     * 添加并发任务
     * @param block 任务逻辑（返回结果会保留类型信息�?
     */
    fun <T> addTask(
        priority: Int = 0,
        depends: List<Int> = emptyList(),
        maxRetries: Int = 0,
        retryCondition: (Throwable) -> Boolean = { true },
        block: suspend () -> T
    ): TaskExecutorV2 {
        tasks.add(
            TaskConfig(
                block = block as suspend () -> Any?,
                priority = priority,
                depends = depends,
                maxRetries = maxRetries,
                retryCondition = retryCondition
            )
        )
        return this
    }

    /**
     * 设置全局超时（毫秒）
     */
    fun setTimeout(millis: Long) = apply { timeoutMillis = millis }

    /**
     * 启用进度通知（每任务单独触发�?
     */
    fun enableProgressTracking() = apply {
        progressChannel = Channel(Channel.UNLIMITED)
    }

    fun onSuccess(block: (List<ResultWithIndex>) -> Unit) = apply { onSuccess = block }
    fun onError(block: (Throwable) -> Unit) = apply { onError = block }
    fun onComplete(block: () -> Unit) = apply { onComplete = block }
    // endregion

    // region 核心执行逻辑
    /**
     * 启动任务执行（Flow方式�?
     */
    fun executeAsFlow(): Flow<ConcurrentEvent> = flow {
        val deferredResults = mutableMapOf<Int, Deferred<Any?>>()
        val taskQueue = resolveDependencies()

        // 按优先级排序后启动任�?
        taskQueue.sortedByDescending { it.priority }.forEachIndexed { index, config ->
            deferredResults[index] = scope.async(dispatcher) {
                executeWithRetry(config, index)
            }
        }

        // 收集结果
        try {
            val results = timeoutMillis?.let {
                withTimeout(it) { deferredResults.values.awaitAll() }
            } ?: deferredResults.values.awaitAll()

            val resultList = results.mapIndexed { i, res -> ResultWithIndex(i, res) }
            onSuccess(resultList)
            emit(ConcurrentEvent.Success(resultList))
        } catch (e: Exception) {
            if (e !is CancellationException) {
                onError(e)
                emit(ConcurrentEvent.Error(e))
            }
        } finally {
            onComplete()
            emit(ConcurrentEvent.Completed)
            progressChannel?.close()
        }
    }.catch { e -> emit(ConcurrentEvent.Error(e)) }

    fun execute() {
        val flow = executeAsFlow()
        flow.launchIn(scope)
    }
    // 解析任务依赖关系，返回可执行任务队列
    private fun resolveDependencies(): List<TaskConfig> {
        // 实现依赖拓扑排序（此处简化为按顺序执行）
        return tasks.filter { it.depends.isEmpty() } +
                tasks.filter { it.depends.isNotEmpty() }
    }

    // 带重试的任务执行
    private suspend fun executeWithRetry(
        config: TaskConfig,
        taskIndex: Int
    ): Any? {
        var retryCount = 0
        while (true) {
            try {
                notifyProgress(taskIndex, ProgressType.START)
                val result = config.block()
                notifyProgress(taskIndex, ProgressType.FINISH)
                return result
            } catch (e: Throwable) {
                if (retryCount < config.maxRetries && config.retryCondition(e)) {
                    retryCount++
                    notifyProgress(taskIndex, ProgressType.RETRY(retryCount))
                } else {
                    notifyProgress(taskIndex, ProgressType.FAILED)
                    throw e
                }
            }
        }
    }

    private fun notifyProgress(index: Int, type: ProgressType) {
        progressChannel?.trySend(ProgressEvent(index, type))
    }
    // endregion

    // region 嵌套类定�?
    sealed class ConcurrentEvent {
        data class Success(val results: List<ResultWithIndex>) : ConcurrentEvent()
        data class Error(val exception: Throwable) : ConcurrentEvent()
        object Completed : ConcurrentEvent()
    }

    data class ResultWithIndex(val index: Int, val result: Any?)

    data class ProgressEvent(
        val taskIndex: Int,
        val type: ProgressType
    )

    sealed class ProgressType {
        object START : ProgressType()
        object FINISH : ProgressType()
        data class RETRY(val count: Int) : ProgressType()
        object FAILED : ProgressType()
    }
    // endregion

    companion object {

        fun get(scope: CoroutineScope): TaskExecutorV2 {
            return TaskExecutorV2(scope)
        }
    }

    // region 扩展方法（结果转换）
    /**
     * 类型安全的结果转换扩�?
     */
    inline fun <reified T> List<ResultWithIndex>.getResult(index: Int): T {
        return this.first { it.index == index }.result as T
    }
}