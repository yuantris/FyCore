@file:Suppress("unused")

package io.core.common.util.extensions.cool

import android.content.res.Resources
import kotlin.math.roundToInt


val Int.hexString: String
    get() = Integer.toHexString(this)

fun Int.dpToPx(): Int = this.toFloat().dpToPx().toInt()

fun Int.spToPx(): Int = this.toFloat().spToPx().toInt()

fun Int.pxToDp(): Int = this.toFloat().pxToDp().toInt()

fun Int.pxToSp(): Int = this.toFloat().pxToSp().toInt()

fun Float.dpToPx(): Float = android.util.TypedValue.applyDimension(
    android.util.TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics
)

fun Float.spToPx(): Float = android.util.TypedValue.applyDimension(
    android.util.TypedValue.COMPLEX_UNIT_SP, this, Resources.getSystem().displayMetrics
)

fun Float.pxToDp(): Float = this / Resources.getSystem().displayMetrics.density + 0.5f

fun Float.pxToSp(): Float = this / Resources.getSystem().displayMetrics.scaledDensity + 0.5f

val Float.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()

val Float.px: Float
    get() = this / Resources.getSystem().displayMetrics.density

val Int.px: Int
    get() = (this / Resources.getSystem().displayMetrics.density).roundToInt()