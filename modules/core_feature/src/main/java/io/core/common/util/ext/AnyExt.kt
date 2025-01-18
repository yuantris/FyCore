package io.core.common.util.ext

import android.annotation.SuppressLint
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import io.core.common.helper.LifecycleHelp
import io.core.common.util.ext.ui.postDelayUI
import kotlin.system.exitProcess

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/3 17:58
 * @description
 * @author Yuan
 */

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


@SuppressLint("NotifyDataSetChanged")
fun RecyclerView.Adapter<*>.notifyAllDataChanged() {
    this.notifyDataSetChanged()
}

fun Any?.exitApp() {
    LifecycleHelp.finishAllActivity()
    postDelayUI(10) {
        exitProcess(0)
    }
}

val currentTimeMillis: Long
    get() = System.currentTimeMillis()

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

