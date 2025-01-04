package com.core.libraries.base.ext

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import com.core.libraries.other.Toast

fun Context.startActivity(clazz: Class<*>) {
    val intent = Intent(this, clazz)
    if (this !is Activity) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    startActivity(intent)
}

fun Activity.startNoTransition(clazz: Class<*>, finish: Boolean = true) {
    startActivity(clazz)
    overridePendingTransition(0, 0)
    if (finish) finish()
}

/**
 * @return 上下文中的Activity对象
 */
fun Context.getActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

/**
 * 显示Toast
 */
fun Context.toast(message: String) {
    Toast.Builder(this)
        .setMessage(message)
        .create()
        .show()
}

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
    if (isMainThread()) {
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
    if (isMainThread()) {
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
    return Looper.getMainLooper().thread == Thread.currentThread()
}