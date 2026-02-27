package io.core.utils.extensions.cool

import android.graphics.Color
import io.core.utils.tools.TimeTools
import io.core.utils.constant.TimePatterns
import kotlin.jvm.Throws
import kotlin.math.abs

/**
 * 安全获取数值，当为null时返回默认�?
 * @param default 默认�?默认为对应类型的零�?
 * @return 原始数值或默认�?当为null�?
 */
inline fun <reified T : Number> T?.orThis(default: T = when (T::class) {
    Long::class -> 0L
    Int::class -> 0
    Float::class -> 0f
    Double::class -> 0.0
    else -> throw IllegalArgumentException("不支持的数字类型: ${T::class.simpleName}")
} as T): T = this ?: default

fun Long.timeFormat(pattern: String = TimePatterns.TIME_FULL): String {
    return TimeTools.getDateFormat(pattern).format(this)
}

fun Long.toTimeAgo(): String {
    val curTime = System.currentTimeMillis()
    val time = this
    val seconds = abs(System.currentTimeMillis() - time) / 1000f
    val end = if (time < curTime) "�? else "�?

    val start = when {
        seconds < 60 -> "${seconds.toInt()}�?
        seconds < 3600 -> {
            val minutes = seconds / 60f
            "${minutes.toInt()}分钟"
        }

        seconds < 86400 -> {
            val hours = seconds / 3600f
            "${hours.toInt()}小时"
        }

        seconds < 604800 -> {
            val days = seconds / 86400f
            "${days.toInt()}�?
        }

        seconds < 2_628_000 -> {
            val weeks = seconds / 604800f
            "${weeks.toInt()}�?
        }

        seconds < 31_536_000 -> {
            val months = seconds / 2_628_000f
            "${months.toInt()}�?
        }

        else -> {
            val years = seconds / 31_536_000f
            "${years.toInt()}�?
        }
    }
    return start + end
}

val Long.currentTimeFormatted: String
    get() = this.timeFormat()

/**
 * 将数值格式化为保留指定小数位数的字符�?
 * @param decimalPlaces 保留小数位数（默认为1位）
 * @return 格式化后的字符串，当decimals=1时返�?12.3"格式
 * @throws IllegalArgumentException 当decimals为负数时抛出
 *
 * 示例�?
 * 123.456.formatToDecimal() -> "123.5"
 * 100.formatToDecimal(2) -> "100.00"
 */
fun Number.formatToFixedDecimal(decimalPlaces: Int = 1): String {
    require(decimalPlaces >= 0) { "小数位数不能为负�? }
    val formatString = "%.${decimalPlaces}f"
    return formatString.format(this.toDouble())
}

// 扩展函数：判断颜色是否为深色
fun Int.isDarkColor(): Boolean {
    val darkness = 1 - (0.299 * Color.red(this) +
            0.587 * Color.green(this) +
            0.114 * Color.blue(this)) / 255
    return darkness >= 0.25
}