package io.core.common.util.extensions.cool

import android.graphics.Color
import io.core.common.util.tools.TimeUtils.getDateFormat
import io.core.constant.TimeFormat

fun Long.timeFormat(pattern: String = TimeFormat.TIME_FULL): String {
    return getDateFormat(pattern).format(this)
}

// 扩展函数：判断颜色是否为深色
fun Int.isDarkColor(): Boolean {
    val darkness = 1 - (0.299 * Color.red(this) +
            0.587 * Color.green(this) +
            0.114 * Color.blue(this)) / 255
    return darkness >= 0.25
}