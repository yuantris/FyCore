package io.core.common.util.extensions.cool

import io.core.common.util.log.LogCat
import io.core.common.util.log.TAG

fun Throwable.logPrint(tag: String = TAG) {
    LogCat.e(this, tag)
}