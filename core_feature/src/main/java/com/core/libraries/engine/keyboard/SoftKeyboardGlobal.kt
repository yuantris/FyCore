package com.core.libraries.engine.keyboard

import android.app.Application
import java.util.LinkedList

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2024/12/24 8:48
 * @description
 * @author Yuan
 */
object SoftKeyboardGlobal : KeyboardObserver.Callback {

    private var isInstalled = false
    var isDebug: Boolean = false
        private set

    private val callbacks by lazy {
        LinkedList<SoftKeyboardCallback>()
    }

    private var lastHeight = 0

    fun install(app: Application, debug: Boolean = false) {
        isDebug = debug
        if (isInstalled) {
            return
        }
        app.registerActivityLifecycleCallbacks(ObserverManager())
        isInstalled = true
    }

    fun addSoftKeyboardCallback(callback: SoftKeyboardCallback) {
        if (callbacks.contains(callback)) {
            return
        }
        callbacks.add(callback)
    }

    fun removeSoftKeyboardCallback(callback: SoftKeyboardCallback) {
        callbacks.remove(callback)
    }

    override fun onKeyboardHeightChanged(height: Int) {
        val cbs = ArrayList(callbacks)
        cbs.forEach {
            it.onHeightChanged(height)
        }
        if (lastHeight == 0 && height > 0) {
            cbs.forEach {
                it.onOpen(height)
            }
        } else if (lastHeight > 0 && height == 0) {
            cbs.forEach {
                it.onClose()
            }
        }
        cbs.clear()
        lastHeight = height
    }

    interface SoftKeyboardCallback {
        fun onOpen(height: Int)
        fun onClose()
        fun onHeightChanged(height: Int) {}
    }

}