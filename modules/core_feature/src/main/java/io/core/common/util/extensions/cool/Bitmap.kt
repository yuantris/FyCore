package io.core.common.util.extensions.cool

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.get
import kotlin.math.roundToInt

fun Bitmap.isEmpty(): Boolean {
    return this.width == 0 || this.height == 0
}

/**
 * 获取指定宽高的图片
 */
fun Bitmap.resizeAndRecycle(newWidth: Int, newHeight: Int): Bitmap {
    //获取新的bitmap
    val ret = Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
    this.recycle()
    return ret
}

/**
 * 取平均色
 */
fun Bitmap.getMeanColor(): Int {
    val width: Int = this.width
    val height: Int = this.height
    var pixel: Int
    var pixelSumRed = 0
    var pixelSumBlue = 0
    var pixelSumGreen = 0
    for (i in 0..99) {
        for (j in 70..99) {
            pixel =
                this[(i * width / 100.toFloat()).roundToInt(), (j * height / 100.toFloat()).roundToInt()]
            pixelSumRed += Color.red(pixel)
            pixelSumGreen += Color.green(pixel)
            pixelSumBlue += Color.blue(pixel)
        }
    }
    val averagePixelRed = pixelSumRed / 3000
    val averagePixelBlue = pixelSumBlue / 3000
    val averagePixelGreen = pixelSumGreen / 3000
    return Color.rgb(
        averagePixelRed + 3,
        averagePixelGreen + 3,
        averagePixelBlue + 3
    )

}

fun Bitmap.safeRecycle() {
    if (!this.isRecycled) {
        this.recycle()
    }
}