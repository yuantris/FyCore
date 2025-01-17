package io.core.common.util.ext

import android.annotation.SuppressLint
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.ColorInt
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.hjq.shape.drawable.ShapeDrawable

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

inline fun Any?.verify(fail: () -> Unit = {}, success: () -> Unit = {}) {
    if (this == null) fail() else success()
}

@SuppressLint("NotifyDataSetChanged")
fun RecyclerView.Adapter<*>.notifyAllDataChanged() {
    this.notifyDataSetChanged()
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
    if (owner != null) {
        addCallback(owner, callback)
    } else {
        addCallback(callback)
    }
    return callback
}

