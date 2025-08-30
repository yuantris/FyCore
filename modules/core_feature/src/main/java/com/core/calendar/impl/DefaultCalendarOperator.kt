package com.core.calendar.impl

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.core.calendar.*
import com.core.calendar.interfaces.CalendarOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 默认的Android系统日历操作实现
 */
class DefaultCalendarOperator(
    private val context: Context
) : CalendarOperator {
    
    private val contentResolver: ContentResolver = context.contentResolver
    
    override suspend fun createEvent(event: CalendarEvent): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, event.calendarId)
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
                put(CalendarContract.Events.DTSTART, event.startTime)
                put(CalendarContract.Events.DTEND, event.endTime)
                put(CalendarContract.Events.ALL_DAY, if (event.allDay) 1 else 0)
                put(CalendarContract.Events.EVENT_TIMEZONE, event.timeZone)
                put(CalendarContract.Events.AVAILABILITY, event.availability.value)
                put(CalendarContract.Events.ACCESS_LEVEL, event.visibility.value)
                
                // 处理重复规则
                event.recurrence?.let { rule ->
                    put(CalendarContract.Events.RRULE, rule.toRRule())
                }
            }
            
            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLong() 
                ?: throw IllegalStateException("Failed to create event")
            
            // 添加提醒
            event.reminders.forEach { reminder ->
                insertReminder(eventId, reminder)
            }
            
            Result.success(eventId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateEvent(eventId: Long, event: CalendarEvent): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
                put(CalendarContract.Events.DTSTART, event.startTime)
                put(CalendarContract.Events.DTEND, event.endTime)
                put(CalendarContract.Events.ALL_DAY, if (event.allDay) 1 else 0)
                put(CalendarContract.Events.EVENT_TIMEZONE, event.timeZone)
                put(CalendarContract.Events.AVAILABILITY, event.availability.value)
                put(CalendarContract.Events.ACCESS_LEVEL, event.visibility.value)
                
                event.recurrence?.let { rule ->
                    put(CalendarContract.Events.RRULE, rule.toRRule())
                }
            }
            
            val uri = CalendarContract.Events.CONTENT_URI.buildUpon()
                .appendPath(eventId.toString())
                .build()
            
            val rowsUpdated = contentResolver.update(uri, values, null, null)
            if (rowsUpdated == 0) {
                throw IllegalStateException("Event not found or update failed")
            }
            
            // 更新提醒
            deleteReminders(eventId)
            event.reminders.forEach { reminder ->
                insertReminder(eventId, reminder)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteEvent(eventId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uri = CalendarContract.Events.CONTENT_URI.buildUpon()
                .appendPath(eventId.toString())
                .build()
            
            val rowsDeleted = contentResolver.delete(uri, null, null)
            if (rowsDeleted == 0) {
                throw IllegalStateException("Event not found or delete failed")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun queryEvents(query: EventQuery): Result<List<CalendarEvent>> = withContext(Dispatchers.IO) {
        try {
            val (selection, selectionArgs) = query.buildSelection()
            val sortOrder = query.getSortOrder()
            
            val cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                EVENT_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
            )
            
            val events = cursor?.use { c ->
                val eventsList = mutableListOf<CalendarEvent>()
                while (c.moveToNext()) {
                    eventsList.add(cursorToEvent(c))
                }
                eventsList
            } ?: emptyList()
            
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getEventById(eventId: Long): Result<CalendarEvent?> = withContext(Dispatchers.IO) {
        try {
            val cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                EVENT_PROJECTION,
                "${CalendarContract.Events._ID} = ?",
                arrayOf(eventId.toString()),
                null
            )
            
            val event = cursor?.use { c ->
                if (c.moveToFirst()) {
                    cursorToEvent(c)
                } else null
            }
            
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun insertReminder(eventId: Long, reminder: ReminderConfig) {
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, reminder.minutes)
            put(CalendarContract.Reminders.METHOD, reminder.method.value)
        }
        contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
    }
    
    private fun deleteReminders(eventId: Long) {
        contentResolver.delete(
            CalendarContract.Reminders.CONTENT_URI,
            "${CalendarContract.Reminders.EVENT_ID} = ?",
            arrayOf(eventId.toString())
        )
    }
    
    private fun cursorToEvent(cursor: Cursor): CalendarEvent {
        return CalendarEvent(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)),
            calendarId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)) ?: "",
            description = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)),
            location = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)),
            startTime = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)),
            endTime = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)),
            allDay = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)) == 1,
            timeZone = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_TIMEZONE)) ?: "UTC",
            availability = Availability.fromValue(cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.AVAILABILITY))),
            visibility = Visibility.fromValue(cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.ACCESS_LEVEL))),
            reminders = getRemindersForEvent(cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)))
        )
    }
    
    private fun getRemindersForEvent(eventId: Long): List<ReminderConfig> {
        val cursor = contentResolver.query(
            CalendarContract.Reminders.CONTENT_URI,
            arrayOf(
                CalendarContract.Reminders.MINUTES,
                CalendarContract.Reminders.METHOD
            ),
            "${CalendarContract.Reminders.EVENT_ID} = ?",
            arrayOf(eventId.toString()),
            null
        )
        
        return cursor?.use { c ->
            val reminders = mutableListOf<ReminderConfig>()
            while (c.moveToNext()) {
                val minutes = c.getInt(c.getColumnIndexOrThrow(CalendarContract.Reminders.MINUTES))
                val method = ReminderMethod.fromValue(c.getInt(c.getColumnIndexOrThrow(CalendarContract.Reminders.METHOD)))
                reminders.add(ReminderConfig(ReminderType.ONCE, method, minutes))
            }
            reminders
        } ?: emptyList()
    }
    
    companion object {
        private val EVENT_PROJECTION = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_TIMEZONE,
            CalendarContract.Events.AVAILABILITY,
            CalendarContract.Events.ACCESS_LEVEL,
            CalendarContract.Events.RRULE
        )
    }
}