package com.core.fy.android.util

import android.content.Context
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes

fun loadAnimation(context: Context, @AnimRes id: Int): Animation {
    val animation = AnimationUtils.loadAnimation(context, id)
//    if (AppConfig.isEInkMode) {
//        animation.duration = 0
//    }
    return animation
}