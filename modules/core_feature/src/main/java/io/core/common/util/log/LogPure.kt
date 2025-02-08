package io.core.common.util.log

import android.util.Log
import io.core.Android

object LogPure {

    private const val DEFAULT_TAG = TAG

    // 私有方法，用于统一处理日志记录逻辑
    private fun log(priority: Int, tag: String, message: String) {
        if (Android.debug) {
            Log.println(priority, tag, message)
        }
    }

    // 展示日志，默认输出到 Logcat
    fun logD(message: String, tag: String = DEFAULT_TAG) {
        log(Log.DEBUG, tag, message)
    }

    fun logI(message: String, tag: String = DEFAULT_TAG) {
        log(Log.INFO, tag, message)
    }

    fun logW(message: String, tag: String = DEFAULT_TAG) {
        log(Log.WARN, tag, message)
    }

    fun logE(message: String, tag: String = DEFAULT_TAG) {
        log(Log.ERROR, tag, message)
    }

    fun logV(message: String, tag: String = DEFAULT_TAG) {
        log(Log.VERBOSE, tag, message)
    }

}

fun Throwable.printOnDebug() {
    if (Android.debug) {
        printStackTrace()
    }
}
