package io.core.common.util.extensions.cool

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import io.core.common.helper.coroutine.Coroutine
import io.core.common.util.log.LogCat
import io.core.common.util.log.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TimeoutCancellationException(msg: String) : CancellationException(msg)

/**
 * 在指定时间内执行异步操作
 *
 * 该函数提供了一种方式，可以在指定的延迟时间后，如果操作尚未完成，则取消该操作
 * 它使用协程来并行执行传入的代码块，并在达到时间限制时抛出异常
 *
 * @param T 返回类型
 * @param delayMillis 延迟时间（以毫秒为单位），在此时间后，如果操作未完成，则会尝试取消操作
 * @param block 在指定时间内要执行的代码块
 * @return 返回代码块的执行结果，如果在指定时间内未完成，则抛出异常
 *
 * 注意：该函数是一个挂起函数，适用于协程环境中
 */
suspend fun <T> withTimeoutAsync(delayMillis: Long, block: suspend CoroutineScope.() -> T): T {
    return suspendCancellableCoroutine { count ->
        Coroutine.async(context = count.context) {
            launch {
                delay(delayMillis)
                if (!count.isCompleted) {
                    count.resumeWithException(TimeoutCancellationException("Timed out waiting for $delayMillis ms"))
                }
            }
            val result = block()
            if (!count.isCompleted) {
                count.resume(result)
            }
        }
    }
}

suspend fun <T> withTimeoutOrNullAsync(
    delayMillis: Long,
    block: suspend CoroutineScope.() -> T
): T? {
    return try {
        withTimeoutAsync(delayMillis, block)
    } catch (e: TimeoutCancellationException) {
        null
    }
}


/**
 * 在生命周期处于 STARTED 或更高状态时启动协程
 */
fun LifecycleOwner.launchWhenStarted(action: suspend () -> Unit) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            action()
        }
    }
}

fun LifecycleOwner.launchWhenResumed(action: suspend () -> Unit) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            action()
        }
    }
}

fun LifecycleOwner.launchWhenCreated(action: suspend () -> Unit) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
            action()
        }
    }
}

fun LifecycleOwner.repeatOnLifecycle(state: Lifecycle.State, block: suspend () -> Unit) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(state) {
            block()
        }
    }
}

suspend fun <T> LifecycleOwner.asyncStart(vararg actions: suspend () -> T): List<T> {
    return lifecycleScope.async {
        actions.map { async { it() } }.awaitAll()
    }.run {
        this.await()
    }
}

fun <T> LifecycleOwner.flowStart(action: suspend () -> T): Flow<T> {
    return flow {
        emit(action())
    }
}

fun LifecycleOwner.launchSync(action: suspend () -> Unit) {
    lifecycleScope.launch {
        try {
            action()
        } catch (e: CancellationException) {
            // 处理协程取消
            LogCat.e("协程取消--Sync", tr = e)
        } catch (e: Exception) {
            // 处理其他异常
            "Exception: ${e.message}".logE()
        }
    }
}

fun LifecycleOwner.launchAsync(action: suspend () -> Unit) {
    lifecycleScope.launch(Dispatchers.Default) {
        try {
            action()
        } catch (e: CancellationException) {
            // 处理协程取消
            LogCat.e("协程取消--Async")
        } catch (e: Exception) {
            // 处理其他异常
            "Exception: ${e.message}".logE()
        }
    }
}

//--------------------------------viewModelScope-----------------------------------------

fun <T> ViewModel.launchAsync(action: suspend () -> T): Deferred<T> {
    return viewModelScope.async {
        action()
    }
}

fun ViewModel.launchSafe(action: suspend () -> Unit) {
    viewModelScope.launch {
        try {
            action()
        } catch (e: Exception) {
            // 处理异常
        }
    }
}


//------------------------------------------------------------------------------------------

fun CoroutineScope.launchSafe(action: suspend () -> Unit) {
    launch {
        try {
            action()
        } catch (e: Exception) {
            // 处理异常
        }
    }
}

fun <T> CoroutineScope.launchAsync(action: suspend () -> T): Deferred<T> {
    return async {
        action()
    }
}


fun <T> flowStart(action: suspend () -> T): Flow<T> {
    return flow {
        emit(action())
    }
}

suspend fun <T> withIOContext(action: suspend () -> T): T {
    return withContext(Dispatchers.IO) {
        action()
    }
}

suspend fun <T> withMainContext(action: suspend () -> T): T {
    return withContext(Dispatchers.Main) {
        action()
    }
}
