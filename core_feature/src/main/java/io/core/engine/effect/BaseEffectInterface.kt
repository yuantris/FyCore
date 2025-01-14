package io.core.engine.effect

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.OvershootInterpolator

interface BaseEffectInterface {

    val targetView: View      // 目标控件
    val effectType: ClickEffectType // 选中的点击效果类型

    // 点击缩放效果
    fun startScaleEffect() {
        ObjectAnimator.ofFloat(targetView, "scaleX", 0.9f).apply {
            duration = 150
            start()
        }
        ObjectAnimator.ofFloat(targetView, "scaleY", 0.9f).apply {
            duration = 150
            start()
        }
    }

    // 恢复缩放效果
    fun resetScaleEffect() {
        ObjectAnimator.ofFloat(targetView, "scaleX", 1f).apply {
            duration = 150
            interpolator = OvershootInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(targetView, "scaleY", 1f).apply {
            duration = 150
            interpolator = OvershootInterpolator()
            start()
        }
    }



}
