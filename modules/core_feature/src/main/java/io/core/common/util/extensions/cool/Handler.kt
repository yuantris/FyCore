package io.core.common.util.extensions.cool

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import io.core.common.util.log.LogPure
import io.core.common.util.tools.buildMainHandler
import java.util.concurrent.atomic.AtomicBoolean

// 主（UI）线程运行
fun <T> T.runMain(
    action: () -> Unit
) where T : Any? {
    when {
        // 主线程立即执行
        isMainThread() -> {
            if (isSafeToRun()) action()
        }
        // 子线程通过 Handler 提交
        else -> MainThreadHandler.handler.post {
            if (isSafeToRun()) action()
        }
    }
}

// 新增支持取消的延迟提交
fun <T> T.runDelayedMain(
    duration: Long,
    action: () -> Unit
): Disposable where T : Any? {
    val disposable = Disposable()
    val runnable = Runnable {
        if (disposable.active && isSafeToRun()) {
            action()
        }
    }

    MainThreadHandler.handler.postDelayed(runnable, duration)
    disposable.runnable = runnable
    return disposable
}

// 新增可取消的操作封装
class Disposable {
    private val _active = AtomicBoolean(true)
    val active: Boolean get() = _active.get()
    var runnable: Runnable? = null
        set(value) {
            synchronized(this) {
                field = value
                if (!active) cancel()
            }
        }

    fun cancel() {
        synchronized(this) {
            if (_active.compareAndSet(true, false)) {
                runnable?.let { MainThreadHandler.handler.removeCallbacks(it) }
            }
        }
    }
}

// 优化检查逻辑为内部方法
private fun <T> T.isSafeToRun(): Boolean where T : Any? {
    return when (this) {
        is Fragment -> isAdded && activity?.isFinishing == false
        is Activity -> !isFinishing && !isDestroyed
        else -> {
            // 可选：添加日志警告
            LogPure.w(message = "Unknown type for isSafeToRun: $this")
            true
        }
    }
}

object MainThreadHandler {
    val handler: Handler by lazy { buildMainHandler() }
}

fun isMainThread(): Boolean {
    return Looper.getMainLooper().thread == Thread.currentThread()
}
