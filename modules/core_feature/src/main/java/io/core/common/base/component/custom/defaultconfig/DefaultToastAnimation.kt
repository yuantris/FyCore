package io.core.common.base.component.custom.defaultconfig

import android.view.View
import io.core.common.base.component.custom.strategy.ToastAnimationStrategy

/**
 * 默认吐司动画实现
 */
class DefaultToastAnimation : ToastAnimationStrategy {
    override fun playShowAnimation(view: View, duration: Long, onComplete: (() -> Unit)?) {
        view.alpha = 0f
        view.scaleX = 0.95f
        view.scaleY = 0.95f

        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .withEndAction(onComplete)
            .start()
    }

    override fun playDismissAnimation(view: View, duration: Long, onComplete: (() -> Unit)?) {
        view.animate()
            .alpha(0f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(duration)
            .withEndAction(onComplete)
            .start()
    }
}