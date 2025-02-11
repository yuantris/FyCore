package io.core.engine.brv.listener

import android.os.SystemClock
import android.view.View
import io.core.engine.brv.utils.BRV


internal fun View.setOnDebounceClickListener(interval: Long = 500, block: View.() -> Unit) {
    setOnClickListener(OnDebounceClickListener(interval, block))
}

private class OnDebounceClickListener(
    private val interval: Long = 500,
    private var block: View.() -> Unit
) : View.OnClickListener {

    private var _lastDebounceClickTime: Long = 0
    private var lastDebounceClickTime: Long
        get() = if (BRV.debounceGlobalEnabled) BRV.lastDebounceClickTime else _lastDebounceClickTime
        set(value) = if (BRV.debounceGlobalEnabled) BRV.lastDebounceClickTime =
            value else _lastDebounceClickTime = value

    override fun onClick(v: View) {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastDebounceClickTime > interval) {
            lastDebounceClickTime = currentTime
            block(v)
        }
    }
}