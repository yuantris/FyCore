package com.core.fy.android.util

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

/**
 * ClickSequenceHandler 用于处理视图的点击序列和长按事件。
 * - 监听连续三次快速点击后，进入长按等待状态。
 * - 如果在指定时间内长按，则触发隐藏操作。
 * - 否则，超时后重置状态。
 *
 * @param view 需要监听点击事件的视图
 * @param hideAction 长按触发的隐藏操作回调
 */
class ClickSequenceHandler(private val view: View, private val hideAction: () -> Unit) {
    private var clickCount = 0
    private var lastClickTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var isWaitingForLongPress = false

    // 时间常量
    private companion object {
        const val CLICK_INTERVAL = 500L    // 点击间隔阈值（毫秒）
        const val LONG_PRESS_DURATION = 1000L // 需要长按的时长
        const val WAIT_TIMEOUT = 3000L     // 等待长按的超时时间
    }

    init {
        setupClickListeners()
    }

    private fun setupClickListeners() {
        view.setOnClickListener {
            if (isWaitingForLongPress) {
                resetState()
                return@setOnClickListener
            }

            val currentTime = System.currentTimeMillis()
            clickCount = if (currentTime - lastClickTime < CLICK_INTERVAL) {
                clickCount + 1
            } else {
                1
            }
            lastClickTime = currentTime

            if (clickCount == 3) {
                isWaitingForLongPress = true
                setupLongPressListener()
                handler.postDelayed(::resetState, WAIT_TIMEOUT)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupLongPressListener() {
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    handler.postDelayed(::triggerHideAction, LONG_PRESS_DURATION)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(::triggerHideAction)
                    resetState()
                    true
                }
                else -> false
            }
        }
    }

    private fun triggerHideAction() {
        hideAction()
        resetState()
    }

    private fun resetState() {
        handler.removeCallbacksAndMessages(null)
        clickCount = 0
        lastClickTime = 0L
        isWaitingForLongPress = false
        view.setOnTouchListener(null)
        setupClickListeners()
    }
}
