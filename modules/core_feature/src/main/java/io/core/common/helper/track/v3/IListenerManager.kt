package io.core.common.helper.track.v3

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 监听器管理器接口
 * 
 * @author [Yuantris] - v3.0
 * @since 2025/8/10
 */
interface IListenerManager<T> {
    fun register(tag: String, listener: T)
    fun unregister(tag: String): Boolean
    fun notifyAll(action: (T) -> Unit)
    fun notifyAllSafe(action: (T) -> Unit): Int
    fun size(): Int
    fun clear()
    fun getStats(): ListenerStats
    fun hasListener(tag: String): Boolean
    fun getListener(tag: String): T?
}

/**
 * 监听器统计信息
 */
data class ListenerStats(
    val totalListeners: Int,
    val activeListeners: Int,
    val totalNotifications: Long,
    val failedNotifications: Long,
    val avgNotificationTime: Long,
    val lastNotificationTime: Long
)

/**
 * 监听器包装器
 */
private data class ListenerWrapper<T>(
    val listener: T,
    val registrationTime: Long = System.currentTimeMillis(),
    val priority: Int = 0,
    var notificationCount: Long = 0,
    var lastNotificationTime: Long = 0,
    var failureCount: Long = 0
)

/**
 * 高性能监听器管理器
 */
class HighPerformanceListenerManager<T> : IListenerManager<T> {
    
    private val listeners = ConcurrentHashMap<String, ListenerWrapper<T>>()
    private val notificationStats = NotificationStats()
    
    private class NotificationStats {
        val totalNotifications = AtomicLong(0)
        val failedNotifications = AtomicLong(0)
        val totalNotificationTime = AtomicLong(0)
        val lastNotificationTime = AtomicLong(0)
        
        fun recordNotification(duration: Long, failed: Boolean = false) {
            totalNotifications.incrementAndGet()
            totalNotificationTime.addAndGet(duration)
            lastNotificationTime.set(System.currentTimeMillis())
            if (failed) {
                failedNotifications.incrementAndGet()
            }
        }
        
        fun getAvgNotificationTime(): Long {
            val total = totalNotifications.get()
            return if (total > 0) totalNotificationTime.get() / total else 0
        }
    }
    
    override fun register(tag: String, listener: T) {
        listeners[tag] = ListenerWrapper(listener)
    }
    
    override fun unregister(tag: String): Boolean {
        return listeners.remove(tag) != null
    }
    
    override fun notifyAll(action: (T) -> Unit) {
        val snapshot = listeners.values.toList()
        snapshot.forEach { wrapper ->
            try {
                val startTime = System.nanoTime()
                action(wrapper.listener)
                val duration = (System.nanoTime() - startTime) / 1_000_000
                
                wrapper.notificationCount++
                wrapper.lastNotificationTime = System.currentTimeMillis()
                notificationStats.recordNotification(duration)
            } catch (e: Exception) {
                wrapper.failureCount++
                notificationStats.recordNotification(0, failed = true)
                // 记录错误但不中断其他监听器
            }
        }
    }
    
    override fun notifyAllSafe(action: (T) -> Unit): Int {
        val snapshot = listeners.values.toList()
        var successCount = 0
        
        snapshot.forEach { wrapper ->
            try {
                val startTime = System.nanoTime()
                action(wrapper.listener)
                val duration = (System.nanoTime() - startTime) / 1_000_000
                
                wrapper.notificationCount++
                wrapper.lastNotificationTime = System.currentTimeMillis()
                notificationStats.recordNotification(duration)
                successCount++
            } catch (e: Exception) {
                wrapper.failureCount++
                notificationStats.recordNotification(0, failed = true)
            }
        }
        
        return successCount
    }
    
    override fun size(): Int = listeners.size
    
    override fun clear() {
        listeners.clear()
    }
    
    override fun getStats(): ListenerStats {
        val totalListeners = listeners.size
        val activeListeners = listeners.values.count { it.lastNotificationTime > 0 }
        
        return ListenerStats(
            totalListeners = totalListeners,
            activeListeners = activeListeners,
            totalNotifications = notificationStats.totalNotifications.get(),
            failedNotifications = notificationStats.failedNotifications.get(),
            avgNotificationTime = notificationStats.getAvgNotificationTime(),
            lastNotificationTime = notificationStats.lastNotificationTime.get()
        )
    }
    
