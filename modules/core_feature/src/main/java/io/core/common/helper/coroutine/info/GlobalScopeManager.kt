package io.core.common.helper.coroutine.info

import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 全局协程作用域管理器，绑定到应用生命周期。
 * 提供协程的启动和清理功能，确保所有协程在应用结束时正确取消。
 */
object GlobalScopeManager {
    private val appJob = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("GlobalScopeManager", "未捕获的异常", throwable)
    }
    private val appScope = CoroutineScope(Dispatchers.Default + appJob + exceptionHandler)
    private var isCleanedUp = false

    /**
     * 在全局作用域中启动协程
     * @param context 协程上下文，可指定调度器等（Job会被自动过滤）
     * @param block 协程代码块
     * @return 启动的协程Job
     */
    fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val safeContext = context.minusKey(Job)
        return appScope.launch(safeContext) { block() }
    }

    /**
     * 清理所有协程，应在应用生命周期结束时调用
     */
    fun cleanup() {
        if (!isCleanedUp) {
            appJob.cancelChildren() // 取消所有子协程但保持job活跃
            appJob.cancel() // 取消根Job
            isCleanedUp = true
        }
    }
}