package com.core.fy.android.function.yunchuang

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

class AnimationDsl(private val view: View) {
    private val animators = mutableListOf<Animator>()

    // 基础动画方法
    fun translateX(to: Float, config: ObjectAnimator.() -> Unit = {}) = 
        addAnimator(View.TRANSLATION_X.name, to, config)

    fun translateY(to: Float, config: ObjectAnimator.() -> Unit = {}) = 
        addAnimator(View.TRANSLATION_Y.name, to, config)

    fun alpha(to: Float, config: ObjectAnimator.() -> Unit = {}) = 
        addAnimator(View.ALPHA.name, to, config)

    fun rotation(to: Float, config: ObjectAnimator.() -> Unit = {}) = 
        addAnimator(View.ROTATION.name, to, config)

    fun rotationX(to: Float, config: ObjectAnimator.() -> Unit = {}) = 
        addAnimator(View.ROTATION_X.name, to, config)

    fun rotationY(to: Float, config: ObjectAnimator.() -> Unit = {}) = 
        addAnimator(View.ROTATION_Y.name, to, config)

    fun scaleX(to: Float, config: ObjectAnimator.() -> Unit = {}) = 
        addAnimator(View.SCALE_X.name, to, config)

    fun scaleY(to: Float, config: ObjectAnimator.() -> Unit = {}) = 
        addAnimator(View.SCALE_Y.name, to, config)

    fun scale(to: Float, config: ObjectAnimator.() -> Unit = {}) {
        scaleX(to, config)
        scaleY(to, config)
    }

    // 组合动画
    fun parallel(block: AnimationDsl.() -> Unit) {
        val child = AnimationDsl(view).apply(block)
        animators += AnimatorSet().apply { playTogether(child.animators) }
    }

    fun sequence(block: AnimationDsl.() -> Unit) {
        val child = AnimationDsl(view).apply(block)
        animators += AnimatorSet().apply { playSequentially(child.animators) }
    }

    // 构建最终动�?
    internal fun build(): AnimatorSet = AnimatorSet().apply {
        playTogether(animators)
        interpolator = LinearInterpolator()
    }

    private fun addAnimator(property: String, to: Float, config: ObjectAnimator.() -> Unit) {
        animators += ObjectAnimator.ofFloat(view, property, to).apply(config)
    }
}

// View扩展函数
fun View.animations(block: AnimationDsl.() -> Unit): AnimatorSet {
    return AnimationDsl(this).apply(block).build()
}

// 快捷方法
fun AnimatorSet.start() = this.apply { start() }
