package io.core.common.helper.track.v3

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 性能监控器接口
 * 
 * @author [Yuantris] - v3.0
 * @since 2025/8/10
 */
interface IPerformanceMonitor {
    fun recordOperation(operation: String, duration: Long, success: Boolean = true)
    fun recordMemoryUsage(usage: Long)
    fun recordConcurrentOperation(count: Int)
    fun startTimer(operation: String): TimerContext
    fun getStats(): DetailedPerformanceStats
    fun getOperationStats(operation: String): OperationStats?
    fun reset()
    fun exportMetrics(): Map<String, Any>
}

/**
 * 计时器上下文
 */
interface TimerContext {
    fun stop(): Long
    fun stopAndRecord(success: Boolean = true): Long
}

/**
 * 操作统计信息
 */
data class OperationStats(
    val operationName: String,
    val totalCount: Long,
    val successCount: Long,
    val failureCount: Long,
    val totalDuration: Long,
    val minDuration: Long,
    val maxDuration: Long,
    val avgDuration: Long,
    val lastExecutionTime: Long
) {
    val successRate: Float get() = if (totalCount > 0) successCount.toFloat() / totalCount else 0f
    val failureRate: Float get() = if (totalCount > 0) failureCount.toFloat() / totalCount else 0f
}

/**
 * 详细性能统计
 */
data class DetailedPerformanceStats(
    // 基础统计
    val totalOperations: Long,
    val successfulOperations: Long,
    val failedOperations: Long,
    val avgResponseTime: Long,
    val maxResponseTime: Long,
    val minResponseTime: Long,
    
    // 内存统计
    val currentMemoryUsage: Long,
    val maxMemoryUsage: Long,
    val avgMemoryUsage: Long,
    val memoryGrowthRate: Float,
    
    // 并发统计
    val currentConcurrency: Int,
    val maxConcurrency: Int,
    val avgConcurrency: Float,
    
    // 时间统计
    val uptime: Long,
    val lastOperationTime: Long,
    val operationsPerSecond: Float,
    
    // 操作分布
    val operationStats: Map<String, OperationStats>,
    
    // 系统资源
    val cpuUsage: Float,
    val availableMemory: Long,
    val gcCount: Long,
    val gcTime: Long
) {
    
    fun toMap(): Map<String, Any> = mapOf(
        "totalOperations" to totalOperations,
        "successRate" to (if (totalOperations > 0) successfulOperations.toFloat() / totalOperations else 0f),
        "avgResponseTime" to avgResponseTime,
        "maxResponseTime" to maxResponseTime,
        "currentMemoryUsage" to currentMemoryUsage,
        "maxMemoryUsage" to maxMemoryUsage,
        "memoryGrowthRate" to memoryGrowthRate,
        "currentConcurrency" to currentConcurrency,
        "maxConcurrency" to maxConcurrency,
        "uptime" to uptime,
        "operationsPerSecond" to operationsPerSecond,
        "cpuUsage" to cpuUsage,
        "gcCount" to gcCount
    )
}

/**
 * 内存指标
 */
data class MemoryMetrics(
    val used: Long,
    val max: Long,
    val free: Long,
    val total: Long,
    val growthRate: Float
) {
    val usageRatio: Float get() = if (max > 0) used.toFloat() / max else 0f
}

/**
 * 响应时间指标
 */
data class ResponseTimeMetrics(
    val avg: Long,
    val min: Long,
    val max: Long,
    val p50: Long,
    val p90: Long,
    val p95: Long,
    val p99: Long
)

/**
 * 高性能监控器实现
 */
class HighPerformanceMonitor : IPerformanceMonitor {
    
    private val operationStats = ConcurrentHashMap<String, MutableOperationStats>()
    private val memoryHistory = CircularBuffer<Long>(100)
    private val concurrencyHistory = CircularBuffer<Int>(100)
    private val responseTimeHistory = CircularBuffer<Long>(1000)
    
    private val totalOperations = AtomicLong(0)
    private val successfulOperations = AtomicLong(0)
    private val failedOperations = AtomicLong(0)
    private val currentMemoryUsage = AtomicLong(0)
    private val maxMemoryUsage = AtomicLong(0)
    private val currentConcurrency = AtomicLong(0)
    private val maxConcurrency = AtomicLong(0)
    private val startTime = System.currentTimeMillis()
    private val lastOperationTime = AtomicLong(0)
    
    private val lock = ReentrantReadWriteLock()
    
