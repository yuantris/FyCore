package io.core.common.helper.rv.animations

import android.animation.Animator
import android.view.View

/**
 * adapter item 动画
 */
interface BaseAnimation {

    fun getAnimators(view: View): Array<Animator>

}
