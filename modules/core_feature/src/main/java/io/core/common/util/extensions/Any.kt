package io.core.common.util.extensions

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.core.appCtx
import io.core.common.helper.AppLifecycleTracker
import io.core.common.util.extensions.cool.runDelayedMain
import io.core.common.util.extensions.cool.timeFormat
import io.core.common.util.extensions.ui.appPackageName
import io.core.common.util.log.LogCat
import io.core.constant.TimeFormat
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


fun Any?.exitApp() {
    AppLifecycleTracker.finishAllActivities()
    runDelayedMain(10) {
        exitProcess(0)
    }
}

val currentTimeMillis: Long
    get() = System.currentTimeMillis()

val currentTime: String
    get() = currentTimeMillis.timeFormat()

val fileNameByTime: String
    get() = currentTimeMillis.timeFormat(TimeFormat.FILE_SAFE_TIMESTAMP)

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

fun registerLifecycleObserver(observer: LifecycleObserver) {
    ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
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

