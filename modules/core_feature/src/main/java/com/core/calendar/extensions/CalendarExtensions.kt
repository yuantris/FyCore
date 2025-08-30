package com.core.calendar.extensions

import com.core.calendar.*
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * 日历相关扩展函数
 */

// ========== CalendarEvent扩展 ==========

/**
 * 检查事件是否在指定时间范围内
 */
fun CalendarEvent.isInTimeRange(startTime: Long, endTime: Long): Boolean {
    return this.startTime < endTime && this.endTime > startTime
}

/**
 * 检查事件是否为全天事件
 */
fun CalendarEvent.isAllDayEvent(): Boolean = allDay

/**
 * 获取事件持续时间（分钟）
 */
fun CalendarEvent.getDurationInMinutes(): Long {
    return (endTime - startTime) / (1000 * 60)
}

/**
 * 检查事件是否有提醒
 */
fun CalendarEvent.hasReminders(): Boolean = reminders.isNotEmpty()

/**
 * 检查事件是否为重复事件
 */
fun CalendarEvent.isRecurring(): Boolean = recurrence != null

/**
 * 添加多个提醒
 */
fun CalendarEvent.withReminders(vararg reminders: ReminderConfig): CalendarEvent {
    return copy(reminders = this.reminders + reminders)
}

// ========== 时间转换扩展 ==========

/**
 * Long时间戳转LocalDateTime
 */
fun Long.toLocalDateTime(timeZone: String = TimeZone.getDefault().id): LocalDateTime {
    return LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(this),
        ZoneId.of(timeZone)
    )
}

/**
 * LocalDateTime转时间戳
 */
fun LocalDateTime.toTimestamp(timeZone: String = TimeZone.getDefault().id): Long {
    return atZone(ZoneId.of(timeZone)).toInstant().toEpochMilli()
}

// ========== EventQuery扩展 ==========

/**
 * 查询今天的事件
 */
fun EventQuery.Companion.today(): EventQuery {
    val now = System.currentTimeMillis()
    val startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay()
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
    
    return builder()
        .timeRange(startOfDay, endOfDay)
        .sortBy(SortOrder.START_TIME_ASC)
}

/**
 * 查询本周的事件
 */
fun EventQuery.Companion.thisWeek(): EventQuery {
    val now = LocalDateTime.now()
    val startOfWeek = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay()
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endOfWeek = startOfWeek + 7 * 24 * 60 * 60 * 1000 - 1
    
    return builder()
        .timeRange(startOfWeek, endOfWeek)
        .sortBy(SortOrder.START_TIME_ASC)
}

/**
 * 查询本月的事件
 */
fun EventQuery.Companion.thisMonth(): EventQuery {
    val now = LocalDateTime.now()
    val startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay()
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endOfMonth = now.toLocalDate().withDayOfMonth(now.toLocalDate().lengthOfMonth())
        .atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    
    return builder()
        .timeRange(startOfMonth, endOfMonth)
        .sortBy(SortOrder.START_TIME_ASC)
}

// ========== ReminderConfig扩展 ==========

/**
 * 创建自定义时间提醒
 */
fun ReminderConfig.Companion.beforeMinutes(minutes: Int): ReminderConfig {
    return ReminderConfig(ReminderType.ONCE, ReminderMethod.ALERT, minutes)
}

/**
 * 创建自定义小时提醒
 */
fun ReminderConfig.Companion.beforeHours(hours: Int): ReminderConfig {
    return ReminderConfig(ReminderType.ONCE, ReminderMethod.ALERT, hours * 60)
}

/**
 * 创建自定义天数提醒
 */
fun ReminderConfig.Companion.beforeDays(days: Int): ReminderConfig {
    return ReminderConfig(ReminderType.ONCE, ReminderMethod.ALERT, days * 24 * 60)
}

// ========== RecurrenceRule扩展 ==========

/**
 * 创建工作日重复规则
 */
fun RecurrenceRule.Companion.weekdays(): RecurrenceRule {
    return RecurrenceRule(
        frequency = RecurrenceFrequency.WEEKLY,
        daysOfWeek = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
    )
}

/**
 * 创建周末重复规则
 */
fun RecurrenceRule.Companion.weekends(): RecurrenceRule {
    return RecurrenceRule(
        frequency = RecurrenceFrequency.WEEKLY,
        daysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    )
}

/**
 * 创建每两周重复规则
 */
fun RecurrenceRule.Companion.biweekly(vararg daysOfWeek: DayOfWeek): RecurrenceRule {
    return RecurrenceRule(
        frequency = RecurrenceFrequency.WEEKLY,
        interval = 2,
        daysOfWeek = daysOfWeek.toSet()
    )
}

// ========== 集合扩展 ==========

/**
 * 过滤今天的事件
 */
fun List<CalendarEvent>.filterToday(): List<CalendarEvent> {
    val startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay()
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
    
    return filter { it.isInTimeRange(startOfDay, endOfDay) }
}

/**
 * 过滤有冲突的事件
 */
fun List<CalendarEvent>.findConflicts(targetEvent: CalendarEvent): List<CalendarEvent> {
    return filter { event ->
        event.id != targetEvent.id && 
        event.isInTimeRange(targetEvent.startTime, targetEvent.endTime)
    }
}

/**
 * 按开始时间排序
 */
fun List<CalendarEvent>.sortByStartTime(): List<CalendarEvent> {
    return sortedBy { it.startTime }
}

/**
 * 按标题排序
 */
fun List<CalendarEvent>.sortByTitle(): List<CalendarEvent> {
    return sortedBy { it.title }
}

/**
 * 分组按日期
 */
fun List<CalendarEvent>.groupByDate(): Map<String, List<CalendarEvent>> {
    return groupBy { event ->
        event.startDateTime.toLocalDate().toString()
    }
}