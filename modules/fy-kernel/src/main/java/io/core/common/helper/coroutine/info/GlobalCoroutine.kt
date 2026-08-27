package io.core.common.helper.coroutine.info

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

/**
 * 全局协程作用域管理类
 * 提供安全的协程启动方法，避免内存泄漏
 */
object GlobalCoroutine {

    // 全局协程作用域
    private val globalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 用于调试的异常处理器
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("GlobalCoroutine caught exception: $throwable")
    }

    /**
     * 在全局作用域中启动协程
     * @param block 协程代码块
     * @return Job 可用于取消协程
     */
    fun launch(
        context: CoroutineContext = Dispatchers.Default,
        exceptionHandler: CoroutineExceptionHandler? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val exception = exceptionHandler ?: this.exceptionHandler
        return globalScope.launch(context + exception) {
            block()
        }
    }

    /**
     * 安全的启动协程，绑定到弱引用的Context
     * @param context 上下文弱引用
     * @param block 协程代码块
     */
    fun <T : Context> launchSafely(
        context: WeakReference<T>,
        block: suspend CoroutineScope.(T) -> Unit
    ): Job {
        return globalScope.launch(exceptionHandler) {
            context.get()?.let { safeContext ->
                block(safeContext)
            } ?: throw CancellationException("Context is null, coroutine cancelled")
        }
    }

    /**
     * 绑定到LifecycleOwner的安全启动方法
     * @param owner LifecycleOwner弱引用
     * @param minState 最小生命周期状态
     * @param block 协程代码块
     */
    fun <T : LifecycleOwner> GlobalCoroutine.launchAutoLifecycle(
        owner: WeakReference<T>,
        minState: Lifecycle.State = Lifecycle.State.STARTED,
        block: suspend CoroutineScope.(T) -> Unit
    ): Job {
        return globalScope.launch(exceptionHandler) {
            owner.get()?.let { safeOwner ->
                safeOwner.lifecycleScope.launch {
                    when (minState) {
                        Lifecycle.State.CREATED -> safeOwner.lifecycle.repeatOnLifecycle(minState) {
                            block(
                                safeOwner
                            )
                        }

                        Lifecycle.State.STARTED -> safeOwner.lifecycle.repeatOnLifecycle(minState) {
                            block(
                                safeOwner
                            )
                        }

                        Lifecycle.State.RESUMED -> safeOwner.lifecycle.repeatOnLifecycle(minState) {
                            block(
                                safeOwner
                            )
                        }

                        else -> block(safeOwner)
                    }
                }
            } ?: throw CancellationException("LifecycleOwner is null")
        }
    }

    /**
     * 在IO线程执行任务
     * @param block IO任务代码块
     * @return Deferred<T> 异步结果
     */
    fun <T> asyncIO(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return globalScope.async(Dispatchers.IO + exceptionHandler) {
            block()
        }
    }

    /**
     * 在主线程执行任务
     * @param block UI任务代码块
     * @return Job
     */
    fun launchUI(block: suspend CoroutineScope.() -> Unit): Job {
        return globalScope.launch(Dispatchers.Main + exceptionHandler) {
            block()
        }
    }

    /**
     * 取消所有全局协程
     */
    fun cancelAll() {
        if (!globalScope.isActive) return
        globalScope.coroutineContext.cancelChildren()
    }

    /**
     * 创建与特定上下文关联的协程作用域
     */
    fun createContextScope(context: CoroutineContext): CoroutineScope {
        return CoroutineScope(globalScope.coroutineContext + context)
    }
}