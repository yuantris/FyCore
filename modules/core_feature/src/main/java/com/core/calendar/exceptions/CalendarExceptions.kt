package com.core.calendar.exceptions

/**
 * 日历相关异常类定义
 */
sealed class CalendarException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * 权限被拒绝异常
     */
    class PermissionDeniedException(message: String = "Calendar permission denied") : CalendarException(message)
    
    /**
     * 网络错误异常
     */
    class NetworkException(message: String = "Network error occurred", cause: Throwable? = null) : CalendarException(message, cause)
    
    /**
     * 验证错误异常
     */
    class ValidationException(val field: String, message: String = "Validation failed for field: $field") : CalendarException(message)
    
    /**
     * 数据库错误异常
     */
    class DatabaseException(message: String = "Database operation failed", cause: Throwable? = null) : CalendarException(message, cause)
    
    /**
     * 时间冲突异常
     */
    class ConflictException(message: String = "Time conflict detected") : CalendarException(message)
    
    /**
     * 事件未找到异常
     */
    class EventNotFoundException(eventId: Long) : CalendarException("Event with id $eventId not found")
    
    /**
     * 同步失败异常
     */
    class SyncException(provider: String, message: String = "Sync failed with provider: $provider", cause: Throwable? = null) : CalendarException(message, cause)
    
    /**
     * 配置错误异常
     */
    class ConfigurationException(message: String = "Configuration error") : CalendarException(message)
}