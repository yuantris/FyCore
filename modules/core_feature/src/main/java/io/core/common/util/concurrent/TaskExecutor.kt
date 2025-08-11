package io.core.common.util.concurrent

import io.core.common.util.extensions.cool.runMain
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max

/**
 * 增强型并发任务执行器 - 优化版本
 *
 * 主要改进：
 * 1. 内存安全：改进资源管理，避免内存泄漏
 * 2. 性能优化：批量处理回调，智能线程池配置，减少线程切换
 * 3. API 友好：Builder 模式，扩展函数，更好的类型安全
 * 4. 异常处理：细化异常类型，详细错误信息，改进取消机制
 * 5. 协程增强：结构化并发，灵活作用域管理，Flow 支持
 *
 * 使用示例：
 * ```
 * // 简单使用
 * val executor = TaskExecutor.get()
 * executor.execute(tasks) { results -> /* 处理结果 */ }
 *
 * // Builder 模式
 * val result = TaskExecutor.builder<String>()
 *     .timeout(30000)
 *     .maxConcurrency(10)
 *     .onProgress { completed, total -> /* 进度更新 */ }
 *     .execute(tasks)
 *
 * // Flow 支持
 * executor.executeAsFlow(tasks)
 *     .collect { result -> /* 处理每个结果 */ }
 * ```
 */
class TaskExecutor private constructor() {
    private val resourceManager = ResourceManager()
    private val progressTracker = ProgressTracker()
    private val isShutdown = AtomicBoolean(false)

    // ====================== 协程版本 - 优化 ======================

    /**
     * 执行并发任务（协程版本）- 简化 API
     */
    suspend fun <T> execute(
        tasks: List<suspend () -> T>,
        onComplete: (List<T>) -> Unit = {},
        onError: (TaskExecutionException) -> Unit = {}
    ): ExecutionResult<T> = execute(
        ExecutionConfig.builder<T>()
            .onComplete(onComplete)
            .onError(onError)
            .build(),
        tasks
    )

    /**
     * 执行并发任务（协程版本）- 完整配置
     */
    suspend fun <T> execute(
        config: ExecutionConfig<T>,
        tasks: List<suspend () -> T>
    ): ExecutionResult<T> = supervisorScope {
        checkNotShutdown()
        
        val tracker = progressTracker.createSession(tasks.size)
        val semaphore = config.maxConcurrency?.let { Semaphore(it) }
        val callbackBatcher = CallbackBatcher(config)

        try {
            val deferredResults = tasks.mapIndexed { index, task ->
                async(config.context) {
                    semaphore?.acquire()
                    try {
                        executeTaskWithTimeout(task, index, config.timeoutMillis, tracker, callbackBatcher)
                    } finally {
                        semaphore?.release()
                    }
                }
            }

            val results = if (config.timeoutMillis != null) {
                withTimeout(config.timeoutMillis) { deferredResults.awaitAll() }
            } else {
                deferredResults.awaitAll()
            }

            callbackBatcher.flush()
            processResults(results, config, tracker)

        } catch (e: Exception) {
            callbackBatcher.flush()
            handleExecutionError(e, config, tracker)
        } finally {
            progressTracker.removeSession(tracker.sessionId)
        }
    }

    /**
     * 以 Flow 形式执行任务，支持流式处理
     */
    fun <T> executeAsFlow(
        tasks: List<suspend () -> T>,
        config: ExecutionConfig<T> = ExecutionConfig.default()
    ): Flow<TaskResult<T>> = flow {
        val tracker = progressTracker.createSession(tasks.size)
        val semaphore = config.maxConcurrency?.let { Semaphore(it) }

        try {
            coroutineScope {
                val channel = Channel<TaskResult<T>>(Channel.UNLIMITED)
                
                tasks.forEachIndexed { index, task ->
                    launch(config.context) {
                        semaphore?.acquire()
                        try {
                            val result = executeTaskWithTimeout(task, index, config.timeoutMillis, tracker)
                            channel.send(result)
                        } catch (e: Exception) {
                            channel.send(TaskResult.Failure(index, TaskExecutionException.fromThrowable(e, index)))
                        } finally {
                            semaphore?.release()
                        }
                    }
                }

                repeat(tasks.size) {
                    emit(channel.receive())
                }
                channel.close()
            }
        } finally {
            progressTracker.removeSession(tracker.sessionId)
        }
    }

    // ====================== Java 兼容版本 - 优化 ======================

    /**
     * Java 兼容版本 - 简化 API
     */
    @JvmOverloads
    fun <T> execute(
        tasks: List<ProcessorTask<T>>,
        callback: ConcurrentCallback<T>,
        timeoutMillis: Long = 0
    ) {
        val config = JavaExecutionConfig(
            callback = callback,
            timeoutMillis = if (timeoutMillis > 0) timeoutMillis else null,
            executor = resourceManager.javaThreadPool
        )
        executeJavaInternal(tasks, config)
    }

    /**
     * Java 版本 - 完整配置
     */
    fun <T> executeJavaInternal(
        tasks: List<ProcessorTask<T>>,
        config: JavaExecutionConfig<T>
    ) {
        checkNotShutdown()
        
        val tracker = progressTracker.createSession(tasks.size)
        val resultMap = ConcurrentSkipListMap<Int, T>()
        val errors = ConcurrentLinkedQueue<TaskExecutionException>()
        val latch = CountDownLatch(tasks.size)
        val callbackBatcher = JavaCallbackBatcher(config, resultMap)

        val futures = tasks.mapIndexed { index, task ->
            CompletableFuture.supplyAsync({
                try {
                    val result = task.process()
                    tracker.incrementCompleted()
                    callbackBatcher.addResult(result, index)
                    result
                } catch (e: Exception) {
                    val taskError = TaskExecutionException.fromThrowable(e, index)
                    errors.add(taskError)
                    throw taskError
                }
            }, config.executor).whenComplete { _, _ ->
                latch.countDown()
            }
        }

        // 超时处理
        val timeoutFuture = config.timeoutMillis?.let { timeout ->
            resourceManager.scheduledExecutor.schedule({
                futures.forEach { it.cancel(true) }
                callbackBatcher.flush()
                runMain {
                    config.callback.onPartialComplete(resultMap)
                    config.callback.onError(TaskExecutionException.Timeout("Overall execution timeout"))
                }
            }, timeout, TimeUnit.MILLISECONDS)
        }

        // 完成处理
        CompletableFuture.runAsync({
            try {
                latch.await()
                timeoutFuture?.cancel(false)
                callbackBatcher.flush()

                runMain {
                    when {
                        errors.isEmpty() -> config.callback.onComplete(resultMap)
                        else -> {
                            config.callback.onPartialComplete(resultMap)
                            config.callback.onError(
                                TaskExecutionException.Aggregate(errors.toList(), resultMap)
                            )
                        }
                    }
                }
            } catch (e: InterruptedException) {
                runMain { 
                    config.callback.onError(TaskExecutionException.Interrupted("Execution interrupted", e))
                }
            } finally {
                progressTracker.removeSession(tracker.sessionId)
            }
        }, config.executor)
    }

    // ====================== 内部实现 ======================

    private suspend fun <T> executeTaskWithTimeout(
        task: suspend () -> T,
        index: Int,
        timeoutMillis: Long?,
        tracker: ProgressSession,
        batcher: CallbackBatcher<T>? = null
    ): TaskResult<T> = try {
        val result = timeoutMillis?.let {
            withTimeout(it) { task() }
        } ?: task()
        
        tracker.incrementCompleted()
        batcher?.addResult(result, index)
        TaskResult.Success(index, result)
    } catch (e: CancellationException) {
        TaskResult.Failure(index, TaskExecutionException.Timeout("Task $index timed out", e))
    } catch (e: Exception) {
        TaskResult.Failure(index, TaskExecutionException.fromThrowable(e, index))
    }

    private suspend fun <T> processResults(
        results: List<TaskResult<T>>,
        config: ExecutionConfig<T>,
        tracker: ProgressSession
    ): ExecutionResult<T> {
        val successes = results.filterIsInstance<TaskResult.Success<T>>()
            .sortedBy { it.index }
            .map { it.value }
        
        val failures = results.filterIsInstance<TaskResult.Failure<T>>()
            .map { it.exception }

        return if (failures.isEmpty()) {
            withContext(Dispatchers.Main) { config.onComplete(successes) }
            ExecutionResult.Success(successes, tracker.getStatistics())
        } else {
            withContext(Dispatchers.Main) {
                config.onPartialComplete?.invoke(successes)
                config.onError(TaskExecutionException.Aggregate(failures, successes))
            }
            ExecutionResult.PartialSuccess(successes, failures, tracker.getStatistics())
        }
    }

    private suspend fun <T> handleExecutionError(
        error: Exception,
        config: ExecutionConfig<T>,
        tracker: ProgressSession
    ): ExecutionResult<T> {
        val taskError = TaskExecutionException.fromThrowable(error)
        withContext(Dispatchers.Main) { config.onError(taskError) }
        return ExecutionResult.Failure(taskError, tracker.getStatistics())
    }

    private fun checkNotShutdown() {
        if (isShutdown.get()) {
            throw IllegalStateException("TaskExecutor has been shutdown")
        }
    }

    // ====================== 资源管理 ======================

    fun shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            resourceManager.shutdown()
            progressTracker.clear()
        }
    }

    fun isShutdown(): Boolean = isShutdown.get()

    // ====================== 数据类和接口 ======================

    /**
     * 任务结果
     */
    sealed class TaskResult<out T> {
        data class Success<out T>(val index: Int, val value: T) : TaskResult<T>()
        data class Failure<out T>(val index: Int, val exception: TaskExecutionException) : TaskResult<T>()
    }

    /**
     * 执行结果
     */
    sealed class ExecutionResult<out T> {
        abstract val statistics: ExecutionStatistics

        data class Success<out T>(
            val results: List<T>,
            override val statistics: ExecutionStatistics
        ) : ExecutionResult<T>()

        data class PartialSuccess<out T>(
            val results: List<T>,
            val errors: List<TaskExecutionException>,
            override val statistics: ExecutionStatistics
        ) : ExecutionResult<T>()

        data class Failure<out T>(
            val error: TaskExecutionException,
            override val statistics: ExecutionStatistics
        ) : ExecutionResult<T>()
    }

    /**
     * 执行统计信息
     */
    data class ExecutionStatistics(
        val totalTasks: Int,
        val completedTasks: Int,
        val failedTasks: Int,
        val executionTimeMs: Long,
        val averageTaskTimeMs: Long
    )

    /**
     * 执行配置
     */
    data class ExecutionConfig<T>(
        val context: CoroutineContext = Dispatchers.Default,
        val timeoutMillis: Long? = null,
        val maxConcurrency: Int? = null,
        val onComplete: (List<T>) -> Unit = {},
        val onPartialComplete: ((List<T>) -> Unit)? = null,
        val onError: (TaskExecutionException) -> Unit = {},
        val onProgress: ((completed: Int, total: Int) -> Unit)? = null,
        val onEachComplete: ((T, Int) -> Unit)? = null,
        val batchCallbacks: Boolean = true
    ) {
        companion object {
            fun <T> default() = ExecutionConfig<T>()
            fun <T> builder() = ExecutionConfigBuilder<T>()
        }
    }

    /**
     * Java 执行配置
     */
    data class JavaExecutionConfig<T>(
        val callback: ConcurrentCallback<T>,
        val timeoutMillis: Long? = null,
        val executor: ExecutorService,
        val maxConcurrency: Int? = null
    )

    /**
     * 配置构建器
     */
    class ExecutionConfigBuilder<T> {
        private var context: CoroutineContext = Dispatchers.Default
        private var timeoutMillis: Long? = null
        private var maxConcurrency: Int? = null
        private var onComplete: (List<T>) -> Unit = {}
        private var onPartialComplete: ((List<T>) -> Unit)? = null
        private var onError: (TaskExecutionException) -> Unit = {}
        private var onProgress: ((completed: Int, total: Int) -> Unit)? = null
        private var onEachComplete: ((T, Int) -> Unit)? = null
        private var batchCallbacks: Boolean = true

        fun context(context: CoroutineContext) = apply { this.context = context }
        fun timeout(timeoutMillis: Long) = apply { this.timeoutMillis = timeoutMillis }
        fun maxConcurrency(max: Int) = apply { this.maxConcurrency = max }
        fun onComplete(callback: (List<T>) -> Unit) = apply { this.onComplete = callback }
        fun onPartialComplete(callback: (List<T>) -> Unit) = apply { this.onPartialComplete = callback }
        fun onError(callback: (TaskExecutionException) -> Unit) = apply { this.onError = callback }
        fun onProgress(callback: (completed: Int, total: Int) -> Unit) = apply { this.onProgress = callback }
        fun onEachComplete(callback: (T, Int) -> Unit) = apply { this.onEachComplete = callback }
        fun batchCallbacks(batch: Boolean) = apply { this.batchCallbacks = batch }

        fun build() = ExecutionConfig(
            context, timeoutMillis, maxConcurrency, onComplete, onPartialComplete,
            onError, onProgress, onEachComplete, batchCallbacks
        )

        suspend fun execute(tasks: List<suspend () -> T>): ExecutionResult<T> {
            return TaskExecutor.get().execute(build(), tasks)
        }
    }

    /**
     * 任务接口
     */
    interface ProcessorTask<T> {
        @Throws(Exception::class)
        fun process(): T
    }

    /**
     * 回调接口 - 增强版
     */
    interface ConcurrentCallback<T> {
        fun onComplete(results: SortedMap<Int, T>)
        fun onPartialComplete(partialResults: SortedMap<Int, T>) {}
        fun onError(e: TaskExecutionException)
        fun onProgress(completed: Int, total: Int) {}
        fun onEachResult(result: T, index: Int) {}
    }

    /**
     * 异常体系 - 细化
     */
    sealed class TaskExecutionException(
        message: String,
        cause: Throwable? = null
    ) : Exception(message, cause) {
        
        abstract val taskIndex: Int?

        data class TaskFailure(
            override val taskIndex: Int,
            val originalException: Throwable
        ) : TaskExecutionException("Task $taskIndex failed: ${originalException.message}", originalException)

        data class Timeout(
            val reason: String,
            val originalException: Throwable? = null,
            override val taskIndex: Int? = null
        ) : TaskExecutionException("Timeout: $reason", originalException)

        data class Interrupted(
            val reason: String,
            val originalException: Throwable? = null,
            override val taskIndex: Int? = null
        ) : TaskExecutionException("Interrupted: $reason", originalException)

        data class Aggregate(
            val causes: List<TaskExecutionException>,
            val partialResults: Any? = null,
            override val taskIndex: Int? = null
        ) : TaskExecutionException("Multiple errors occurred (${causes.size} failures)")

        companion object {
            fun fromThrowable(throwable: Throwable, taskIndex: Int? = null): TaskExecutionException {
                return when (throwable) {
                    is TaskExecutionException -> throwable
                    is CancellationException -> Timeout("Task cancelled", throwable, taskIndex)
                    is InterruptedException -> Interrupted("Task interrupted", throwable, taskIndex)
                    else -> TaskFailure(taskIndex ?: -1, throwable)
                }
            }
        }
    }

    // ====================== 支持类 ======================

    /**
     * 资源管理器 - 负责线程池和资源的生命周期管理
     */
    private class ResourceManager {
        val javaThreadPool: ExecutorService by lazy {
            createThreadPool().apply {
                registerShutdownHook(this)
            }
        }

        val scheduledExecutor: ScheduledExecutorService by lazy {
            Executors.newScheduledThreadPool(2) { r ->
                Thread(r, "TaskExecutor-Scheduler").apply {
                    isDaemon = true
                }
            }.apply {
                registerShutdownHook(this)
            }
        }

        private fun createThreadPool(): ExecutorService {
            val corePoolSize = Runtime.getRuntime().availableProcessors()
            val maxPoolSize = max(corePoolSize * 2, 16)
            
            return ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS,
                LinkedBlockingQueue(),
                { r -> Thread(r, "TaskExecutor-Worker").apply { isDaemon = true } },
                ThreadPoolExecutor.CallerRunsPolicy()
            )
        }

        private fun registerShutdownHook(executor: ExecutorService) {
            Runtime.getRuntime().addShutdownHook(Thread {
                runCatching {
                    executor.shutdown()
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        executor.shutdownNow()
                    }
                }
            })
        }

        fun shutdown() {
            runCatching { javaThreadPool.shutdown() }
            runCatching { scheduledExecutor.shutdown() }
        }
    }

    /**
     * 进度跟踪器 - 管理多个执行会话的进度
     */
    private class ProgressTracker {
        private val sessions = ConcurrentHashMap<String, ProgressSession>()
        private val sessionIdGenerator = AtomicLong(0)

        fun createSession(totalTasks: Int): ProgressSession {
            val sessionId = "session-${sessionIdGenerator.incrementAndGet()}"
            val session = ProgressSession(sessionId, totalTasks)
            sessions[sessionId] = session
            return session
        }

        fun removeSession(sessionId: String) {
            sessions.remove(sessionId)
        }

        fun clear() {
            sessions.clear()
        }
    }

    /**
     * 进度会话 - 单个执行会话的进度跟踪
     */
    private class ProgressSession(
        val sessionId: String,
        private val totalTasks: Int
    ) {
        private val completedTasks = AtomicInteger(0)
        private val startTime = System.currentTimeMillis()

        fun incrementCompleted(): Int = completedTasks.incrementAndGet()

        fun getStatistics(): ExecutionStatistics {
            val completed = completedTasks.get()
            val executionTime = System.currentTimeMillis() - startTime
            val averageTime = if (completed > 0) executionTime / completed else 0L
            
            return ExecutionStatistics(
                totalTasks = totalTasks,
                completedTasks = completed,
                failedTasks = totalTasks - completed,
                executionTimeMs = executionTime,
                averageTaskTimeMs = averageTime
            )
        }
    }

    /**
     * 回调批处理器 - 优化主线程切换性能
     */
    private class CallbackBatcher<T>(private val config: ExecutionConfig<T>) {
        private val results = mutableListOf<Pair<T, Int>>()
        private val lock = Any()

        fun addResult(result: T, index: Int) {
            if (!config.batchCallbacks) {
                // 立即回调
                runMain {
                    config.onEachComplete?.invoke(result, index)
                    config.onProgress?.invoke(index + 1, -1) // -1 表示未知总数
                }
                return
            }

            synchronized(lock) {
                results.add(result to index)
            }
        }

        fun flush() {
            if (!config.batchCallbacks) return

            val toProcess = synchronized(lock) {
                results.toList().also { results.clear() }
            }

            if (toProcess.isNotEmpty()) {
                runMain {
                    toProcess.forEach { (result, index) ->
                        config.onEachComplete?.invoke(result, index)
                    }
                    config.onProgress?.invoke(toProcess.size, -1)
                }
            }
        }
    }

    /**
     * Java 回调批处理器
     */
    private class JavaCallbackBatcher<T>(
        private val config: JavaExecutionConfig<T>,
        private val resultMap: ConcurrentSkipListMap<Int, T>
    ) {
        private val pendingResults = mutableListOf<Pair<T, Int>>()
        private val lock = Any()

        fun addResult(result: T, index: Int) {
            synchronized(lock) {
                pendingResults.add(result to index)
            }
        }

        fun flush() {
            val toProcess = synchronized(lock) {
                pendingResults.toList().also { pendingResults.clear() }
            }

            if (toProcess.isNotEmpty()) {
                runMain {
                    toProcess.forEach { (result, index) ->
                        config.callback.onEachResult(result, index)
                        config.callback.onProgress(resultMap.size, -1)
                    }
                }
            }
        }
    }

    // ====================== 单例管理 ======================

    companion object {
        @Volatile
        private var instance: TaskExecutor? = null
        private val instanceLock = Any()

        /**
         * 获取单例实例 - 改进内存安全
         */
        @JvmStatic
        fun get(): TaskExecutor = instance ?: synchronized(instanceLock) {
            instance ?: TaskExecutor().also { instance = it }
        }

        /**
         * 创建新实例 - 改进配置
         */
        @JvmStatic
        fun newInstance(): TaskExecutor = TaskExecutor()

        /**
         * Builder 模式入口
         */
        @JvmStatic
        fun <T> builder(): ExecutionConfigBuilder<T> = ExecutionConfigBuilder()

        /**
         * 清理单例实例 - 用于测试或应用关闭
         */
        @JvmStatic
        fun clearInstance() {
            synchronized(instanceLock) {
                instance?.shutdown()
                instance = null
            }
        }
    }
}

// ====================== 扩展函数 ======================

/**
 * 简化的协程执行扩展
 */
suspend fun <T> List<suspend () -> T>.executeParallel(
    maxConcurrency: Int? = null,
    timeoutMillis: Long? = null
): List<T> {
    val executor = TaskExecutor.get()
    val config = TaskExecutor.ExecutionConfig<T>(
        maxConcurrency = maxConcurrency,
        timeoutMillis = timeoutMillis
    )
    
    val result = executor.execute(config, this)
    return when (result) {
        is TaskExecutor.ExecutionResult.Success -> result.results
        is TaskExecutor.ExecutionResult.PartialSuccess -> result.results
        is TaskExecutor.ExecutionResult.Failure -> throw result.error
    }
}

/**
 * Flow 扩展
 */
fun <T> List<suspend () -> T>.executeAsFlow(
    maxConcurrency: Int? = null
): Flow<TaskExecutor.TaskResult<T>> {
    val executor = TaskExecutor.get()
    val config = TaskExecutor.ExecutionConfig<T>(maxConcurrency = maxConcurrency)
    return executor.executeAsFlow(this, config)
}

/**
 * Java 友好的扩展
 */
fun <T> List<TaskExecutor.ProcessorTask<T>>.executeParallel(
    callback: TaskExecutor.ConcurrentCallback<T>,
    timeoutMillis: Long = 0
) {
    TaskExecutor.get().execute(this, callback, timeoutMillis)
}