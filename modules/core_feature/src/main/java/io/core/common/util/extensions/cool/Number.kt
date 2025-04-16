package io.core.common.util.extensions.cool

import android.graphics.Color
import io.core.common.util.tools.TimeTools
import io.core.constant.TimeFormat
import kotlin.math.abs

fun Long.timeFormat(pattern: String = TimeFormat.TIME_FULL): String {
    return TimeTools.getDateFormat(pattern).format(this)
}

fun Long.toTimeAgo(): String {
    val curTime = System.currentTimeMillis()
    val time = this
    val seconds = abs(System.currentTimeMillis() - time) / 1000f
    val end = if (time < curTime) "前" else "后"

    val start = when {
        seconds < 60 -> "${seconds.toInt()}秒"
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
            "${days.toInt()}天"
        }

        seconds < 2_628_000 -> {
            val weeks = seconds / 604800f
            "${weeks.toInt()}周"
        }

        seconds < 31_536_000 -> {
            val months = seconds / 2_628_000f
            "${months.toInt()}月"
        }

        else -> {
            val years = seconds / 31_536_000f
            "${years.toInt()}年"
        }
    }
    return start + end
}

val Long.currentTimeFormatted: String
    get() = this.timeFormat()

/**
 * 将数值格式化为保留指定小数位数的字符串
 * @param decimalPlaces 保留小数位数（默认为1位）
 * @return 格式化后的字符串，当decimals=1时返回"12.3"格式
 * @throws IllegalArgumentException 当decimals为负数时抛出
 *
 * 示例：
 * 123.456.formatToDecimal() -> "123.5"
 * 100.formatToDecimal(2) -> "100.00"
 */
fun Number.formatToFixedDecimal(decimalPlaces: Int = 1): String {
    require(decimalPlaces >= 0) { "小数位数不能为负数" }
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