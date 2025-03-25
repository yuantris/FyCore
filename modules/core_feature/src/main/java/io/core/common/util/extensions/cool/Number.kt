package io.core.common.util.extensions.cool

import android.graphics.Color
import io.core.common.util.tools.TimeTools
import io.core.constant.TimeFormat

fun Long.timeFormat(pattern: String = TimeFormat.TIME_FULL): String {
    return TimeTools.getDateFormat(pattern).format(this)
}

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