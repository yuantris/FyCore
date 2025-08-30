package com.core.calendar.impl

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.core.calendar.ReminderConfig
import com.core.calendar.ReminderMethod
import com.core.calendar.interfaces.ReminderProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 默认的提醒处理器实现
 */
class DefaultReminderProcessor(
    private val context: Context
) : ReminderProcessor {
    
    override suspend fun scheduleReminder(eventId: Long, reminder: ReminderConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, reminder.minutes)
                put(CalendarContract.Reminders.METHOD, reminder.method.value)
            }
            
            val uri = context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
            if (uri == null) {
                throw IllegalStateException("Failed to schedule reminder")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun cancelReminder(eventId: Long, reminderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rowsDeleted = context.contentResolver.delete(
                CalendarContract.Reminders.CONTENT_URI,
                "${CalendarContract.Reminders._ID} = ? AND ${CalendarContract.Reminders.EVENT_ID} = ?",
                arrayOf(reminderId, eventId.toString())
            )
            
            if (rowsDeleted == 0) {
                throw IllegalStateException("Reminder not found or delete failed")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateReminder(eventId: Long, reminder: ReminderConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 删除旧提醒
            context.contentResolver.delete(
                CalendarContract.Reminders.CONTENT_URI,
                "${CalendarContract.Reminders.EVENT_ID} = ?",
                arrayOf(eventId.toString())
            )
            
            // 添加新提醒
            scheduleReminder(eventId, reminder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getSupportedMethods(): List<ReminderMethod> {
        return listOf(
            ReminderMethod.DEFAULT,
            ReminderMethod.ALERT,
            ReminderMethod.EMAIL,
            ReminderMethod.SMS
        )
    }
}