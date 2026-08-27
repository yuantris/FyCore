package io.core.common.base.component.custom.defaultconfig

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import io.core.common.base.component.custom.strategy.ToastPositionStrategy
import io.core.common.util.extensions.cool.dpToPx

/**
 * 默认吐司位置实现
 */
class DefaultToastPosition : ToastPositionStrategy {
    override fun createLayoutParams(context: Context): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = getDefaultGravity()
            x = 0
            y = 120.dpToPx()
        }
    }

    override fun getDefaultGravity(): Int = Gravity.BOTTOM
}