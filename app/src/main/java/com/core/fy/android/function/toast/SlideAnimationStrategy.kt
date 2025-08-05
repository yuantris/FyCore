package com.core.fy.android.function.toast

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import io.core.Android.context
import io.core.common.base.component.custom.strategy.ToastAnimationStrategy

class SlideAnimationStrategy : ToastAnimationStrategy {

    private fun getSlideHeight(): Int {
        return (50 * context.resources.displayMetrics.density).toInt()
    }

    override fun playShowAnimation(view: View, duration: Long, onComplete: (() -> Unit)?) {
        // 从屏幕顶部外部开始（负的View高度）
        val startY = -getSlideHeight().toFloat()

        // 目标位置：距离屏幕顶部200dp（正值向下）
        val targetY = getSlideHeight().toFloat()

        view.translationY = startY
        view.alpha = 0f

        view.animate()
            .translationY(targetY)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction(onComplete)
            .start()
    }



    override fun playDismissAnimation(view: View, duration: Long, onComplete: (() -> Unit)?) {
        // 从当前位置返回到屏幕顶部外部
        val endY = -getSlideHeight().toFloat()

        view.animate()
            .translationY(endY)
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction(onComplete)
            .start()
    }
}