    private class MutableOperationStats(
        val operationName: String
    ) {
        val totalCount = AtomicLong(0)
        val successCount = AtomicLong(0)
        val failureCount = AtomicLong(0)
        val totalDuration = AtomicLong(0)
        val minDuration = AtomicLong(Long.MAX_VALUE)
        val maxDuration = AtomicLong(0)
        val lastExecutionTime = AtomicLong(0)
        
        fun record(duration: Long, success: Boolean) {
            totalCount.incrementAndGet()
            if (success) successCount.incrementAndGet() else failureCount.incrementAndGet()
            totalDuration.addAndGet(duration)
            lastExecutionTime.set(System.currentTimeMillis())
            
            // 更新最小值
            var currentMin = minDuration.get()
            while (duration < currentMin && !minDuration.compareAndSet(currentMin, duration)) {
                currentMin = minDuration.get()
            }
            
            // 更新最大值
            var currentMax = maxDuration.get()
            while (duration > currentMax && !maxDuration.compareAndSet(currentMax, duration)) {
                currentMax = maxDuration.get()
            }
        }
        
        fun toOperationStats(): OperationStats {
            val total = totalCount.get()
            val avgDuration = if (total > 0) totalDuration.get() / total else 0
            
            return OperationStats(
                operationName = operationName,
                totalCount = total,
                successCount = successCount.get(),
                failureCount = failureCount.get(),
                totalDuration = totalDuration.get(),
                minDuration = if (minDuration.get() == Long.MAX_VALUE) 0 else minDuration.get(),
                maxDuration = maxDuration.get(),
                avgDuration = avgDuration,
                lastExecutionTime = lastExecutionTime.get()
            )
        }
    }
    
    override fun recordOperation(operation: String, duration: Long, success: Boolean) {
        val stats = operationStats.computeIfAbsent(operation) { MutableOperationStats(it) }
        stats.record(duration, success)
        
        totalOperations.incrementAndGet()
        if (success) successfulOperations.incrementAndGet() else failedOperations.incrementAndGet()
        lastOperationTime.set(System.currentTimeMillis())
        
        lock.write {
            responseTimeHistory.add(duration)
        }
    }
    
    override fun recordMemoryUsage(usage: Long) {
        currentMemoryUsage.set(usage)
        
        // 更新最大内存使用量
        var currentMax = maxMemoryUsage.get()
        while (usage > currentMax && !maxMemoryUsage.compareAndSet(currentMax, usage)) {
            currentMax = maxMemoryUsage.get()
        }
        
        lock.write {
            memoryHistory.add(usage)
        }
    }
    
    override fun recordConcurrentOperation(count: Int) {
        currentConcurrency.set(count.toLong())
        
        // 更新最大并发数
        var currentMax = maxConcurrency.get()
        while (count > currentMax && !maxConcurrency.compareAndSet(currentMax, count.toLong())) {
            currentMax = maxConcurrency.get()
        }
        
        lock.write {
            concurrencyHistory.add(count)
        }
    }
    
    override fun startTimer(operation: String): TimerContext {
        return TimerContextImpl(operation, this)
    }
    
    override fun getStats(): DetailedPerformanceStats {
        val currentTime = System.currentTimeMillis()
        val uptime = currentTime - startTime
        val totalOps = totalOperations.get()
        val operationsPerSecond = if (uptime > 0) totalOps * 1000f / uptime else 0f
        
        val memoryMetrics = calculateMemoryMetrics()
        val responseTimeMetrics = calculateResponseTimeMetrics()
        val avgConcurrency = calculateAvgConcurrency()
        
        val operationStatsMap = operationStats.mapValues { it.value.toOperationStats() }
        
        return DetailedPerformanceStats(
            totalOperations = totalOps,
            successfulOperations = successfulOperations.get(),
            failedOperations = failedOperations.get(),
            avgResponseTime = responseTimeMetrics.avg,
            maxResponseTime = responseTimeMetrics.max,
            minResponseTime = responseTimeMetrics.min,
            currentMemoryUsage = currentMemoryUsage.get(),
            maxMemoryUsage = maxMemoryUsage.get(),
            avgMemoryUsage = memoryMetrics.used,
            memoryGrowthRate = memoryMetrics.growthRate,
            currentConcurrency = currentConcurrency.get().toInt(),
            maxConcurrency = maxConcurrency.get().toInt(),
            avgConcurrency = avgConcurrency,
            uptime = uptime,
            lastOperationTime = lastOperationTime.get(),
            operationsPerSecond = operationsPerSecond,
            operationStats = operationStatsMap,
            cpuUsage = getCpuUsage(),
            availableMemory = getAvailableMemory(),
            gcCount = getGcCount(),
            gcTime = getGcTime()
        )
    }
    
