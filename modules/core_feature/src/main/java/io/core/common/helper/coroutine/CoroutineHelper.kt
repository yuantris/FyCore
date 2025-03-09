package io.core.common.helper.coroutine

import android.util.Log
import io.core.common.util.log.TAG
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * 启动一个协程，自动捕获并处理异常，避免取消异常被捕获。
 *
 * @param dispatcher 协程调度器，默认为 [Dispatchers.Default]
 * @param scope 协程作用域，若未提供则使用[SupervisorJob]避免异常传播，并与[dispatcher]结合
 * @param onError 异常处理回调，默认为记录日志
 * @param block 要在协程中执行的代码块
 * @return 启动的协程的[Job]
 */
fun launchSuspend(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    onError: (Throwable) -> Unit = { e -> Log.e(TAG, "Caught exception", e) },
    block: suspend CoroutineScope.() -> Unit
): Job {
    return scope.launch(dispatcher) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e // 重新抛出取消异常，确保协程正常取消
        } catch (e: Throwable) {
            onError(e)
        }
    }
}


/**
 * 使用阻塞方式运行挂起函数，适用于测试或非协程环境。
 *
 * @param context 额外的协程上下文，可用于指定调度器等
 * @param onError 异常处理回调，默认记录日志后重新抛出异常
 * @param block 要执行的挂起代码块
 * @return 代码块的执行结果
 */
fun <T> runSuspend(
    context: CoroutineContext = EmptyCoroutineContext,
    onError: (Throwable) -> Unit = { e -> Log.e(TAG, "Caught exception", e) },
    block: suspend CoroutineScope.() -> T
): T = runBlocking(context) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e // 传播取消异常
    } catch (e: Throwable) {
        onError(e)
        throw e // 重新抛出确保调用方感知异常
    }
}
