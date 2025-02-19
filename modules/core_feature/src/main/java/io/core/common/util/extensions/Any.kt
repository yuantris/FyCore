package io.core.common.util.extensions

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LifecycleOwner
import io.core.appCtx
import io.core.common.helper.AppLifecycleTracker
import io.core.common.util.extensions.cool.currentTimeFormat
import io.core.common.util.extensions.cool.postDelayUI
import io.core.common.util.extensions.ui.appPackageName
import io.core.common.util.log.LogCat
import kotlin.system.exitProcess


inline fun <T> T?.ifNotNull(action: (T) -> Unit) {
    if (this != null) action(this)
}

inline fun Any?.ifNull(action: () -> Unit) {
    if (this == null) {
        action()
    }
}

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


fun Any?.exitApp() {
    AppLifecycleTracker.finishAllActivity()
    postDelayUI(10) {
        exitProcess(0)
    }
}

val currentTimeMillis: Long
    get() = System.currentTimeMillis()

val currentTime: String
    get() = currentTimeMillis.currentTimeFormat()

val authority: String
    get() = "${appCtx.appPackageName}.fycore.fileprovider"

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

fun Any?.logE() {
    LogCat.e(this)
}

fun Any?.logV() {
    LogCat.v(this)
}

fun Any?.logD() {
    LogCat.d(this)
}

fun Any?.logI() {
    LogCat.i(this)
}

fun Any?.logW() {
    LogCat.w(this)
}

fun Any?.logJson() {
    LogCat.json(this)
}

