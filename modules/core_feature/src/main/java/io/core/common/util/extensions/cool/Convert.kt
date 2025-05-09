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

@Suppress("DEPRECATION")
fun Float.pxToSp(): Float = this / Resources.getSystem().displayMetrics.scaledDensity + 0.5f

fun Int.dp(): Int = (this * Resources.getSystem().displayMetrics.density).roundToInt()
fun Int.px(): Int = (this / Resources.getSystem().displayMetrics.density).roundToInt()
fun Float.dp(): Float = this * Resources.getSystem().displayMetrics.density
fun Float.px(): Float = this / Resources.getSystem().displayMetrics.density