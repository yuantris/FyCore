package io.core.common.helper.track.v3

import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 内存管理器接口
 * 
 * @author [Yuantris] - v3.0
 * @since 2025/8/10
 */
interface IMemoryManager {
    fun trackObject(obj: Any, tag: String = "default")
    fun untrackObject(obj: Any): Boolean
    fun forceCleanup()
    fun scheduleCleanup(interval: Long, unit: TimeUnit): ScheduledFuture<*>?
    fun detectLeaks(): List<LeakInfo>
    fun getMemoryStats(): MemoryStats
    fun setMemoryThreshold(threshold: Long)
    fun enableLeakDetection(enabled: Boolean)
    fun destroy()
}

/**
 * 内存统计信息
 */
data class MemoryStats(
    val trackedObjects: Int,
    val leakedObjects: Int,
    val totalMemoryUsage: Long,
    val cleanupCount: Long,
    val lastCleanupTime: Long,
    val memoryThreshold: Long,
    val isThresholdExceeded: Boolean,
    val leakDetectionEnabled: Boolean
)

/**
 * 对象追踪信息
 */
private data class ObjectTrackInfo(
    val tag: String,
    val creationTime: Long,
    val stackTrace: String?,
    val objectHash: Int
)

/**
 * 智能内存管理器
 */
