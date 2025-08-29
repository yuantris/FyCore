package io.core.common.util.extensions.tool

import io.core.common.util.tools.TimeTools
import io.core.constant.TimePatterns
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