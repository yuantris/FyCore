package io.core.common.util.tools

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 线程管理工具类
 * 特性：
 * 1. 支持任务取消和生命周期绑定
 * 2. 完善的错误处理机制
 * 3. 多种预设线程池配置
 * 4. 进度更新和状态回调
 */
object ThreadUltra {
    enum class ThreadType {
        IO, COMPUTATION, SINGLE, CACHED
    }

    private val mainHandler = buildMainHandler()
    private val threadPoolMap = ConcurrentHashMap<ThreadType, ExecutorService>()
    private val customPools = ConcurrentHashMap<String, ExecutorService>()

    sealed class ErrorType {
        object Network : ErrorType()
        object IO : ErrorType()
        object Parse : ErrorType()
        object Timeout : ErrorType()
        data class Custom(val code: Int, val message: String) : ErrorType()
        data class Unknown(val exception: Throwable) : ErrorType()
    }

    abstract class Task<T> : LifecycleObserver {
        private val isCancelled = AtomicBoolean(false)
        private var future: Future<*>? = null

        @WorkerThread
        @Throws(Throwable::class)
        abstract fun doInBackground(): T
        abstract fun onSuccess(result: T)

        open fun onFail(errorType: ErrorType, ex: Throwable) = Unit
        open fun onComplete() = Unit
        open fun onProgress(vararg values: Int) = Unit

        fun bindTo(lifecycle: Lifecycle) {
            lifecycle.addObserver(this)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        private fun onLifecycleDestroy() {
            cancel()
        }

        @AnyThread
        protected fun publishProgress(vararg values: Int) {
            if (!isCancelled()) {
                mainHandler.post { onProgress(*values) }
            }
        }

        fun cancel() {
            if (isCancelled.compareAndSet(false, true)) {
                future?.cancel(true)
            }
        }

        fun isCancelled() = isCancelled.get()

        fun setFuture(future: Future<*>) {
            this.future = future
        }
    }

    init {
        initDefaultPools()
    }

    @JvmStatic
    @JvmOverloads
    fun <T> execute(task: Task<T>, threadType: ThreadType = ThreadType.SINGLE) {
        execute(task, getExecutorByType(threadType))
    }

    @JvmStatic
    fun <T> execute(task: Task<T>, executor: ExecutorService) {
        if (task.isCancelled()) return

        val future = executor.submit(Callable<T> {
            checkCancellation(task)
            val result = task.doInBackground()
            checkCancellation(task)
            result
        })

        task.setFuture(future)

        // 使用CompletableFuture处理回调
        CompletableFuture.supplyAsync({
            try {
                future.get()
            } catch (e: InterruptedException) {
                throw CancellationException()
            } catch (e: ExecutionException) {
                throw e.cause ?: e
            }
        }, executor).whenComplete { result, ex ->
            handleCompletion(task, result, ex)
        }
    }

    fun getIoPool() = threadPoolMap[ThreadType.IO]!!
    fun getCachedPool() = threadPoolMap[ThreadType.CACHED]!!
    fun getCpuPool() = threadPoolMap[ThreadType.COMPUTATION]!!
    fun getSinglePool() = threadPoolMap[ThreadType.SINGLE]!!

    private fun <T> checkCancellation(task: Task<T>) {
        if (task.isCancelled()) {
            throw CancellationException()
        }
    }

    private fun <T> handleCompletion(task: Task<T>, result: T?, ex: Throwable?) {
        when {
            ex != null -> handleFailure(ex, task)
            !task.isCancelled() -> mainHandler.post {
                task.onSuccess(result as T)
                task.onComplete()
            }

            else -> mainHandler.post(task::onComplete)
        }
    }

    private fun <T> handleFailure(ex: Throwable, task: Task<T>) {
        when (ex) {
            is CancellationException -> return
            else -> mainHandler.post {
                task.onFail(classifyError(ex), ex)
                task.onComplete()
            }
        }
    }

    private fun classifyError(ex: Throwable): ErrorType = when (ex) {
        is java.net.UnknownHostException,
        is java.net.ConnectException -> ErrorType.Network

        is java.net.SocketTimeoutException,
        is TimeoutException -> ErrorType.Timeout

        is java.io.IOException -> ErrorType.IO

        is com.google.gson.JsonSyntaxException,
        is org.json.JSONException -> ErrorType.Parse

        else -> ErrorType.Unknown(ex)
    }

    private fun initDefaultPools() {
        val cpuCount = Runtime.getRuntime().availableProcessors()

        threadPoolMap.apply {
            put(ThreadType.IO, ThreadPoolExecutor(
                cpuCount * 2, cpuCount * 2,
                60L, TimeUnit.SECONDS,
                LinkedBlockingQueue(),
                NamedThreadFactory("IO")
            ).apply { allowCoreThreadTimeOut(true) })

            put(
                ThreadType.COMPUTATION, ThreadPoolExecutor(
                    cpuCount, cpuCount,
                    0L, TimeUnit.SECONDS,
                    LinkedBlockingQueue(),
                    NamedThreadFactory("Computation")
                )
            )

            put(
                ThreadType.SINGLE, Executors.newSingleThreadExecutor(
                    NamedThreadFactory("Single")
                )
            )

            put(
                ThreadType.CACHED, ThreadPoolExecutor(
                    0, Int.MAX_VALUE,
                    60L, TimeUnit.SECONDS,
                    SynchronousQueue(),
                    NamedThreadFactory("Cached")
                )
            )
        }
    }


    // 线程池管理方法
    @JvmStatic
    fun shutdown() = (threadPoolMap.values + customPools.values).forEach(::shutdownExecutor)

    @JvmStatic
    fun shutdown(type: ThreadType) = threadPoolMap[type]?.let(::shutdownExecutor)

    private fun shutdownExecutor(executor: ExecutorService) {
        runCatching {
            executor.shutdown()
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow()
                executor.awaitTermination(1, TimeUnit.SECONDS)
            }
        }
    }

    // 其他工具方法
    private fun getExecutorByType(type: ThreadType) = threadPoolMap[type]
        ?: throw IllegalArgumentException("Invalid thread type")

    @JvmStatic
    fun registerCustomPool(name: String, executor: ExecutorService) {
        customPools[name] = executor
    }

    @JvmStatic
    fun getCustomPool(name: String) = customPools[name]

    @JvmOverloads
    @JvmStatic
    fun <T> executeWithLifecycle(
        lifecycleOwner: LifecycleOwner,
        task: Task<T>,
        threadType: ThreadType = ThreadType.SINGLE
    ) {
        task.bindTo(lifecycleOwner.lifecycle)
        execute(task, threadType)
    }

    private class NamedThreadFactory(private val prefix: String) : ThreadFactory {
        private val counter = AtomicInteger(0)
        override fun newThread(r: Runnable) =
            Thread(r, "ThreadUltra-$prefix-${counter.incrementAndGet()}").apply {
                priority = Thread.NORM_PRIORITY
                isDaemon = false
            }
    }
}