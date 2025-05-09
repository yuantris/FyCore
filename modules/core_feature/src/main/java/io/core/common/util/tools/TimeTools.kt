package io.core.common.util.tools

import android.icu.util.Calendar
import io.core.common.util.extensions.cool.printOnDebug
import io.core.common.util.extensions.currentTimeMillis
import io.core.constant.TimePatterns
import java.text.ParseException
import java.text.SimpleDateFormat

object TimeTools {

    /**
     * 时间单位枚举
     */
    enum class Unit {
        SECOND, MINUTE, HOUR, DAY
    }

    @JvmStatic
    @JvmOverloads
    fun getDateFormat(format: String = TimePatterns.TIME_FULL): SimpleDateFormat {
        return TimePatterns.getFormatter(format)
    }

    @JvmStatic
    @JvmOverloads
    fun getNowString(pattern: String = TimePatterns.TIME_FULL): String {
        return getDateFormat(pattern).format(currentTimeMillis)
    }

    /**
     * 获取相对于当前日期的日期字符串
     * @param daysOffset 天数偏移量（正数表示往后推，负数表示往前推）
     * @param pattern 日期格式（默认yyyy-MM-dd）
     * @return 格式化后的日期字符串
     */
    @JvmStatic
    @JvmOverloads
    fun getDateString(daysOffset: Int, pattern: String = TimePatterns.DATE_YMD): String {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysOffset)
        }
        return getDateFormat(pattern).format(calendar.timeInMillis)
    }


    @JvmStatic
    fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    @JvmStatic
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
    @JvmStatic
    @JvmOverloads
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
    @JvmStatic
    @JvmOverloads
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
    @JvmStatic
    fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * 获取年龄(根据生日时间戳)
     */
    @JvmStatic
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
    @JvmStatic
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
    @JvmStatic
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
    @JvmStatic
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
    @JvmStatic
    fun secondsBetween(start: Long, end: Long): Int {
        return ((end - start) / 1000).toInt()
    }

    @JvmStatic
    fun isToday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    @JvmStatic
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
    @JvmStatic
    @JvmOverloads
    fun string2Millis(time: String, pattern: String = TimePatterns.DATE_YMD): Long {
        try {
            return getDateFormat(pattern).parse(time)?.time ?: -1
        } catch (e: ParseException) {
            e.printOnDebug()
        }
        return -1
    }

    /**
     * 将时间戳转换为时间字符串
     *
     * @param millis 时间戳
     * @param pattern 时间字符串格式(默认 yyyy-MM-dd HH:mm:ss)
     * @return 时间字符串
     */
    @JvmStatic
    @JvmOverloads
    fun millis2String(millis: Long, pattern: String = TimePatterns.TIME_FULL): String {
        return getDateFormat(pattern).format(millis)
    }

    /**
     * 获取友好时间显示(今天/昨天显示具体时间，其他显示日期)
     */
    @JvmStatic
    fun getFriendlyTime(timestamp: Long): String {
        return when {
            isToday(timestamp) -> "今天 ${getDateFormat("HH:mm").format(timestamp)}"
            isYesterday(timestamp) -> "昨天 ${getDateFormat("HH:mm").format(timestamp)}"
            else -> getDateFormat(TimePatterns.DATE_YMD).format(timestamp)
        }
    }

    /**
     * 毫秒时间单位转换
     * @param millis 需要转换的毫秒值
     * @param unit 时间单位（SECOND/MINUTE/HOUR/DAY）
     * @return 转换后的整数单位值
     */
    @JvmStatic
    @JvmOverloads
    fun convertMillis(millis: Long, unit: Unit = Unit.SECOND): Int {
        return when (unit) {
            Unit.SECOND -> (millis / 1000).toInt()
            Unit.MINUTE -> (millis / (1000 * 60)).toInt()
            Unit.HOUR -> (millis / (1000 * 60 * 60)).toInt()
            Unit.DAY -> (millis / (1000 * 60 * 60 * 24)).toInt()
        }
    }

}