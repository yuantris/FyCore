package io.core.common.util.ext.cool

import io.core.common.util.tools.TimeUtils.getDateFormat
import io.core.constant.DateFormat

fun Long.currentTimeFormat(pattern: String = DateFormat.yyyyMMddHHmmss): String {
    return getDateFormat(pattern).format(this)
}