package io.core.utils.log

import android.util.Log
import io.core.base.Android

/**
 * 纯日志打印工具类()�?
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
    @JvmOverloads
    fun d(tag: String = DEFAULT_TAG, message: String) {
        log(Log.DEBUG, tag, message)
    }


    fun d(tag: String = DEFAULT_TAG, message: () -> String) {
        log(Log.DEBUG, tag, message())
    }

    @JvmStatic
    @JvmOverloads
    fun i(tag: String = DEFAULT_TAG, message: String) {
        log(Log.INFO, tag, message)
    }

    fun i(tag: String = DEFAULT_TAG, message: () -> String) {
        log(Log.INFO, tag, message())
    }

    @JvmStatic
    @JvmOverloads
    fun w(tag: String = DEFAULT_TAG, message: String) {
        log(Log.WARN, tag, message)
    }

    fun w(tag: String = DEFAULT_TAG, message: () -> String) {
        log(Log.WARN, tag, message())
    }

    @JvmStatic
    @JvmOverloads
    fun e(tag: String = DEFAULT_TAG, message: String) {
        log(Log.ERROR, tag, message)
    }

    fun e(tag: String = DEFAULT_TAG, message: () -> String) {
        log(Log.ERROR, tag, message())
    }

    @JvmStatic
    @JvmOverloads
    fun v(tag: String = DEFAULT_TAG, message: String) {
        log(Log.VERBOSE, tag, message)
    }

    fun v(tag: String = DEFAULT_TAG, message: () -> String) {
        log(Log.VERBOSE, tag, message())
    }

}

