package io.core.common.util.ext

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LifecycleOwner

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

fun Any?.isNotNull(action: () -> Unit) {
    if (this != null) {
        action()
    }
}

fun Any?.isNull(action: () -> Unit) {
    if (this == null) {
        action()
    }
}

fun Any?.verify(action: (isNull: Boolean) -> Unit) {
    action(this == null)
}

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
    if (owner != null) {
        addCallback(owner, callback)
    } else {
        addCallback(callback)
    }
    return callback
}

