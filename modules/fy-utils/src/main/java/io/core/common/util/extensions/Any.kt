package io.core.common.util.extensions

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LifecycleOwner
import io.core.appCtx
import io.core.common.util.extensions.cool.runDelayedMain
import io.core.common.util.extensions.cool.timeFormat
import io.core.common.util.extensions.ui.appPackageName
import io.core.common.util.log.LogCat
import io.core.common.util.log.TAG
import io.core.constant.TimePatterns
import io.core.constant.X_FileProvider
import kotlin.system.exitProcess

inline fun <T> T?.verify(
    ifNull: () -> Unit = {},
    ifNotNull: (T) -> Unit = {}
) {
    if (this == null) {
        ifNull()
    } else {
        ifNotNull(this)
    }
}

/**
 * 获取类名(常用作TAG)
 */
fun Any.simpleName(): String = this::class.simpleName ?: "Unknown"

/**
 * 替换：【?: run { ... }】
 */
inline fun <T> T?.orElseRun(block: () -> T): T = this ?: block()

val currentTimeMillis: Long
    get() = System.currentTimeMillis()

val currentTime: String
    get() = currentTimeMillis.timeFormat()

val fileNameByTime: String
    get() = currentTimeMillis.timeFormat(TimePatterns.FILE_SAFE_TIMESTAMP)

val authority: String
    get() = "${appCtx.appPackageName}$X_FileProvider"

/**
 * 返回键回调
 */
fun OnBackPressedDispatcher.addCallback(
    owner: LifecycleOwner? = null,
    enabled: Boolean = true,
    onBackPressed: OnBackPressedCallback.() -> Unit
): OnBackPressedCallback {
    val callback = object : OnBackPressedCallback(enabled) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }
    }
    owner.verify(
        ifNull = {
            addCallback(callback)
        },
        ifNotNull = {
            addCallback(it, callback)
        }
    )
    return callback
}

fun Any?.logE(tag: String = TAG) {
    LogCat.e(this, tag = tag)
}

fun Any?.logV(tag: String = TAG) {
    LogCat.v(this, tag = tag)
}

fun Any?.logD(tag: String = TAG) {
    LogCat.d(this, tag = tag)
}

fun Any?.logI(tag: String = TAG) {
    LogCat.i(this, tag = tag)
}

fun Any?.logW(tag: String = TAG) {
    LogCat.w(this, tag = tag)
}

fun Any?.logJson(tag: String = TAG) {
    LogCat.json(this, tag = tag)
}
