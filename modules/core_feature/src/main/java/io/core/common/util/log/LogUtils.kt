package io.core.common.util.log

import io.core.Android

object LogUtils {

    private const val DEFAULT_TAG = TAG

    // 展示日志，默认输出到 Logcat
    fun logD(tag: String = DEFAULT_TAG, message: String) {
        LogCat.d(message, tag)
    }

    fun logI(tag: String = DEFAULT_TAG, message: String) {
        LogCat.i(message, tag)
    }

    fun logW(tag: String = DEFAULT_TAG, message: String) {
        LogCat.w(message, tag)
    }

    fun logE(tag: String = DEFAULT_TAG, message: String) {
        LogCat.e(message, tag)
    }

    fun logV(tag: String = DEFAULT_TAG, message: String) {
        LogCat.v(message, tag)
    }

}

fun Throwable.printOnDebug() {
    if (Android.debug) {
        printStackTrace()
    }
}

