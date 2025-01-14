package com.core.libraries.common.util.ext.cool

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.core.libraries.common.helper.coroutine.Coroutine
import com.core.libraries.common.util.log.logE
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

suspend fun <T> withTimeoutAsync(delayMillis: Long, block: suspend CoroutineScope.() -> T): T {
    return suspendCancellableCoroutine { cout ->
        Coroutine.async(context = cout.context) {
            launch {
                delay(delayMillis)
                if (!cout.isCompleted) {
                    cout.resumeWithException(TimeoutCancellationException("Timed out waiting for $delayMillis ms"))
                }
            }
            val result = block()
            if (!cout.isCompleted) {
                cout.resume(result)
            }
        }
    }
}

suspend fun <T> withTimeoutOrNullAsync(delayMillis: Long, block: suspend CoroutineScope.() -> T): T? {
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
            "协程取消: $e".logE()
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
            "协程取消".logE()
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

//----------------------------------GlobalScope---------------------------------------------

@OptIn(DelicateCoroutinesApi::class)
fun launchGlobal(action: suspend () -> Unit) {
    GlobalScope.launch {
        action()
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun launchGlobalSafe(action: suspend () -> Unit) {
    GlobalScope.launch {
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
