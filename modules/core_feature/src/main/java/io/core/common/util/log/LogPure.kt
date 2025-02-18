package io.core.common.util.log

import android.util.Log
import io.core.Android

/**
 * 纯日志打印工具类()。
 */
object LogPure {

    private const val DEFAULT_TAG = TAG

    // 私有方法，用于统一处理日志记录逻辑
    private fun log(priority: Int, tag: String, message: String) {
        if (Android.debug) {
            Log.println(priority, tag, message)
        }
    }

    // 展示日志，默认输出到 Logcat
    @JvmStatic
    fun logD(message: String, tag: String = DEFAULT_TAG) {
        log(Log.DEBUG, tag, message)
    }

    fun logD(tag: String = DEFAULT_TAG, message: () -> String) {
        log(Log.DEBUG, tag, message())
    }

    @JvmStatic
    fun logI(message: String, tag: String = DEFAULT_TAG) {
        log(Log.INFO, tag, message)
    }

    fun logI(tag: String = DEFAULT_TAG, message: () -> String) {
        log(Log.INFO, tag, message())
    }

    @JvmStatic
    fun logW(message: String, tag: String = DEFAULT_TAG) {
        log(Log.WARN, tag, message)
    }

    fun logW(tag: String = DEFAULT_TAG, message: () -> String) {
        log(Log.WARN, tag, message())
    }

    @JvmStatic
    fun logE(message: String, tag: String = DEFAULT_TAG) {
        log(Log.ERROR, tag, message)
    }

    fun logE(tag: String = DEFAULT_TAG, message: () -> String) {
        log(Log.ERROR, tag, message())
    }

    @JvmStatic
    fun logV(message: String, tag: String = DEFAULT_TAG) {
        log(Log.VERBOSE, tag, message)
    }

    fun logV(tag: String = DEFAULT_TAG, message: () -> String) {
        log(Log.VERBOSE, tag, message())
    }

}

