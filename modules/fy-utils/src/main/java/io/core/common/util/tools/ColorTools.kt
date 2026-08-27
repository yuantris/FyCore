package io.core.common.util.tools

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.FloatRange
import androidx.core.graphics.ColorUtils
import io.core.appCtx
import io.core.common.util.extensions.ui.getCompatColor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

object ColorTools {

    @JvmStatic
    fun getColor(@ColorRes color: Int): Int {
        return appCtx.getCompatColor(color)
    }

    @JvmStatic
    fun isColorLight(@ColorInt color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) >= 0.5
    }

    @JvmStatic
    fun intToString(intColor: Int): String {
        return String.format("#%06X", 0xFFFFFF and intColor)
    }

    /**
     * 移除颜色值中的Alpha通道
     *
     * 此函数的目的是将给定颜色值中的Alpha通道设置为不透明（即FF），而不改变其他颜色信息
     * 通过执行按位或操作，将Alpha通道的值设置为-0x1000000（在二进制表示中，这意味着最高位被设置为1，其余位为0），
     * 从而确保Alpha通道被设置为全不透明，而其他RGB值保持不变
     *
     * @param color 一个包含Alpha通道的整数颜色值
     * @return 移除Alpha通道后的颜色值，Alpha通道被设置为不透明
     */
    @JvmStatic
    fun stripAlpha(@ColorInt color: Int): Int {
        return -0x1000000 or color
    }

    /**
     * 调整给定颜色的亮度
     * 此函数通过改变颜色的HSV值中的价值组件（V）来实现颜色的明暗调整
     * 主要用于用户界面中动态调整颜色的亮度，以适应不同的视觉需求
     *
     * @param color 需要调整的颜色，必须是一个有效的ColorInt值
     * @param by 亮度调整因子，介于0.0到2.0之间，用于控制颜色变亮或变暗的程度
     *           当by=1f时，颜色不变；大于1f时，颜色变亮；小于1f时，颜色变暗
     * @return 调整亮度后的颜色值，返回一个ColorInt值
     */
    @ColorInt
    @JvmStatic
    fun shiftColor(@ColorInt color: Int, @FloatRange(from = 0.0, to = 2.0) by: Float): Int {
        if (by == 1f) return color
        val alpha = Color.alpha(color)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= by // value component
        return (alpha shl 24) + (0x00ffffff and Color.HSVToColor(hsv))
    }

    /**
     * 暗化给定颜色
     *
     * 此方法接收一个颜色值，并返回一个暗化后的颜色值暗化是通过调用`shiftColor`函数，
     * 并将暗化因子设置为0.9来实现的这意味着返回的颜色将会比原始颜色暗10%
     *
     * @param color 要暗化的颜色，必须是一个有效的颜色值
     * @return 暗化后的颜色值
     */
    @ColorInt
    @JvmStatic
    fun darkenColor(@ColorInt color: Int): Int {
        return shiftColor(color, 0.9f)
    }

    /**
     * 使给定的颜色变亮。
     *
     * 该函数通过调整给定颜色的色调来使其看起来更亮。它主要用于用户界面的视觉效果优化。
     *
     * @param color 一个代表ARGB颜色值的整数。
     * @return 一个代表变亮后颜色的整数。
     */
    @ColorInt
    @JvmStatic
    fun lightenColor(@ColorInt color: Int): Int {
        return shiftColor(color, 1.1f)
    }

    /**
     * 反转给定颜色值
     *
     * 此函数接受一个颜色值作为输入，并返回其反转颜色值
     * 颜色值的每个RGB分量将被反转，即从255中减去其值，以实现颜色反转效果
     * Alpha通道值保持不变
     *
     * @param color Int 类型的颜色值，使用@ColorInt注解标记
     * @return Int 返回反转后的颜色值，同样使用@ColorInt注解标记
     */
    @ColorInt
    @JvmStatic
    fun invertColor(@ColorInt color: Int): Int {
        val r = 255 - Color.red(color)
        val g = 255 - Color.green(color)
        val b = 255 - Color.blue(color)
        return Color.argb(Color.alpha(color), r, g, b)
    }

    /**
     * 调整给定颜色的透明度
     *
     * 此函数接受一个颜色值和一个透明度因子，返回一个具有调整后透明度的新颜色值
     * 它用于在不改变原色的前提下，调整颜色的透明效果
     *
     * @param color Int 类型的颜色值，包含 alpha 通道信息
     * @param factor Float 类型的透明度因子，范围从 0.0（完全透明）到 1.0（完全不透明）
     * @return Int 返回调整透明度后的颜色值
     */
    @ColorInt
    @JvmStatic
    fun adjustAlpha(@ColorInt color: Int, @FloatRange(from = 0.0, to = 1.0) factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).roundToInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    /**
     * 给指定的颜色添加alpha透明度通道
     *
     * 该函数接受一个基础颜色值和一个alpha透明度值作为输入，返回一个新的颜色值，其alpha通道根据给定的alpha值设置
     * 颜色值和alpha值都必须在特定的范围内，以确保颜色的正确处理
     *
     * @param baseColor 一个代表基础颜色的整数，使用ColorInt注解标记，表示它应该是一个有效的颜色值
     * @param alpha 一个浮点数，表示透明度，范围从0.0（完全透明）到1.0（完全不透明），使用FloatRange注解标记
     * @return 返回一个新的颜色整数，其中alpha通道根据指定的透明度值设置
     */
    @ColorInt
    @JvmStatic
    fun withAlpha(@ColorInt baseColor: Int, @FloatRange(from = 0.0, to = 1.0) alpha: Float): Int {
        val a = min(255, max(0, (alpha * 255).toInt())) shl 24
        val rgb = 0x00ffffff and baseColor
        return a + rgb
    }

    @ColorInt
    @JvmStatic
    fun getRandomColor(): Int {
        val red = Random.nextInt(256)
        val green = Random.nextInt(256)
        val blue = Random.nextInt(256)
        return Color.rgb(red, green, blue)
    }

    @ColorInt
    @JvmStatic
    fun getRandomColorWithAlpha(): Int {
        val alpha = Random.nextInt(256)
        val red = Random.nextInt(256)
        val green = Random.nextInt(256)
        val blue = Random.nextInt(256)
        return Color.argb(alpha, red, green, blue)
    }

    /**
     * Taken from CollapsingToolbarLayout's CollapsingTextHelper class.
     */
    @JvmStatic
    fun blendColors(color1: Int, color2: Int, @FloatRange(from = 0.0, to = 1.0) ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val a = Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio
        val r = Color.red(color1) * inverseRatio + Color.red(color2) * ratio
        val g = Color.green(color1) * inverseRatio + Color.green(color2) * ratio
        val b = Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio
        return Color.argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
    }

    @JvmStatic
    fun argb(r: Int, g: Int, b: Int): Int {
        return argb(Byte.MAX_VALUE.toInt(), r, g, b)
    }

    @JvmStatic
    fun argb(alpha: Int, r: Int, g: Int, b: Int): Int {
        val colorByteArr =
            byteArrayOf(alpha.toByte(), r.toByte(), g.toByte(), b.toByte())
        return byteArrToInt(colorByteArr)
    }

    @JvmStatic
    fun rgb(argb: Int): IntArray {
        return intArrayOf(argb shr 16 and 0xFF, argb shr 8 and 0xFF, argb and 0xFF)
    }

    @JvmStatic
    fun byteArrToInt(colorByteArr: ByteArray): Int {
        return ((colorByteArr[0].toInt() shl 24) + (colorByteArr[1].toInt() and 0xFF shl 16)
                + (colorByteArr[2].toInt() and 0xFF shl 8) + (colorByteArr[3].toInt() and 0xFF))
    }

    /**
     * Computes the difference between two RGB colors by converting them to the L*a*b scale and
     * comparing them using the CIE76 algorithm { http://en.wikipedia.org/wiki/Color_difference#CIE76}
     */
    @JvmStatic
    fun getColorDifference(a: Int, b: Int): Double {
        val lab1 = DoubleArray(3)
        val lab2 = DoubleArray(3)
        ColorUtils.colorToLAB(a, lab1)
        ColorUtils.colorToLAB(b, lab2)
        return sqrt(
            (lab2[0] - lab1[0])
                .pow(2.0) + (lab2[1] - lab1[1])
                .pow(2.0) + (lab2[2] - lab1[2])
                .pow(2.0)
        )
    }
}
