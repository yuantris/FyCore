package com.core.calendar

import android.content.Context
import com.core.calendar.impl.DefaultCalendarOperator
import com.core.calendar.impl.DefaultReminderProcessor
import com.core.calendar.interfaces.*
import kotlinx.coroutines.*

/**
 * Android日历管理器主类
 * 支持插件化扩展和Java兼容调用
 */
class AndroidCalendarManager private constructor(
    private val context: Context,
    private val calendarOperator: CalendarOperator,
    private val reminderProcessor: ReminderProcessor
) {
    // 扩展点：事件处理器链
    private val eventProcessors = mutableListOf<EventProcessor>()
    
    // 扩展点：同步策略
    private val syncStrategies = mutableMapOf<String, SyncStrategy>()
    
    // 扩展点：自定义验证器
    private val validators = mutableListOf<EventValidator>()
    
    // 扩展点：事件监听器
    private val eventListeners = mutableListOf<CalendarEventListener>()
    
    companion object {
        @Volatile
        private var INSTANCE: AndroidCalendarManager? = null
        
        @JvmStatic
        fun getInstance(context: Context): AndroidCalendarManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Builder(context).build().also { INSTANCE = it }
            }
        }
        
        @JvmStatic
        fun builder(context: Context) = Builder(context)
    }
    
    // ========== 核心功能 - Kotlin协程版本 ==========
    
    /**
     * 创建事件（支持扩展处理）
     */
    suspend fun createEvent(event: CalendarEvent): Result<Long> {
        return try {
            // 1. 预处理链
            var processedEvent = event
            eventProcessors.sortedBy { it.getPriority() }.forEach { processor ->
                if (processor.canHandle(processedEvent)) {
                    processedEvent = processor.preProcess(processedEvent)
                }
            }
            
            // 2. 验证
            validators.forEach { validator ->
                validator.validate(processedEvent).getOrThrow()
            }
            
            // 3. 创建事件
            val result = calendarOperator.createEvent(processedEvent)
            
            // 4. 后处理和通知
            result.onSuccess { eventId ->
                eventProcessors.forEach { processor ->
                    if (processor.canHandle(processedEvent)) {
                        processor.postProcess(eventId, processedEvent)
                    }
                }
                
                eventListeners.forEach { listener ->
                    listener.onEventCreated(eventId, processedEvent)
                }
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 更新事件
     */
    suspend fun updateEvent(eventId: Long, event: CalendarEvent): Result<Unit> {
        return try {
            var processedEvent = event
            eventProcessors.sortedBy { it.getPriority() }.forEach { processor ->
                if (processor.canHandle(processedEvent)) {
                    processedEvent = processor.preProcess(processedEvent)
                }
            }
            
            validators.forEach { validator ->
                validator.validate(processedEvent).getOrThrow()
            }
            
            val result = calendarOperator.updateEvent(eventId, processedEvent)
            
            result.onSuccess {
                eventProcessors.forEach { processor ->
                    if (processor.canHandle(processedEvent)) {
                        processor.postProcess(eventId, processedEvent)
                    }
                }
                
                eventListeners.forEach { listener ->
                    listener.onEventUpdated(eventId, processedEvent)
                }
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 删除事件
     */
    suspend fun deleteEvent(eventId: Long): Result<Unit> {
        return calendarOperator.deleteEvent(eventId).also { result ->
            result.onSuccess {
                eventListeners.forEach { listener ->
                    listener.onEventDeleted(eventId)
                }
            }
        }
    }
    
    /**
     * 查询事件
     */
    suspend fun queryEvents(query: EventQuery): Result<List<CalendarEvent>> {
        return calendarOperator.queryEvents(query).also { result ->
            result.onSuccess { events ->
                eventListeners.forEach { listener ->
                    listener.onEventQueried(events)
                }
            }
        }
    }
    
    /**
     * 根据ID获取事件
     */
    suspend fun getEventById(eventId: Long): Result<CalendarEvent?> {
        return calendarOperator.getEventById(eventId)
    }
    
    // ========== Java兼容版本 - 使用回调 ==========
    
    @JvmOverloads
    fun createEventAsync(
        event: CalendarEvent, 
        callback: CalendarCallback<Long>? = null
    ): Job = CoroutineScope(Dispatchers.Main).launch {
        val result = createEvent(event)
        callback?.onResult(result)
    }
    
    @JvmOverloads
    fun updateEventAsync(
        eventId: Long, 
        event: CalendarEvent, 
        callback: CalendarCallback<Unit>? = null
    ): Job = CoroutineScope(Dispatchers.Main).launch {
        val result = updateEvent(eventId, event)
        callback?.onResult(result)
    }
    
    @JvmOverloads
    fun deleteEventAsync(
        eventId: Long, 
        callback: CalendarCallback<Unit>? = null
    ): Job = CoroutineScope(Dispatchers.Main).launch {
        val result = deleteEvent(eventId)
        callback?.onResult(result)
    }
    
    @JvmOverloads
    fun queryEventsAsync(
        query: EventQuery, 
        callback: CalendarCallback<List<CalendarEvent>>? = null
    ): Job = CoroutineScope(Dispatchers.Main).launch {
        val result = queryEvents(query)
        callback?.onResult(result)
    }
    
    @JvmOverloads
    fun getEventByIdAsync(
        eventId: Long,
        callback: CalendarCallback<CalendarEvent?>? = null
    ): Job = CoroutineScope(Dispatchers.Main).launch {
        val result = getEventById(eventId)
        callback?.onResult(result)
    }
    
    // ========== 扩展管理方法 ==========
    
    fun registerEventProcessor(processor: EventProcessor): AndroidCalendarManager = apply {
        eventProcessors.add(processor)
        eventProcessors.sortBy { it.getPriority() }
    }
    
    fun registerSyncStrategy(name: String, strategy: SyncStrategy): AndroidCalendarManager = apply {
        syncStrategies[name] = strategy
    }
    
    fun registerValidator(validator: EventValidator): AndroidCalendarManager = apply {
        validators.add(validator)
    }
    
    fun registerEventListener(listener: CalendarEventListener): AndroidCalendarManager = apply {
        eventListeners.add(listener)
    }
    
    fun unregisterEventProcessor(processor: EventProcessor): AndroidCalendarManager = apply {
        eventProcessors.remove(processor)
    }
    
    fun unregisterSyncStrategy(name: String): AndroidCalendarManager = apply {
        syncStrategies.remove(name)
    }
    
    fun unregisterValidator(validator: EventValidator): AndroidCalendarManager = apply {
        validators.remove(validator)
    }
    
    fun unregisterEventListener(listener: CalendarEventListener): AndroidCalendarManager = apply {
        eventListeners.remove(listener)
    }
    
    // ========== 同步功能 ==========
    
    /**
     * 与指定提供商同步
     */
    suspend fun syncWith(providerName: String): Result<Unit> {
        return syncStrategies[providerName]?.let { strategy ->
            if (strategy.isAvailable()) {
                strategy.syncFromExternal()
                    .mapCatching { events ->
                        events.forEach { event ->
                            createEvent(event)
                        }
                    }
            } else {
                Result.failure(IllegalStateException("Sync strategy $providerName is not available"))
            }
        } ?: Result.failure(IllegalArgumentException("Unknown sync strategy: $providerName"))
    }
    
    @JvmOverloads
    fun syncWithAsync(
        providerName: String,
        callback: CalendarCallback<Unit>? = null
    ): Job = CoroutineScope(Dispatchers.Main).launch {
        val result = syncWith(providerName)
        callback?.onResult(result)
    }
    
    /**
     * 获取可用的同步策略
     */
    fun getAvailableSyncStrategies(): List<String> {
        return syncStrategies.filter { it.value.isAvailable() }.keys.toList()
    }
    
    // ========== Builder模式 ==========
    
    class Builder(private val context: Context) {
        private var calendarOperator: CalendarOperator? = null
        private var reminderProcessor: ReminderProcessor? = null
        private val eventProcessors = mutableListOf<EventProcessor>()
        private val syncStrategies = mutableMapOf<String, SyncStrategy>()
        private val validators = mutableListOf<EventValidator>()
        private val eventListeners = mutableListOf<CalendarEventListener>()
        
        fun setCalendarOperator(operator: CalendarOperator) = apply { 
            this.calendarOperator = operator 
        }
        
        fun setReminderProcessor(processor: ReminderProcessor) = apply { 
            this.reminderProcessor = processor 
        }
        
        fun addEventProcessor(processor: EventProcessor) = apply { 
            this.eventProcessors.add(processor) 
        }
        
        fun addSyncStrategy(name: String, strategy: SyncStrategy) = apply { 
            this.syncStrategies[name] = strategy 
        }
        
        fun addValidator(validator: EventValidator) = apply { 
            this.validators.add(validator) 
        }
        
        fun addEventListener(listener: CalendarEventListener) = apply { 
            this.eventListeners.add(listener) 
        }
        
        fun build(): AndroidCalendarManager {
            val manager = AndroidCalendarManager(
                context = context,
                calendarOperator = calendarOperator ?: DefaultCalendarOperator(context),
                reminderProcessor = reminderProcessor ?: DefaultReminderProcessor(context)
            )
            
            eventProcessors.forEach { manager.registerEventProcessor(it) }
            syncStrategies.forEach { (name, strategy) -> manager.registerSyncStrategy(name, strategy) }
            validators.forEach { manager.registerValidator(it) }
            eventListeners.forEach { manager.registerEventListener(it) }
            
            return manager
        }
    }
}