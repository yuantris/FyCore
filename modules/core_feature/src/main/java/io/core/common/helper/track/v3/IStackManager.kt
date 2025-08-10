package io.core.common.helper.track.v3

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 栈管理器接口
 * 
 * @author [Yuantris] - v3.0
 * @since 2025/8/10
 */
interface IStackManager<T> {
    fun add(item: T)
    fun remove(item: T): Boolean
    fun removeAll(predicate: (T?) -> Boolean): Int
    fun getTop(): T?
    fun getAll(): List<T>
    fun size(): Int
    fun clear()
    fun getStats(): StackStats
}

/**
 * 栈统计信息
 */
data class StackStats(
    val totalSize: Int,
    val validSize: Int,
    val invalidSize: Int,
    val cleanupCount: Long,
    val lastCleanupTime: Long,
    val memoryUsage: Long
)

/**
 * 智能弱引用
 */
class SmartWeakReference<T>(
    referent: T,
    queue: ReferenceQueue<T>,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: Int = 0
) : WeakReference<T>(referent, queue) {
    
    val id = System.identityHashCode(referent)
    
    fun isExpired(ttl: Long): Boolean = System.currentTimeMillis() - timestamp > ttl
}

/**
 * 无锁环形缓冲区栈管理器
 */
class LockFreeRingBufferStackManager<T>(
    private val capacity: Int = 200,
    private val cleanupThreshold: Float = 0.3f
) : IStackManager<T> {
    
    private val buffer = Array<AtomicReference<SmartWeakReference<T>?>>(capacity) { 
        AtomicReference(null) 
    }
    private val head = AtomicInteger(0)
    private val tail = AtomicInteger(0)
    private val size = AtomicInteger(0)
    private val referenceQueue = ReferenceQueue<T>()
    private val invalidCount = AtomicInteger(0)
    private val cleanupCount = AtomicLong(0)
    private val lastCleanupTime = AtomicLong(0)
    
    override fun add(item: T) {
        val ref = SmartWeakReference(item, referenceQueue)
        val currentTail = tail.get()
        val nextTail = (currentTail + 1) % capacity
        
        // 如果缓冲区满了，移除最旧的元素
        if (nextTail == head.get()) {
            moveHead()
        }
        
        buffer[currentTail].set(ref)
        tail.set(nextTail)
        size.incrementAndGet()
        
        processReferenceQueue()
        cleanupIfNeeded()
    }
    
    override fun remove(item: T): Boolean {
        val itemId = System.identityHashCode(item)
        var removed = false
        
        for (i in 0 until capacity) {
            val ref = buffer[i].get()
            if (ref?.id == itemId && ref.get() == item) {
                if (buffer[i].compareAndSet(ref, null)) {
                    size.decrementAndGet()
                    removed = true
                }
            }
        }
        
        return removed
    }
    
    override fun removeAll(predicate: (T?) -> Boolean): Int {
        var removedCount = 0
        
        for (i in 0 until capacity) {
            val ref = buffer[i].get()
            val item = ref?.get()
            if (predicate(item)) {
                if (buffer[i].compareAndSet(ref, null)) {
                    size.decrementAndGet()
                    removedCount++
                }
            }
        }
        
        return removedCount
    }
    
    override fun getTop(): T? {
        cleanupIfNeeded()
        
        // 从tail向head方向查找最新的有效元素
        var current = (tail.get() - 1 + capacity) % capacity
        val start = current
        
        do {
            val ref = buffer[current].get()
            val item = ref?.get()
            if (item != null) {
                return item
            }
            current = (current - 1 + capacity) % capacity
        } while (current != start)
        
        return null
    }
    
    override fun getAll(): List<T> {
        cleanupIfNeeded()
        val result = mutableListOf<T>()
        
        // 从tail向head方向收集所有有效元素
        var current = (tail.get() - 1 + capacity) % capacity
        val start = current
        
        do {
            val ref = buffer[current].get()
            val item = ref?.get()
            if (item != null) {
                result.add(item)
            }
            current = (current - 1 + capacity) % capacity
        } while (current != start && result.size < capacity)
        
        return result
    }
    
    override fun size(): Int = size.get()
    
    override fun clear() {
        for (i in 0 until capacity) {
            buffer[i].set(null)
        }
        head.set(0)
        tail.set(0)
        size.set(0)
        invalidCount.set(0)
    }
    
    override fun getStats(): StackStats {
        val totalSize = size.get()
        val invalidSize = invalidCount.get()
        val validSize = totalSize - invalidSize
        
        return StackStats(
            totalSize = totalSize,
            validSize = validSize,
            invalidSize = invalidSize,
            cleanupCount = cleanupCount.get(),
            lastCleanupTime = lastCleanupTime.get(),
            memoryUsage = estimateMemoryUsage()
        )
    }
    
    private fun moveHead() {
        val currentHead = head.get()
        val ref = buffer[currentHead].get()
        if (ref != null) {
            buffer[currentHead].set(null)
            size.decrementAndGet()
        }
        head.set((currentHead + 1) % capacity)
    }
    
    private fun processReferenceQueue() {
        var processed = 0
        while (referenceQueue.poll() != null) {
            invalidCount.incrementAndGet()
            processed++
        }
    }
    
    private fun cleanupIfNeeded() {
        val totalSize = size.get()
        if (totalSize == 0) return
        
        processReferenceQueue()
        val invalidRatio = invalidCount.get().toFloat() / totalSize
        
        if (invalidRatio > cleanupThreshold) {
            performCleanup()
        }
    }
    
    private fun performCleanup() {
        val startTime = System.nanoTime()
        var cleaned = 0
        
        for (i in 0 until capacity) {
            val ref = buffer[i].get()
            if (ref?.get() == null) {
                if (buffer[i].compareAndSet(ref, null)) {
                    size.decrementAndGet()
                    cleaned++
                }
            }
        }
        
        invalidCount.addAndGet(-cleaned)
        cleanupCount.incrementAndGet()
        lastCleanupTime.set(System.currentTimeMillis())
        
        val duration = (System.nanoTime() - startTime) / 1_000_000
        // 可以在这里记录清理性能指标
    }
    
    private fun estimateMemoryUsage(): Long {
        // 估算内存使用量（字节）
        val refSize = 32L // 每个引用大约32字节
        val bufferSize = capacity * 8L // 数组指针
        return size.get() * refSize + bufferSize
    }
}