    override fun getOperationStats(operation: String): OperationStats? {
        return operationStats[operation]?.toOperationStats()
    }
    
    override fun reset() {
        operationStats.clear()
        totalOperations.set(0)
        successfulOperations.set(0)
        failedOperations.set(0)
        currentMemoryUsage.set(0)
        maxMemoryUsage.set(0)
        currentConcurrency.set(0)
        maxConcurrency.set(0)
        lastOperationTime.set(0)
        
        lock.write {
            memoryHistory.clear()
            concurrencyHistory.clear()
            responseTimeHistory.clear()
        }
    }
    
    override fun exportMetrics(): Map<String, Any> {
        return getStats().toMap()
    }
    
    private fun calculateMemoryMetrics(): MemoryMetrics {
        return lock.read {
            val history = memoryHistory.toList()
            if (history.isEmpty()) {
                return@read MemoryMetrics(0, 0, 0, 0, 0f)
            }
            
            val current = currentMemoryUsage.get()
            val max = maxMemoryUsage.get()
            val avg = history.average().toLong()
            
            // 计算增长率
            val growthRate = if (history.size >= 2) {
                val first = history.first()
                val last = history.last()
                if (first > 0) (last - first).toFloat() / first else 0f
            } else 0f
            
            MemoryMetrics(
                used = current,
                max = max,
                free = max - current,
                total = max,
                growthRate = growthRate
            )
        }
    }
    
    private fun calculateResponseTimeMetrics(): ResponseTimeMetrics {
        return lock.read {
            val history = responseTimeHistory.toList().sorted()
            if (history.isEmpty()) {
                return@read ResponseTimeMetrics(0, 0, 0, 0, 0, 0, 0)
            }
            
            val size = history.size
            ResponseTimeMetrics(
                avg = history.average().toLong(),
                min = history.first(),
                max = history.last(),
                p50 = history[size * 50 / 100],
                p90 = history[size * 90 / 100],
                p95 = history[size * 95 / 100],
                p99 = history[size * 99 / 100]
            )
        }
    }
    
    private fun calculateAvgConcurrency(): Float {
        return lock.read {
            val history = concurrencyHistory.toList()
            if (history.isEmpty()) 0f else history.average().toFloat()
        }
    }
    
    private fun getCpuUsage(): Float {
        // 简化的CPU使用率获取，实际实现可能需要更复杂的逻辑
        return try {
            val runtime = Runtime.getRuntime()
            val processors = runtime.availableProcessors()
            // 这里应该实现真正的CPU使用率计算
            0f
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun getAvailableMemory(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            runtime.freeMemory()
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun getGcCount(): Long {
        // 简化实现，实际可能需要使用 ManagementFactory
        return 0L
    }
    
    private fun getGcTime(): Long {
        // 简化实现，实际可能需要使用 ManagementFactory
        return 0L
    }
}

/**
 * 计时器上下文实现
 */
private class TimerContextImpl(
    private val operation: String,
    private val monitor: IPerformanceMonitor
) : TimerContext {
    
    private val startTime = System.nanoTime()
    private val stopped = AtomicReference(false)
    
    override fun stop(): Long {
        if (stopped.compareAndSet(false, true)) {
            return (System.nanoTime() - startTime) / 1_000_000 // 转换为毫秒
        }
        return 0
    }
    
    override fun stopAndRecord(success: Boolean): Long {
        val duration = stop()
        if (duration > 0) {
            monitor.recordOperation(operation, duration, success)
        }
        return duration
    }
}

/**
 * 环形缓冲区
 */
private class CircularBuffer<T>(private val capacity: Int) {
    private val buffer = Array<Any?>(capacity) { null }
    private var head = 0
    private var tail = 0
    private var size = 0
    
    @Synchronized
    fun add(item: T) {
        buffer[tail] = item
        tail = (tail + 1) % capacity
        
        if (size < capacity) {
            size++
        } else {
            head = (head + 1) % capacity
        }
    }
    
    @Synchronized
    fun toList(): List<T> {
        val result = mutableListOf<T>()
        var current = head
        repeat(size) {
            @Suppress("UNCHECKED_CAST")
            result.add(buffer[current] as T)
            current = (current + 1) % capacity
        }
        return result
    }
    
    @Synchronized
    fun clear() {
        head = 0
        tail = 0
        size = 0
        buffer.fill(null)
    }
}