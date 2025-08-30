package com.core.calendar

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 日程事件数据类
 * 充分利用Kotlin特性，同时兼容Java调用
 */
@Parcelize
data class CalendarEvent(
    val id: Long = -1L,
    val calendarId: Long,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTime: Long,
    val endTime: Long,
    val allDay: Boolean = false,
    val timeZone: String = TimeZone.getDefault().id,
    val reminders: List<ReminderConfig> = emptyList(),
    val recurrence: RecurrenceRule? = null,
    val availability: Availability = Availability.BUSY,
    val visibility: Visibility = Visibility.DEFAULT
) : Parcelable {
    
    // Kotlin扩展属性
    val duration: Duration
        get() = Duration.ofMillis(endTime - startTime)
    
    val startDateTime: LocalDateTime
        get() = Instant.ofEpochMilli(startTime)
            .atZone(ZoneId.of(timeZone))
            .toLocalDateTime()
    
    val endDateTime: LocalDateTime
        get() = Instant.ofEpochMilli(endTime)
            .atZone(ZoneId.of(timeZone))
            .toLocalDateTime()
    
    // Java兼容的getter方法
    @JvmName("getDurationMillis")
    fun getDurationInMillis(): Long = endTime - startTime
    
    @JvmName("getStartDateTimeForJava")
    fun getStartDateTimeAsString(): String = startDateTime.toString()
    
    @JvmName("getEndDateTimeForJava") 
    fun getEndDateTimeAsString(): String = endDateTime.toString()
    
    // 便捷方法
    fun withReminder(reminder: ReminderConfig): CalendarEvent = 
        copy(reminders = reminders + reminder)
    
    fun withRecurrence(rule: RecurrenceRule): CalendarEvent = 
        copy(recurrence = rule)
    
    /**
     * Builder模式支持Java链式调用
     */
    class Builder {
        private var calendarId: Long = 0
        private var title: String = ""
        private var description: String? = null
        private var location: String? = null
        private var startTime: Long = 0
        private var endTime: Long = 0
        private var allDay: Boolean = false
        private var timeZone: String = TimeZone.getDefault().id
        private var reminders: MutableList<ReminderConfig> = mutableListOf()
        private var recurrence: RecurrenceRule? = null
        private var availability: Availability = Availability.BUSY
        private var visibility: Visibility = Visibility.DEFAULT
        
        fun calendarId(calendarId: Long) = apply { this.calendarId = calendarId }
        fun title(title: String) = apply { this.title = title }
        fun description(description: String?) = apply { this.description = description }
        fun location(location: String?) = apply { this.location = location }
        fun startTime(startTime: Long) = apply { this.startTime = startTime }
        fun endTime(endTime: Long) = apply { this.endTime = endTime }
        fun allDay(allDay: Boolean) = apply { this.allDay = allDay }
        fun timeZone(timeZone: String) = apply { this.timeZone = timeZone }
        fun availability(availability: Availability) = apply { this.availability = availability }
        fun visibility(visibility: Visibility) = apply { this.visibility = visibility }
        
        fun addReminder(reminder: ReminderConfig) = apply { 
            this.reminders.add(reminder) 
        }
        
        fun addReminder(minutes: Int, method: ReminderMethod = ReminderMethod.ALERT) = apply {
            this.reminders.add(ReminderConfig(ReminderType.ONCE, method, minutes))
        }
        
        fun setRecurrence(rule: RecurrenceRule) = apply { 
            this.recurrence = rule 
        }
        
        // 快捷方法
        fun dailyReminder() = apply { 
            this.recurrence = RecurrenceRule.daily() 
        }
        
        fun weeklyReminder(vararg days: DayOfWeek) = apply {
            this.recurrence = RecurrenceRule.weekly(*days)
        }
        
        fun monthlyReminder(dayOfMonth: Int) = apply {
            this.recurrence = RecurrenceRule.monthly(dayOfMonth)
        }
        
        fun build() = CalendarEvent(
            calendarId = calendarId,
            title = title,
            description = description,
            location = location,
            startTime = startTime,
            endTime = endTime,
            allDay = allDay,
            timeZone = timeZone,
            reminders = reminders.toList(),
            recurrence = recurrence,
            availability = availability,
            visibility = visibility
        )
    }
    
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}

/**
 * 可用性枚举
 */
enum class Availability(@JvmField val value: Int) {
    BUSY(0),
    FREE(1),
    TENTATIVE(2);
    
    companion object {
        @JvmStatic
        fun fromValue(value: Int): Availability = values().find { it.value == value } ?: BUSY
    }
}

/**
 * 可见性枚举
 */
enum class Visibility(@JvmField val value: Int) {
    DEFAULT(0),
    CONFIDENTIAL(1),
    PRIVATE(2),
    PUBLIC(3);
    
    companion object {
        @JvmStatic
        fun fromValue(value: Int): Visibility = values().find { it.value == value } ?: DEFAULT
    }
}