class SmartMemoryManager(
    private val config: TrackerConfig
) : IMemoryManager {
    
    private val trackedObjects = ConcurrentHashMap<Int, ObjectTrackInfo>()
    private val phantomReferences = ConcurrentHashMap<PhantomReference<*>, ObjectTrackInfo>()
    private val referenceQueue = ReferenceQueue<Any>()
    private val memoryThreshold = AtomicLong(config.maxMemoryUsage)
    private val leakDetectionEnabled = AtomicReference(config.enableLeakDetection)
    private val cleanupCount = AtomicLong(0)
    private val lastCleanupTime = AtomicLong(0)
    private val cleanupScheduler = AtomicReference<ScheduledFuture<*>?>(null)
    
    // 内存池
    private val objectPool = ObjectPool<ObjectTrackInfo>(
        factory = { ObjectTrackInfo("", 0, null, 0) },
        maxSize = 100
    )
    
    override fun trackObject(obj: Any, tag: String) {
        if (!leakDetectionEnabled.get()) return
        
        val objectHash = System.identityHashCode(obj)
        val stackTrace = if (config.enableVerboseLogging) {
            Thread.currentThread().stackTrace.joinToString("\n") { it.toString() }
        } else null
        
        val trackInfo = ObjectTrackInfo(
            tag = tag,
            creationTime = System.currentTimeMillis(),
            stackTrace = stackTrace,
            objectHash = objectHash
        )
        
        trackedObjects[objectHash] = trackInfo
        
        // 创建幻象引用用于检测对象回收
        val phantomRef = PhantomReference(obj, referenceQueue)
        phantomReferences[phantomRef] = trackInfo
        
        // 检查内存阈值
        checkMemoryThreshold()
    }
    
    override fun untrackObject(obj: Any): Boolean {
        val objectHash = System.identityHashCode(obj)
        val removed = trackedObjects.remove(objectHash) != null
        
        // 清理对应的幻象引用
        phantomReferences.entries.removeAll { (_, info) ->
            info.objectHash == objectHash
        }
        
        return removed
    }
    
    override fun forceCleanup() {
        val startTime = System.currentTimeMillis()
        var cleanedCount = 0
        
        // 处理引用队列
        while (true) {
            val ref = referenceQueue.poll() ?: break
            if (ref is PhantomReference<*>) {
                phantomReferences.remove(ref)?.let { info ->
                    trackedObjects.remove(info.objectHash)
                    cleanedCount++
                }
            }
        }
        
        // 清理过期的追踪信息
        val currentTime = System.currentTimeMillis()
        val expiredThreshold = currentTime - config.metricsRetentionTime
        
        trackedObjects.entries.removeAll { (_, info) ->
            info.creationTime < expiredThreshold
        }
        
        cleanupCount.incrementAndGet()
        lastCleanupTime.set(currentTime)
        
        val duration = currentTime - startTime
        if (config.enableVerboseLogging) {
            println("Memory cleanup completed: cleaned $cleanedCount objects in ${duration}ms")
        }
    }
    
    override fun scheduleCleanup(interval: Long, unit: TimeUnit): ScheduledFuture<*>? {
        // 取消之前的调度
        cleanupScheduler.get()?.cancel(false)
        
        // 这里需要传入 ScheduledExecutorService，简化实现
        return null
    }
    
    override fun detectLeaks(): List<LeakInfo> {
        if (!leakDetectionEnabled.get()) return emptyList()
        
        val currentTime = System.currentTimeMillis()
        val leakThreshold = currentTime - 300_000L // 5分钟阈值
        val leaks = mutableListOf<LeakInfo>()
        
        trackedObjects.values.forEach { info ->
            if (info.creationTime < leakThreshold) {
                val suggestions = generateLeakSuggestions(info)
                leaks.add(
                    LeakInfo(
                        componentType = "TrackedObject",
                        componentName = info.tag,
                        leakTime = currentTime - info.creationTime,
                        retainedSize = estimateObjectSize(info),
                        stackTrace = info.stackTrace,
                        suggestions = suggestions
                    )
                )
            }
        }
        
        return leaks
    }
    
    override fun getMemoryStats(): MemoryStats {
        val currentMemory = getCurrentMemoryUsage()
        val threshold = memoryThreshold.get()
        
        return MemoryStats(
            trackedObjects = trackedObjects.size,
            leakedObjects = detectLeaks().size,
            totalMemoryUsage = currentMemory,
            cleanupCount = cleanupCount.get(),
            lastCleanupTime = lastCleanupTime.get(),
            memoryThreshold = threshold,
            isThresholdExceeded = currentMemory > threshold,
            leakDetectionEnabled = leakDetectionEnabled.get()
        )
    }
    
    override fun setMemoryThreshold(threshold: Long) {
        memoryThreshold.set(threshold)
    }
    
    override fun enableLeakDetection(enabled: Boolean) {
        leakDetectionEnabled.set(enabled)
        if (!enabled) {
            // 清理所有追踪信息
            trackedObjects.clear()
            phantomReferences.clear()
        }
    }
    
    override fun destroy() {
        cleanupScheduler.get()?.cancel(true)
        trackedObjects.clear()
        phantomReferences.clear()
        objectPool.clear()
    }
    
    private fun checkMemoryThreshold() {
        val currentMemory = getCurrentMemoryUsage()
        val threshold = memoryThreshold.get()
        
        if (currentMemory > threshold) {
            if (config.enableVerboseLogging) {
                println("Memory threshold exceeded: ${currentMemory}/${threshold} bytes")
            }
            
            // 触发清理
            if (config.enableAdaptiveCleanup) {
                forceCleanup()
            }
        }
    }
    
    private fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun estimateObjectSize(info: ObjectTrackInfo): Long {
        // 简化的对象大小估算
        return when (info.tag) {
            "Activity" -> 1024 * 50L // 50KB
            "Fragment" -> 1024 * 20L // 20KB
            "Service" -> 1024 * 30L // 30KB
            else -> 1024L // 1KB
        }
    }
    
    private fun generateLeakSuggestions(info: ObjectTrackInfo): List<String> {
        val suggestions = mutableListOf<String>()
        
        when (info.tag) {
            "Activity" -> {
                suggestions.add("检查Activity是否正确调用了finish()")
                suggestions.add("确保没有静态引用持有Activity实例")
                suggestions.add("检查是否有未取消的异步任务引用Activity")
            }
            "Fragment" -> {
                suggestions.add("检查Fragment是否正确从FragmentManager中移除")
                suggestions.add("确保Fragment中的监听器已正确注销")
                suggestions.add("检查是否有循环引用")
            }
            "Service" -> {
                suggestions.add("检查Service是否正确调用了stopSelf()")
                suggestions.add("确保Service中的资源已正确释放")
            }
            else -> {
                suggestions.add("检查对象的生命周期管理")
                suggestions.add("确保及时释放不再使用的引用")
            }
        }
        
        return suggestions
    }
}

/**
 * 自适应清理策略
 */