    override fun hasListener(tag: String): Boolean = listeners.containsKey(tag)
    
    override fun getListener(tag: String): T? = listeners[tag]?.listener
}

/**
 * 优先级监听器管理器
 */
class PriorityListenerManager<T> : IListenerManager<T> {
    
    private val listeners = ConcurrentHashMap<String, ListenerWrapper<T>>()
    private val sortedListeners = CopyOnWriteArrayList<ListenerWrapper<T>>()
    private val lock = ReentrantReadWriteLock()
    private val notificationStats = NotificationStats()
    
    private class NotificationStats {
        val totalNotifications = AtomicLong(0)
        val failedNotifications = AtomicLong(0)
        val totalNotificationTime = AtomicLong(0)
        val lastNotificationTime = AtomicLong(0)
        
        fun recordNotification(duration: Long, failed: Boolean = false) {
            totalNotifications.incrementAndGet()
            totalNotificationTime.addAndGet(duration)
            lastNotificationTime.set(System.currentTimeMillis())
            if (failed) {
                failedNotifications.incrementAndGet()
            }
        }
        
        fun getAvgNotificationTime(): Long {
            val total = totalNotifications.get()
            return if (total > 0) totalNotificationTime.get() / total else 0
        }
    }
    
    fun register(tag: String, listener: T, priority: Int = 0) {
        val wrapper = ListenerWrapper(listener, priority = priority)
        listeners[tag] = wrapper
        
        lock.write {
            sortedListeners.add(wrapper)
            sortedListeners.sortByDescending { it.priority }
        }
    }
    
    override fun register(tag: String, listener: T) {
        register(tag, listener, 0)
    }
    
    override fun unregister(tag: String): Boolean {
        val wrapper = listeners.remove(tag)
        if (wrapper != null) {
            lock.write {
                sortedListeners.remove(wrapper)
            }
            return true
        }
        return false
    }
    
    override fun notifyAll(action: (T) -> Unit) {
        val snapshot = lock.read { sortedListeners.toList() }
        
        snapshot.forEach { wrapper ->
            try {
                val startTime = System.nanoTime()
                action(wrapper.listener)
                val duration = (System.nanoTime() - startTime) / 1_000_000
                
                wrapper.notificationCount++
                wrapper.lastNotificationTime = System.currentTimeMillis()
                notificationStats.recordNotification(duration)
            } catch (e: Exception) {
                wrapper.failureCount++
                notificationStats.recordNotification(0, failed = true)
            }
        }
    }
    
    override fun notifyAllSafe(action: (T) -> Unit): Int {
        val snapshot = lock.read { sortedListeners.toList() }
        var successCount = 0
        
        snapshot.forEach { wrapper ->
            try {
                val startTime = System.nanoTime()
                action(wrapper.listener)
                val duration = (System.nanoTime() - startTime) / 1_000_000
                
                wrapper.notificationCount++
                wrapper.lastNotificationTime = System.currentTimeMillis()
                notificationStats.recordNotification(duration)
                successCount++
            } catch (e: Exception) {
                wrapper.failureCount++
                notificationStats.recordNotification(0, failed = true)
            }
        }
        
        return successCount
    }
    
    override fun size(): Int = listeners.size
    
    override fun clear() {
        listeners.clear()
        lock.write {
            sortedListeners.clear()
        }
    }
    
    override fun getStats(): ListenerStats {
        val totalListeners = listeners.size
        val activeListeners = listeners.values.count { it.lastNotificationTime > 0 }
        
        return ListenerStats(
            totalListeners = totalListeners,
            activeListeners = activeListeners,
            totalNotifications = notificationStats.totalNotifications.get(),
            failedNotifications = notificationStats.failedNotifications.get(),
            avgNotificationTime = notificationStats.getAvgNotificationTime(),
            lastNotificationTime = notificationStats.lastNotificationTime.get()
        )
    }
    
    override fun hasListener(tag: String): Boolean = listeners.containsKey(tag)
    
    override fun getListener(tag: String): T? = listeners[tag]?.listener
}

/**
 * 批量通知监听器管理器
 */
