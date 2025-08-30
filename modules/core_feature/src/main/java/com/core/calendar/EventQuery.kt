package com.core.calendar

import android.content.ContentResolver
import android.database.Cursor
import android.provider.CalendarContract

/**
 * 事件查询构建器
 * 支持Kotlin DSL风格和Java链式调用
 */
class EventQuery private constructor() {
    private var calendarIds: List<Long>? = null
    private var startTime: Long? = null
    private var endTime: Long? = null
    private var searchText: String? = null
    private var limit: Int? = null
    private var sortOrder: SortOrder = SortOrder.START_TIME_ASC
    
    // Kotlin DSL风格
    fun calendarIds(vararg ids: Long) = apply { this.calendarIds = ids.toList() }
    fun timeRange(start: Long, end: Long) = apply { 
        this.startTime = start
        this.endTime = end 
    }
    fun searchText(text: String) = apply { this.searchText = text }
    fun limit(limit: Int) = apply { this.limit = limit }
    fun sortBy(order: SortOrder) = apply { this.sortOrder = order }
    
    // Java兼容方法
    @JvmName("setCalendarIds")
    fun setCalendarIdsForJava(ids: LongArray) = apply { 
        this.calendarIds = ids.toList() 
    }
    
    @JvmName("setTimeRange")
    fun setTimeRangeForJava(start: Long, end: Long) = apply {
        this.startTime = start
        this.endTime = end
    }
    
    /**
     * 构建查询条件
     */
    internal fun buildSelection(): Pair<String?, Array<String>?> {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()
        
        calendarIds?.let { ids ->
            conditions.add("${CalendarContract.Events.CALENDAR_ID} IN (${ids.joinToString(",") { "?" }})")
            args.addAll(ids.map { it.toString() })
        }
        
        startTime?.let { start ->
            conditions.add("${CalendarContract.Events.DTSTART} >= ?")
            args.add(start.toString())
        }
        
        endTime?.let { end ->
            conditions.add("${CalendarContract.Events.DTEND} <= ?")
            args.add(end.toString())
        }
        
        searchText?.let { text ->
            conditions.add("(${CalendarContract.Events.TITLE} LIKE ? OR ${CalendarContract.Events.DESCRIPTION} LIKE ?)")
            args.add("%$text%")
            args.add("%$text%")
        }
        
        val selection = if (conditions.isNotEmpty()) conditions.joinToString(" AND ") else null
        val selectionArgs = if (args.isNotEmpty()) args.toTypedArray() else null
        
        return Pair(selection, selectionArgs)
    }
    
    /**
     * 获取排序字符串
     */
    internal fun getSortOrder(): String {
        return sortOrder.sqlOrder + limit?.let { " LIMIT $it" }.orEmpty()
    }
    
    companion object {
        @JvmStatic
        fun builder() = EventQuery()
    }
}

/**
 * 排序方式枚举
 */
enum class SortOrder(val sqlOrder: String) {
    START_TIME_ASC("${CalendarContract.Events.DTSTART} ASC"),
    START_TIME_DESC("${CalendarContract.Events.DTSTART} DESC"),
    TITLE_ASC("${CalendarContract.Events.TITLE} ASC"),
    TITLE_DESC("${CalendarContract.Events.TITLE} DESC"),
    CREATED_ASC("${CalendarContract.Events.DTSTART} ASC"),
    CREATED_DESC("${CalendarContract.Events.DTSTART} DESC")
}