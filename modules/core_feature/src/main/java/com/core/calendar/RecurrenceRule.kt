package com.core.calendar

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 重复规则数据类
 */
@Parcelize
data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,              // 间隔（如每2周）
    val daysOfWeek: Set<DayOfWeek>? = null,  // 星期几（周重复时使用）
    val dayOfMonth: Int? = null,        // 每月第几天
    val monthOfYear: Int? = null,       // 每年第几月
    val endDate: LocalDate? = null,     // 结束日期
    val count: Int? = null              // 重复次数
) : Parcelable {
    
    /**
     * 生成RRULE字符串（RFC 5545标准）
     */
    fun toRRule(): String = buildString {
        append("FREQ=${frequency.name}")
        if (interval > 1) append(";INTERVAL=$interval")
        
        daysOfWeek?.let { days ->
            val daysList = days.joinToString(",") { 
                when(it) {
                    DayOfWeek.SUNDAY -> "SU"
                    DayOfWeek.MONDAY -> "MO"
                    DayOfWeek.TUESDAY -> "TU"
                    DayOfWeek.WEDNESDAY -> "WE"
                    DayOfWeek.THURSDAY -> "TH"
                    DayOfWeek.FRIDAY -> "FR"
                    DayOfWeek.SATURDAY -> "SA"
                }
            }
            append(";BYDAY=$daysList")
        }
        
        dayOfMonth?.let { append(";BYMONTHDAY=$it") }
        monthOfYear?.let { append(";BYMONTH=$it") }
        endDate?.let { append(";UNTIL=${it.format(DateTimeFormatter.BASIC_ISO_DATE)}") }
        count?.let { append(";COUNT=$it") }
    }
    
    companion object {
        // 常用重复规则预设
        @JvmStatic
        fun daily() = RecurrenceRule(RecurrenceFrequency.DAILY)
        
        @JvmStatic
        fun weekly(vararg daysOfWeek: DayOfWeek) = RecurrenceRule(
            frequency = RecurrenceFrequency.WEEKLY,
            daysOfWeek = daysOfWeek.toSet()
        )
        
        @JvmStatic
        fun monthly(dayOfMonth: Int) = RecurrenceRule(
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = dayOfMonth
        )
        
        @JvmStatic
        fun yearly(monthOfYear: Int, dayOfMonth: Int) = RecurrenceRule(
            frequency = RecurrenceFrequency.YEARLY,
            monthOfYear = monthOfYear,
            dayOfMonth = dayOfMonth
        )
        
        // 每月固定时间提醒
        @JvmStatic
        fun monthlyOnDay(day: Int) = RecurrenceRule(
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = day
        )
        
        // 每月最后一天
        @JvmStatic
        fun monthlyLastDay() = RecurrenceRule(
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = -1
        )
    }
}

/**
 * 重复频率枚举
 */
enum class RecurrenceFrequency {
    DAILY,
    WEEKLY, 
    MONTHLY,
    YEARLY
}