package com.core.fy.android.widget.rv.animations

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import com.core.fy.android.widget.rv.animations.BaseAnimation

class SlideInBottomAnimation : BaseAnimation {


    override fun getAnimators(view: View): Array<Animator> =
        arrayOf(ObjectAnimator.ofFloat(view, "translationY", view.measuredHeight.toFloat(), 0f))
}
