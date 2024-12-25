package com.core.libraries.base.ext

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment


/**
 * 将Activity移到前台，需将launchMode设置为SingleTop，否则会创建新实例
 */
fun Activity.moveTaskToFront(context: Context) {
    val intent = Intent(context, this.javaClass)
    intent.flags =
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
    context.startActivity(intent)
}

fun <T> T.postUI(action: () -> Unit) {

    // 创建一个Handler实例指向主线程的Looper
    val mainHandler = MainLooper.handler

    // 如果当前已经是主线程，直接执行action
    if (Looper.getMainLooper().thread == Thread.currentThread()) {
        action()
        return
    }

    // 对于Fragment和Activity的特定检查
    when (this) {
        is Fragment -> {
            if (!isAdded || activity == null || activity!!.isFinishing) {
                return
            }
        }

        is Activity -> {
            if (isFinishing) {
                return
            }
        }
    }

    // 利用Handler将操作post到主线程执行
    mainHandler.post { action() }
}

fun <T> T.postDelayUI(duration: Long, action: () -> Unit) {
    val mainHandler = MainLooper.handler

    // 判断执行线程如果已经是主线程，直接使用Handler处理延迟
    if (Looper.getMainLooper() == Looper.myLooper()) {
        mainHandler.postDelayed({ action() }, duration)
        return
    }

    // Fragment
    if (this is Fragment) {
        if (!isAdded) return
        val activity = activity ?: return
        if (activity.isFinishing) return
        mainHandler.postDelayed({ activity.runOnUiThread(action) }, duration)
        return
    }

    // Activity
    if (this is Activity) {
        if (isFinishing) return
        mainHandler.postDelayed({ runOnUiThread(action) }, duration)
        return
    }

    // 在子线程中，无需检查线程，直接使用Handler处理延迟
    mainHandler.postDelayed({ action() }, duration)
}

object MainLooper {
    val handler: Handler by lazy { Handler(Looper.getMainLooper()) }
}

fun isMainThread(): Boolean {
    return Looper.getMainLooper() == Looper.myLooper()
}