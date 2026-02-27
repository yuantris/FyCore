package io.core.ui.base.component.custom.defaultconfig

import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import io.core.ui.base.component.custom.strategy.ToastAnimationStrategy

/**
 * 默认吐司动画实现
 * 支持多种动画类型和自定义配置
 */
class DefaultToastAnimation(
    private val animationType: AnimType = AnimType.FADE,
    private val interpolator: Interpolator = AccelerateDecelerateInterpolator()
) : ToastAnimationStrategy {

    enum class AnimType {
        FADE, SCALE, SLIDE_UP, SLIDE_DOWN
    }

    override fun playShowAnimation(view: View, duration: Long, onComplete: (() -> Unit)?) {
        when (animationType) {
            AnimType.FADE -> playFadeShowAnimation(view, duration, onComplete)
            AnimType.SCALE -> playScaleShowAnimation(view, duration, onComplete)
            AnimType.SLIDE_UP -> playSlideShowAnimation(view, duration, onComplete, true)
            AnimType.SLIDE_DOWN -> playSlideShowAnimation(view, duration, onComplete, false)
        }
    }

    override fun playDismissAnimation(view: View, duration: Long, onComplete: (() -> Unit)?) {
        when (animationType) {
            AnimType.FADE -> playFadeDismissAnimation(view, duration, onComplete)
            AnimType.SCALE -> playScaleDismissAnimation(view, duration, onComplete)
            AnimType.SLIDE_UP -> playSlideDismissAnimation(view, duration, onComplete, true)
            AnimType.SLIDE_DOWN -> playSlideDismissAnimation(view, duration, onComplete, false)
        }
    }

    /**
     * 淡入动画
     */
    private fun playFadeShowAnimation(view: View, duration: Long, onComplete: (() -> Unit)?) {
        view.alpha = 0f
        view.scaleX = 0.95f
        view.scaleY = 0.95f

        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .withEndAction(onComplete)
            .start()
    }

    /**
     * 淡出动画
     */
    private fun playFadeDismissAnimation(view: View, duration: Long, onComplete: (() -> Unit)?) {
        view.animate()
            .alpha(0f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .withEndAction(onComplete)
            .start()
    }

    /**
     * 缩放显示动画
     */
    private fun playScaleShowAnimation(view: View, duration: Long, onComplete: (() -> Unit)?) {
        view.scaleX = 0f
        view.scaleY = 0f

        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .withEndAction(onComplete)
            .start()
    }

    /**
     * 缩放消失动画
     */
    private fun playScaleDismissAnimation(view: View, duration: Long, onComplete: (() -> Unit)?) {
        view.animate()
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .withEndAction(onComplete)
            .start()
    }

    /**
     * 滑动动画
     * @param isUp true为向上滑入，false为向下滑�?
     */
    private fun playSlideShowAnimation(view: View, duration: Long, onComplete: (() -> Unit)?, isUp: Boolean) {
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 安全地移除监听器
                if (view.viewTreeObserver.isAlive) {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }

                val startTranslation = if (isUp) view.height.toFloat() else -view.height.toFloat()
                view.translationY = startTranslation

                view.animate()
                    .translationY(0f)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .withEndAction(onComplete)
                    .start()
            }
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    /**
     * 滑动消失动画
     * @param isUp true为向上滑出，false为向下滑�?
     */
    private fun playSlideDismissAnimation(view: View, duration: Long, onComplete: (() -> Unit)?, isUp: Boolean) {
        val endTranslation = if (isUp) -view.height.toFloat() else view.height.toFloat()

        view.animate()
            .translationY(endTranslation)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .withEndAction(onComplete)
            .start()
    }
}