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
     * 获取指定时间戳所在周的第一天(周一)的起始时间
     */
    fun getStartOfWeek(timestamp: Long = currentTimeMillis): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * 获取指定时间戳所在月的第一天的起始时间
     */
    fun getStartOfMonth(timestamp: Long = currentTimeMillis): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * 判断两个时间戳是否在同一天
     */
    fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * 获取年龄(根据生日时间戳)
     */
    fun getAge(birthday: Long): Int {
        val now = Calendar.getInstance()
        val birth = Calendar.getInstance().apply { timeInMillis = birthday }
        var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
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

    /**
     * 将时间字符串解析为时间戳
     *
     * @param time 时间字符串
     * @param pattern 时间字符串格式(默认 yyyy-MM-dd)
     * @return 解析后的时间戳，解析失败返回-1
     */
    fun string2Millis(time: String, pattern: String = TimeFormat.DATE_DASHES): Long {
        try {
            return getDateFormat(pattern).parse(time)?.time ?: -1
        } catch (e: ParseException) {
            e.printOnDebug()
        }
        return -1
    }

    /**
     * 获取友好时间显示(今天/昨天显示具体时间，其他显示日期)
     */
    fun getFriendlyTime(timestamp: Long): String {
        return when {
            isToday(timestamp) -> "今天 ${getDateFormat("HH:mm").format(timestamp)}"
            isYesterday(timestamp) -> "昨天 ${getDateFormat("HH:mm").format(timestamp)}"
            else -> getDateFormat(TimeFormat.DATE_DASHES).format(timestamp)
        }
    }

}