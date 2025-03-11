package io.core.common.util.tools

import android.icu.util.Calendar
import io.core.common.util.extensions.cool.printOnDebug
import io.core.common.util.extensions.cool.timeFormat
import io.core.common.util.extensions.currentTimeMillis
import io.core.constant.TimeFormat
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import kotlin.math.abs

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

object TimeTools {

    fun getDateFormat(format: String = TimeFormat.TIME_FULL): SimpleDateFormat {
        return TimeFormat.getFormatter(format)
    }

    fun getNowString(pattern: String = TimeFormat.TIME_FULL): String {
        return getDateFormat(pattern).format(currentTimeMillis)
    }

    fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfDay(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    /**
     * 计算两个时间戳之间的天数差
     *
     * @param start 起始时间戳（单位：毫秒）
     * @param end 结束时间戳（单位：毫秒）
     * @return 两个时间戳之间的完整天数差值（向下取整后的整数值）
     */
    fun daysBetween(start: Long, end: Long): Int {
        return ((end - start) / (1000 * 60 * 60 * 24)).toInt()
    }

    /**
     * 计算两个时间戳之间的完整小时差（向下取整）
     *
     * @param start 起始时间戳（单位：毫秒）
     * @param end 结束时间戳（单位：毫秒）
     * @return 两个时间戳之间的小时差值（1小时=3600秒）
     */
    fun hoursBetween(start: Long, end: Long): Int {
        return ((end - start) / (1000 * 60 * 60)).toInt()
    }

    /**
     * 计算两个时间戳之间的完整分钟差（向下取整）
     *
     * @param start 起始时间戳（单位：毫秒）
     * @param end 结束时间戳（单位：毫秒）
     * @return 两个时间戳之间的分钟差值（1分钟=60秒）
     */
    fun minutesBetween(start: Long, end: Long): Int {
        return ((end - start) / (1000 * 60)).toInt()
    }

    /**
     * 计算两个时间戳之间的完整秒数差
     *
     * @param start 起始时间戳（单位：毫秒）
     * @param end 结束时间戳（单位：毫秒）
     * @return 两个时间戳之间的秒级差值（1秒=1000毫秒）
     */
    fun secondsBetween(start: Long, end: Long): Int {
        return ((end - start) / 1000).toInt()
    }

    fun isToday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }

    fun string2Millis(time: String, pattern: String = TimeFormat.DATE_DASHES): Long {
        try {
            return getDateFormat(pattern).parse(time)?.time ?: -1
        } catch (e: ParseException) {
            e.printOnDebug()
        }
        return -1
    }


}