/**
 * 传统锁式栈管理器（向后兼容）
 */
class LockBasedStackManager<T>(
    private val maxSize: Int = 100,
    private val cleanupThreshold: Float = 0.3f
) : IStackManager<T> {
    
    private val stack = ArrayDeque<SmartWeakReference<T>>()
    private val referenceQueue = ReferenceQueue<T>()
    private val lock = ReentrantReadWriteLock()
    private val invalidCount = AtomicInteger(0)
    private val cleanupCount = AtomicLong(0)
    private val lastCleanupTime = AtomicLong(0)
    
    override fun add(item: T) {
        val ref = SmartWeakReference(item, referenceQueue)
        lock.write {
            stack.addFirst(ref)
            if (stack.size > maxSize) {
                stack.removeLast()
            }
        }
        processReferenceQueue()
        cleanupIfNeeded()
    }
    
    override fun remove(item: T): Boolean {
        return lock.write {
            stack.removeAll { it.get() == item }
        }
    }
    
    override fun removeAll(predicate: (T?) -> Boolean): Int {
        return lock.write {
            val sizeBefore = stack.size
            stack.removeAll { ref -> predicate(ref.get()) }
            sizeBefore - stack.size
        }
    }
    
    override fun getTop(): T? {
        cleanupIfNeeded()
        return lock.read {
            stack.firstOrNull { it.get() != null }?.get()
        }
    }
    
    override fun getAll(): List<T> {
        cleanupIfNeeded()
        return lock.read {
            stack.mapNotNull { it.get() }
        }
    }
    
    override fun size(): Int = lock.read { stack.size }
    
    override fun clear() {
        lock.write {
            stack.clear()
            invalidCount.set(0)
        }
    }
    
    override fun getStats(): StackStats {
        return lock.read {
            val totalSize = stack.size
            val invalidSize = invalidCount.get()
            val validSize = totalSize - invalidSize
            
            StackStats(
                totalSize = totalSize,
                validSize = validSize,
                invalidSize = invalidSize,
                cleanupCount = cleanupCount.get(),
                lastCleanupTime = lastCleanupTime.get(),
                memoryUsage = estimateMemoryUsage()
            )
        }
    }
    
    private fun processReferenceQueue() {
        var processed = 0
        while (referenceQueue.poll() != null) {
            invalidCount.incrementAndGet()
            processed++
        }
    }
    
    private fun cleanupIfNeeded() {
        val totalSize = size()
        if (totalSize == 0) return
        
        processReferenceQueue()
        val invalidRatio = invalidCount.get().toFloat() / totalSize
        
        if (invalidRatio > cleanupThreshold) {
            performCleanup()
        }
    }
    
    private fun performCleanup() {
        lock.write {
            val sizeBefore = stack.size
            stack.removeAll { it.get() == null }
            val cleaned = sizeBefore - stack.size
            
            invalidCount.addAndGet(-cleaned)
            cleanupCount.incrementAndGet()
            lastCleanupTime.set(System.currentTimeMillis())
        }
    }
    
    private fun estimateMemoryUsage(): Long {
        return lock.read {
            stack.size * 32L + 64L // 估算值
        }
    }
}