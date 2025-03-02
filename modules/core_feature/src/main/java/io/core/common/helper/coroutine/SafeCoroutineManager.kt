package io.core.common.helper.coroutine

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import io.core.common.util.log.LogCat
import io.core.common.util.log.TAG
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

internal val SafeCoroutine = SafeCoroutineManager.getInstance()

class SafeCoroutineManager private constructor() {

    companion object {
        @Volatile
        private var instance: SafeCoroutineManager? = null

        fun getInstance(): SafeCoroutineManager =
            instance ?: synchronized(this) {
                instance ?: SafeCoroutineManager().also { instance = it }
            }
    }

    // 核心数据结构
    private val jobRegistry = ConcurrentHashMap<Job, LifecycleObserver>()
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 生命周期绑定策略
    sealed class LifecycleStrategy {
        object AutoClose : LifecycleStrategy()
        data class BindTo(val lifecycle: Lifecycle) : LifecycleStrategy()
        data class BindToWeak(val lifecycle: WeakReference<Lifecycle>) : LifecycleStrategy()
    }

    // 生命周期观察者封装
    private data class LifecycleObserver(
        val lifecycle: WeakReference<Lifecycle>,
        val observer: LifecycleEventObserver
    )

    // 增强版启动方法
    fun launch(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        strategy: LifecycleStrategy = LifecycleStrategy.AutoClose,
        onStart: (() -> Unit)? = null,
        onCompletion: (() -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val context = dispatcher + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            LogCat.e("Uncaught exception", TAG, e)
        }

        val job = mainScope.launch(context) {
            onStart?.invoke()
            try {
                applyLifecycleStrategy(strategy)
                block()
            } finally {
                onCompletion?.invoke()
                if (strategy is LifecycleStrategy.AutoClose) {
                    cancel("AutoClose triggered")
                }
            }
        }

        registerLifecycleTracking(job, strategy)
        setupCompletionHandling(job)
        return job
    }

    // 生命周期策略应用
    private suspend fun applyLifecycleStrategy(strategy: LifecycleStrategy) {
        when (strategy) {
            is LifecycleStrategy.BindTo -> trackStrongLifecycle(strategy.lifecycle)
            is LifecycleStrategy.BindToWeak -> trackWeakLifecycle(strategy.lifecycle)
            else -> Unit
        }
    }

    // 强引用生命周期跟踪
    private suspend fun trackStrongLifecycle(lifecycle: Lifecycle) {
        val currentJob = coroutineContext.job
        val observer = LifecycleEventObserver { source, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                currentJob.cancel("Lifecycle destroyed: ${source::class.simpleName}")
            }
        }

        withContext(Dispatchers.Main.immediate) {
            lifecycle.addObserver(observer)
            jobRegistry[currentJob] = LifecycleObserver(WeakReference(lifecycle), observer)
        }
    }

    // 弱引用生命周期跟踪
    private suspend fun trackWeakLifecycle(lifecycleRef: WeakReference<Lifecycle>) {
        val lifecycle = lifecycleRef.get() ?: return
        trackStrongLifecycle(lifecycle)
    }

    // 注册生命周期跟踪
    private fun registerLifecycleTracking(job: Job, strategy: LifecycleStrategy) {
        when (strategy) {
            is LifecycleStrategy.BindTo -> {
                val observer = LifecycleEventObserver { source, event ->
                    if (event == Lifecycle.Event.ON_DESTROY) job.cancel()
                }
                strategy.lifecycle.addObserver(observer)
                jobRegistry[job] = LifecycleObserver(WeakReference(strategy.lifecycle), observer)
            }

            else -> Unit
        }
    }

    // 设置完成处理
    private fun setupCompletionHandling(job: Job) {
        job.invokeOnCompletion { cause ->
            cause?.let { handleCompletionError(it) }
            cleanupJobResources(job)
        }
    }

    // 异常处理
    private fun handleCompletionError(cause: Throwable) {
        when (cause) {
            is CancellationException -> LogCat.w(
                "Job cancelled: ${cause.message}",
                SafeCoroutineManager::class.java.simpleName
            )

            else -> LogCat.e("Job failed", TAG, cause)
        }
    }

    // 清理单个任务资源
    private fun cleanupJobResources(job: Job) {
        jobRegistry.remove(job)?.let { (lifecycleRef, observer) ->
            lifecycleRef.get()?.removeObserver(observer)
        }
    }

    // 全局清理
    fun cancelAll(reason: String = "Manual cancellation") {
        jobRegistry.keys.forEach { it.cancel(reason) }
        cleanupAllResources()
    }

    private fun cleanupAllResources() {
        jobRegistry.values.forEach { (lifecycleRef, observer) ->
            lifecycleRef.get()?.removeObserver(observer)
        }
        jobRegistry.clear()
    }
}

// 扩展函数
fun LifecycleOwner.launchSafely(
    manager: SafeCoroutineManager = SafeCoroutine,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return manager.launch(
        dispatcher = dispatcher,
        strategy = SafeCoroutineManager.LifecycleStrategy.BindTo(this.lifecycle),
        block = block
    )
}

fun WeakReference<Lifecycle>.launchSafely(
    manager: SafeCoroutineManager = SafeCoroutineManager.getInstance(),
    block: suspend CoroutineScope.() -> Unit
): Job {
    return manager.launch(
        strategy = SafeCoroutineManager.LifecycleStrategy.BindToWeak(this),
        block = block
    )
}
