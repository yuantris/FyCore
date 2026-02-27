package io.core.utils.extensions.cool

import io.core.base.Android
import io.core.utils.log.LogCat
import io.core.utils.log.TAG

fun Throwable.logPrint(tag: String = TAG) {
    LogCat.e(this, tag)
}

fun Throwable.printOnDebug() {
    if (Android.debug) {
        printStackTrace()
    }
}