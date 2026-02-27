package com.core.fy.android.function.toast

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import io.core.ui.base.component.custom.strategy.ToastPositionStrategy
import io.core.utils.tools.SizeTools

class TopPositionStrategy : ToastPositionStrategy {
    override fun createLayoutParams(context: Context): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            SizeTools.getScreenHeight(),
            WindowManager.LayoutParams.TYPE_APPLICATION,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // 允许覆盖状态栏
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = getDefaultGravity()
            x = 0
            y = -getStatusBarHeight(context) // 负值使其从屏幕顶部开�?
        }
    }


    override fun getDefaultGravity(): Int = Gravity.TOP

    /**
     * 获取状态栏高度
     */
    @SuppressLint("InternalInsetResource")
    private fun getStatusBarHeight(context: Context): Int {
        var statusBarHeight = 0
        val resourceId = context.resources.getIdentifier(
            "status_bar_height", "dimen", "android"
        )
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }
}