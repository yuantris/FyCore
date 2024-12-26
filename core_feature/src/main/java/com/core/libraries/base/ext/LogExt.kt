package com.core.libraries.base.ext

import android.util.Log

const val TAG = "FyCore_"

var isLog = true

private enum class LEVEL {
    V, D, I, W, E
}

fun String.logV(tag: String = TAG) =
    log(LEVEL.V, tag, this)

fun String.logD(tag: String = TAG) =
    log(LEVEL.D, tag, this)

fun String.logI(tag: String = TAG) =
    log(LEVEL.I, tag, this)

fun String.logW(tag: String = TAG) =
    log(LEVEL.W, tag, this)

fun String.logE(tag: String = TAG) =
    log(LEVEL.E, tag, this)

private fun log(level: LEVEL, tag: String, message: String) {
    if (!isLog) return
    val stackTraceElement = getActualStackTraceElement()
    val logMessage = formatLogMessage(stackTraceElement, message)
    when (level) {
        LEVEL.V -> Log.v(tag, logMessage)
        LEVEL.D -> Log.d(tag, logMessage)
        LEVEL.I -> Log.i(tag, logMessage)
        LEVEL.W -> Log.w(tag, logMessage)
        LEVEL.E -> Log.e(tag, logMessage)
    }
}

// 获取调用日志的堆栈信息
private fun getActualStackTraceElement(): StackTraceElement {
    val stackTrace = Throwable().stackTrace
    // 获取堆栈中的第 4 个元素
    return stackTrace[4]
}

// 格式化日志输出内容
private fun formatLogMessage(stackTraceElement: StackTraceElement, message: String): String {
    return "(${stackTraceElement.fileName}:${stackTraceElement.lineNumber}) <${stackTraceElement.methodName}>---> $message"
}