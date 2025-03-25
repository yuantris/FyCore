package io.core.common.helper.coroutine.info

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

class CoroutineLauncher private constructor() {
    private var weakOwner: WeakReference<LifecycleOwner>? = null
    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("CoroutineLauncher", "Coroutine error: ${throwable.message}")
    }

    private val scope: CoroutineScope by lazy {
        CoroutineScope(job + Dispatchers.Main.immediate + exceptionHandler)
    }

    companion object {
        fun bind(lifecycleOwner: LifecycleOwner): CoroutineLauncher {
            return CoroutineLauncher().apply {
                setupWithLifecycle(lifecycleOwner)
            }
        }

        fun global(): CoroutineLauncher {
            return CoroutineLauncher().apply {
                setupGlobalScope()
            }
        }
    }

    private fun setupWithLifecycle(lifecycleOwner: LifecycleOwner) {
        weakOwner = WeakReference(lifecycleOwner)
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                dispose()
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        })
    }

    private fun setupGlobalScope() {
        // 全局作用域需要手动管理生命周期
        job.invokeOnCompletion { 
            Log.d("CoroutineLauncher", "Global scope terminated") 
        }
    }

    fun launch(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        block: suspend CoroutineScope.() -> Unit
    ): Job = scope.launch(dispatcher) { block() }

    fun dispose() {
        job.cancel("CoroutineLauncher disposed")
        weakOwner?.clear()
    }

    // 可选：自动取消的扩展函数
    fun LifecycleOwner.launchSafe(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        block: suspend CoroutineScope.() -> Unit
    ) = bind(this).launch(dispatcher, block)
}