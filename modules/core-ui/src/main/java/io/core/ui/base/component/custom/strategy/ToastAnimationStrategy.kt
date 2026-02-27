package io.core.ui.base.component.custom.strategy

import android.view.View

/**
 * 吐司动画策略接口
 * 定义吐司的显示和隐藏动画
 */
interface ToastAnimationStrategy {
    /**
     * 播放显示动画
     * @param view 要动画的视图
     * @param duration 动画时长
     * @param onComplete 动画完成回调
     */
    fun playShowAnimation(view: View, duration: Long = 300L, onComplete: (() -> Unit)? = null)

    /**
     * 播放隐藏动画
     * @param view 要动画的视图
     * @param duration 动画时长
     * @param onComplete 动画完成回调
     */
    fun playDismissAnimation(view: View, duration: Long = 300L, onComplete: (() -> Unit)? = null)
}