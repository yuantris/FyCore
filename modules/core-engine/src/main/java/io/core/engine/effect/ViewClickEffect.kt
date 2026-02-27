package io.core.engine.effect

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import java.lang.ref.WeakReference

object ViewClickEffect {

    // 默认参数
    private const val DEFAULT_SCALE = 0.95f
    private const val DEFAULT_SCALE_DURATION = 150L

    // 使用 WeakReference 防止内存泄漏
    private val originalClickListeners = mutableMapOf<Int, WeakReference<View.OnClickListener>>()

    /**
     * 为多�?View 添加缩放效果
     * @param views 可变参数接收多个 View
     * @param scale 缩放比例
     * @param duration 动画时长
     */
    fun applyScaleToViews(vararg views: View, scale: Float = DEFAULT_SCALE, duration: Long = DEFAULT_SCALE_DURATION) {
        views.forEach { view ->
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> animateScale(v, scale, duration)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animateScale(v, 1f, duration)
                }
                false // 不消费事�?
            }
        }
    }


    private fun animateScale(view: View, scale: Float, duration: Long) {
        ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, scale),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, scale)
        ).apply {
            this.duration = duration
            interpolator = OvershootInterpolator()
            start()
        }
    }

    private fun animateShake(view: View, shakeRange: Float, duration: Long) {
        AnimatorSet().apply {
            playSequentially(
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, -shakeRange),
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, shakeRange),
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, -shakeRange),
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, shakeRange),
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f)
            )
            this.duration = duration
            start()
        }
    }

    /**
     * 清除指定 View 的所有效�?
     */
    fun clearEffects(vararg views: View) {
        views.forEach { view ->
            view.setOnTouchListener(null)
            view.setOnClickListener(originalClickListeners[view.id]?.get())
            originalClickListeners.remove(view.id)
        }
    }
}