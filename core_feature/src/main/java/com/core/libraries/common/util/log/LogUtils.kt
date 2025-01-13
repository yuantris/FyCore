package com.core.libraries.common.util.log

import android.util.Log
import com.core.libraries.Android
import com.core.libraries.common.util.ext.tool.TAG

object LogUtils {

    private const val DEFAULT_TAG = TAG

    // 展示日志，默认输出到 Logcat
    fun logD(tag: String = DEFAULT_TAG, message: String) {
        val stackTraceElement = getStackTraceElement()
        val logMessage = formatLogMessage(stackTraceElement, message)
        Log.d(tag, logMessage)
    }

    fun logI(tag: String = DEFAULT_TAG, message: String) {
        val stackTraceElement = getStackTraceElement()
        val logMessage = formatLogMessage(stackTraceElement, message)
        Log.i(tag, logMessage)
    }

    fun logW(tag: String = DEFAULT_TAG, message: String) {
        val stackTraceElement = getStackTraceElement()
        val logMessage = formatLogMessage(stackTraceElement, message)
        Log.w(tag, logMessage)
    }

    fun logE(tag: String = DEFAULT_TAG, message: String) {
        val stackTraceElement = getStackTraceElement()
        val logMessage = formatLogMessage(stackTraceElement, message)
        Log.e(tag, logMessage)
    }

    fun logV(tag: String = DEFAULT_TAG, message: String) {
        val stackTraceElement = getStackTraceElement()
        val logMessage = formatLogMessage(stackTraceElement, message)
        Log.v(tag, logMessage)
    }

    // 获取调用日志的堆栈信息
    private fun getStackTraceElement(): StackTraceElement {
        val stackTrace = Thread.currentThread().stackTrace
        // 获取调用log方法的调用者栈信息
        return stackTrace[4]  // 堆栈位置4是调用log方法的地方
    }

    // 格式化日志输出内容
    private fun formatLogMessage(stackTraceElement: StackTraceElement, message: String): String {
        return "(${stackTraceElement.fileName}:${stackTraceElement.lineNumber}) ${stackTraceElement.methodName}: $message"
    }
}

fun Throwable.printOnDebug() {
    if (Android.debug) {
        printStackTrace()
    }
}