class BatchNotificationListenerManager<T>(
    private val batchSize: Int = 10,
    private val flushInterval: Long = 100L
) : IListenerManager<T> {
    
    private val listeners = ConcurrentHashMap<String, ListenerWrapper<T>>()
    private val pendingNotifications = CopyOnWriteArrayList<() -> Unit>()
    private val notificationStats = NotificationStats()
    private var lastFlushTime = System.currentTimeMillis()
    
    private class NotificationStats {
        val totalNotifications = AtomicLong(0)
        val failedNotifications = AtomicLong(0)
        val totalNotificationTime = AtomicLong(0)
        val lastNotificationTime = AtomicLong(0)
        val batchCount = AtomicLong(0)
        
        fun recordNotification(duration: Long, failed: Boolean = false) {
            totalNotifications.incrementAndGet()
            totalNotificationTime.addAndGet(duration)
            lastNotificationTime.set(System.currentTimeMillis())
            if (failed) {
                failedNotifications.incrementAndGet()
            }
        }
        
        fun recordBatch() {
            batchCount.incrementAndGet()
        }
        
        fun getAvgNotificationTime(): Long {
            val total = totalNotifications.get()
            return if (total > 0) totalNotificationTime.get() / total else 0
        }
    }
    
    override fun register(tag: String, listener: T) {
        listeners[tag] = ListenerWrapper(listener)
    }
    
    override fun unregister(tag: String): Boolean {
        return listeners.remove(tag) != null
    }
    
    override fun notifyAll(action: (T) -> Unit) {
        pendingNotifications.add {
            val snapshot = listeners.values.toList()
            snapshot.forEach { wrapper ->
                try {
                    val startTime = System.nanoTime()
                    action(wrapper.listener)
                    val duration = (System.nanoTime() - startTime) / 1_000_000
                    
                    wrapper.notificationCount++
                    wrapper.lastNotificationTime = System.currentTimeMillis()
                    notificationStats.recordNotification(duration)
                } catch (e: Exception) {
                    wrapper.failureCount++
                    notificationStats.recordNotification(0, failed = true)
                }
            }
        }
        
        flushIfNeeded()
    }
    
    override fun notifyAllSafe(action: (T) -> Unit): Int {
        var totalSuccess = 0
        
        pendingNotifications.add {
            val snapshot = listeners.values.toList()
            var successCount = 0
            
            snapshot.forEach { wrapper ->
                try {
                    val startTime = System.nanoTime()
                    action(wrapper.listener)
                    val duration = (System.nanoTime() - startTime) / 1_000_000
                    
                    wrapper.notificationCount++
                    wrapper.lastNotificationTime = System.currentTimeMillis()
                    notificationStats.recordNotification(duration)
                    successCount++
                } catch (e: Exception) {
                    wrapper.failureCount++
                    notificationStats.recordNotification(0, failed = true)
                }
            }
            
            totalSuccess = successCount
        }
        
        flushIfNeeded()
        return totalSuccess
    }
    
    override fun size(): Int = listeners.size
    
    override fun clear() {
        listeners.clear()
        pendingNotifications.clear()
    }
    
    override fun getStats(): ListenerStats {
        val totalListeners = listeners.size
        val activeListeners = listeners.values.count { it.lastNotificationTime > 0 }
        
        return ListenerStats(
            totalListeners = totalListeners,
            activeListeners = activeListeners,
            totalNotifications = notificationStats.totalNotifications.get(),
            failedNotifications = notificationStats.failedNotifications.get(),
            avgNotificationTime = notificationStats.getAvgNotificationTime(),
            lastNotificationTime = notificationStats.lastNotificationTime.get()
        )
    }
    
    override fun hasListener(tag: String): Boolean = listeners.containsKey(tag)
    
    override fun getListener(tag: String): T? = listeners[tag]?.listener
    
    private fun flushIfNeeded() {
        val currentTime = System.currentTimeMillis()
        val shouldFlush = pendingNotifications.size >= batchSize || 
                         (currentTime - lastFlushTime) >= flushInterval
        
        if (shouldFlush) {
            flush()
        }
    }
    
    private fun flush() {
        if (pendingNotifications.isEmpty()) return
        
        val notifications = pendingNotifications.toList()
        pendingNotifications.clear()
        lastFlushTime = System.currentTimeMillis()
        
        notifications.forEach { notification ->
            try {
                notification()
            } catch (e: Exception) {
                // 记录批量处理错误
            }
        }
        
        notificationStats.recordBatch()
    }
    
    /**
     * 强制刷新所有待处理的通知
     */
    fun forceFlush() {
        flush()
    }
}