package com.core.calendar.interfaces

import com.core.calendar.CalendarEvent
import com.core.calendar.EventQuery

/**
 * 核心日历操作接口
 * 定义基础的CRUD操作
 */
interface CalendarOperator {
    suspend fun createEvent(event: CalendarEvent): Result<Long>
    suspend fun updateEvent(eventId: Long, event: CalendarEvent): Result<Unit>
    suspend fun deleteEvent(eventId: Long): Result<Unit>
    suspend fun queryEvents(query: EventQuery): Result<List<CalendarEvent>>
    suspend fun getEventById(eventId: Long): Result<CalendarEvent?>
}

/**
 * 提醒处理接口
 */
interface ReminderProcessor {
    suspend fun scheduleReminder(eventId: Long, reminder: com.core.calendar.ReminderConfig): Result<Unit>
    suspend fun cancelReminder(eventId: Long, reminderId: String): Result<Unit>
    suspend fun updateReminder(eventId: Long, reminder: com.core.calendar.ReminderConfig): Result<Unit>
    fun getSupportedMethods(): List<com.core.calendar.ReminderMethod>
}

/**
 * 同步策略接口
 */
interface SyncStrategy {
    suspend fun syncToExternal(events: List<CalendarEvent>): Result<Unit>
    suspend fun syncFromExternal(): Result<List<CalendarEvent>>
    fun isAvailable(): Boolean
    fun getProviderName(): String
}

/**
 * 事件处理器接口
 */
interface EventProcessor {
    fun canHandle(event: CalendarEvent): Boolean
    suspend fun preProcess(event: CalendarEvent): CalendarEvent
    suspend fun postProcess(eventId: Long, event: CalendarEvent): Unit
    fun getPriority(): Int
}

/**
 * 事件验证器接口
 */
interface EventValidator {
    suspend fun validate(event: CalendarEvent): Result<Unit>
    fun getValidatorName(): String
}

/**
 * 事件监听器接口
 */
interface CalendarEventListener {
    suspend fun onEventCreated(eventId: Long, event: CalendarEvent)
    suspend fun onEventUpdated(eventId: Long, event: CalendarEvent)
    suspend fun onEventDeleted(eventId: Long)
    suspend fun onEventQueried(events: List<CalendarEvent>)
}

/**
 * 数据转换器接口
 */
interface EventDataConverter<T> {
    fun canConvert(data: T): Boolean
    fun convert(data: T): CalendarEvent
    fun convertBack(event: CalendarEvent): T
}

/**
 * 权限管理器接口
 */
interface PermissionManager {
    suspend fun requestPermissions(): Result<Unit>
    fun hasRequiredPermissions(): Boolean
    fun getRequiredPermissions(): Array<String>
}