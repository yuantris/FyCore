package io.core.utils.extensions.tool

import io.core.utils.tools.TimeTools
import io.core.utils.constant.TimePatterns
import java.util.Date

fun String.convertDateString(
    targetPattern: String,
    sourcePattern: String = TimePatterns.DATE_YMD
): String {
    return TimeTools.convertDateString(this, sourcePattern, targetPattern)
}

fun Date.date2String(pattern: String = TimePatterns.TIME_FULL): String {
    return TimeTools.date2String(this, pattern)
}