class AdaptiveCleanupStrategy(
    private val memoryManager: IMemoryManager,
    private val performanceMonitor: IPerformanceMonitor
) {
    
    private var lastCleanupTime = 0L
    private var cleanupInterval = 30_000L // 初始30秒
    private val minInterval = 5_000L // 最小5秒
    private val maxInterval = 300_000L // 最大5分钟
    
    fun shouldCleanup(stats: MemoryStats): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCleanup = currentTime - lastCleanupTime
        
        // 基于内存使用情况调整清理策略
        return when {
            stats.isThresholdExceeded -> true
            stats.leakedObjects > 10 -> true
            timeSinceLastCleanup > cleanupInterval -> true
            else -> false
        }
    }
    
    fun getOptimalCleanupInterval(pattern: UsagePattern): Long {
        // 基于使用模式调整清理间隔
        val baseInterval = when {
            pattern.memoryGrowthRate > 0.1f -> minInterval
            pattern.memoryGrowthRate > 0.05f -> cleanupInterval / 2
            pattern.memoryGrowthRate < 0.01f -> maxInterval
            else -> cleanupInterval
        }
        
        // 基于清理效率调整
        val efficiencyFactor = when {
            pattern.cleanupEfficiency > 0.8f -> 1.5f
            pattern.cleanupEfficiency > 0.5f -> 1.0f
            else -> 0.7f
        }
        
        cleanupInterval = (baseInterval * efficiencyFactor).toLong()
            .coerceIn(minInterval, maxInterval)
        
        return cleanupInterval
    }
    
    fun performAdaptiveCleanup() {
        val stats = memoryManager.getMemoryStats()
        
        if (shouldCleanup(stats)) {
            val startTime = System.currentTimeMillis()
            memoryManager.forceCleanup()
            val duration = System.currentTimeMillis() - startTime
            
            performanceMonitor.recordOperation("adaptive_cleanup", duration)
            lastCleanupTime = startTime
        }
    }
}

/**
 * 对象池
 */
class ObjectPool<T>(
    private val factory: () -> T,
    private val maxSize: Int = 50
) {
    private val pool = ConcurrentHashMap<T, Boolean>()
    private val available = mutableListOf<T>()
    
    @Synchronized
    fun acquire(): T {
        return if (available.isNotEmpty()) {
            val obj = available.removeAt(available.size - 1)
            pool[obj] = false // 标记为使用中
            obj
        } else {
            val obj = factory()
            pool[obj] = false
            obj
        }
    }
    
    @Synchronized
    fun release(obj: T) {
        if (pool.containsKey(obj) && available.size < maxSize) {
            pool[obj] = true // 标记为可用
            available.add(obj)
        }
    }
    
    @Synchronized
    fun clear() {
        pool.clear()
        available.clear()
    }
    
    fun size(): Int = pool.size
    fun availableCount(): Int = available.size
}

/**
 * 内存泄漏检测器
 */
class LeakDetector(
    private val memoryManager: IMemoryManager
) {
    
    fun detectLeaks(): List<LeakInfo> {
        return memoryManager.detectLeaks()
    }
    
    fun suggestFixes(leaks: List<LeakInfo>): List<String> {
        val suggestions = mutableListOf<String>()
        
        val leaksByType = leaks.groupBy { it.componentType }
        
        leaksByType.forEach { (type, typeLeaks) ->
            suggestions.add("发现 ${typeLeaks.size} 个 $type 类型的内存泄漏:")
            
            typeLeaks.forEach { leak ->
                suggestions.add("  - ${leak.componentName}: 泄漏时间 ${leak.leakTime}ms")
                leak.suggestions.forEach { suggestion ->
                    suggestions.add("    * $suggestion")
                }
            }
        }
        
        // 通用建议
        if (leaks.isNotEmpty()) {
            suggestions.add("\n通用建议:")
            suggestions.add("- 定期检查和清理不再使用的对象引用")
            suggestions.add("- 使用弱引用来避免循环引用")
            suggestions.add("- 在组件销毁时及时注销监听器和回调")
            suggestions.add("- 使用内存分析工具进行深入分析")
        }
        
        return suggestions
    }
    
    fun generateReport(): String {
        val leaks = detectLeaks()
        val suggestions = suggestFixes(leaks)
        
        return buildString {
            appendLine("=== 内存泄漏检测报告 ===")
            appendLine("检测时间: ${System.currentTimeMillis()}")
            appendLine("发现泄漏: ${leaks.size} 个")
            appendLine()
            
            if (leaks.isNotEmpty()) {
                appendLine("详细信息:")
                suggestions.forEach { suggestion ->
                    appendLine(suggestion)
                }
            } else {
                appendLine("未发现内存泄漏")
            }
            
            appendLine("\n=== 报告结束 ===")
        }
    }
}