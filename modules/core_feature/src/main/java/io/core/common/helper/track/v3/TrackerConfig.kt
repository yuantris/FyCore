package io.core.common.helper.track.v3

/**
 * 追踪器配置
 * 
 * @author [Yuantris] - v3.0
 * @since 2025/8/10
 */
data class TrackerConfig(
    // 栈管理配置
    val maxStackSize: Int = 100,
    val enableRingBuffer: Boolean = true,
    val ringBufferSize: Int = 200,
    
    // 内存管理配置
    val cleanupThreshold: Float = 0.3f,
    val cleanupInterval: Long = 30_000L,
    val enableAdaptiveCleanup: Boolean = true,
    val maxMemoryUsage: Long = 50 * 1024 * 1024, // 50MB
    
    // 性能监控配置
    val enablePerformanceStats: Boolean = true,
    val enableDetailedMetrics: Boolean = false,
    val metricsRetentionTime: Long = 300_000L, // 5分钟
    val enableLeakDetection: Boolean = true,
    
    // 并发控制配置
    val enableLockFree: Boolean = true,
    val maxConcurrentOperations: Int = 32,
    val batchSize: Int = 10,
    val batchFlushInterval: Long = 100L,
    
    // Fragment配置
    val fragmentTimeWindow: Long = 100L,
    val maxFragmentRecords: Int = 200,
    val enableFragmentOptimization: Boolean = true,
    
    // 调试配置
    val enableDebugMode: Boolean = false,
    val enableVerboseLogging: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO,
    
    // 扩展配置
    val enableHotReload: Boolean = false,
    val enableRemoteConfig: Boolean = false,
    val configVersion: String = "3.0.0"
) {
    
    /**
     * 验证配置有效性
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (maxStackSize <= 0) errors.add("maxStackSize must be positive")
        if (ringBufferSize <= maxStackSize) errors.add("ringBufferSize should be larger than maxStackSize")
        if (cleanupThreshold !in 0.1f..0.9f) errors.add("cleanupThreshold should be between 0.1 and 0.9")
        if (cleanupInterval < 1000L) errors.add("cleanupInterval should be at least 1000ms")
        if (maxMemoryUsage < 10 * 1024 * 1024) errors.add("maxMemoryUsage should be at least 10MB")
        if (batchSize <= 0) errors.add("batchSize must be positive")
        if (maxFragmentRecords < 50) errors.add("maxFragmentRecords should be at least 50")
        
        return errors
    }
    
    /**
     * 创建默认配置
     */
    companion object {
        fun createDefault() = TrackerConfig()
        
        fun createPerformanceOptimized() = TrackerConfig(
            enableLockFree = true,
            enableAdaptiveCleanup = true,
            enableDetailedMetrics = false,
            batchSize = 20,
            maxConcurrentOperations = 64
        )
        
        fun createMemoryOptimized() = TrackerConfig(
            maxStackSize = 50,
            ringBufferSize = 100,
            cleanupThreshold = 0.2f,
            cleanupInterval = 15_000L,
            maxMemoryUsage = 20 * 1024 * 1024
        )
        
        fun createDebugMode() = TrackerConfig(
            enableDebugMode = true,
            enableVerboseLogging = true,
            enableDetailedMetrics = true,
            enableLeakDetection = true,
            logLevel = LogLevel.DEBUG
        )
    }
}

/**
 * 日志级别
 */
enum class LogLevel(val value: Int) {
    VERBOSE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    NONE(5)
}

/**
 * 性能统计数据
 */
data class PerformanceStats(
    // 基础统计
    val activityTransitions: Long = 0,
    val fragmentTransitions: Long = 0,
    val serviceTransitions: Long = 0,
    val listenerNotifications: Long = 0,
    
    // 清理统计
    val cleanupOperations: Long = 0,
    val totalCleanupTime: Long = 0,
    val avgCleanupTime: Long = 0,
    val memoryReclaimed: Long = 0,
    
    // 性能指标
    val avgResponseTime: Long = 0,
    val maxResponseTime: Long = 0,
    val minResponseTime: Long = Long.MAX_VALUE,
    val errorCount: Long = 0,
    
    // 内存指标
    val currentMemoryUsage: Long = 0,
    val maxMemoryUsage: Long = 0,
    val gcCount: Long = 0,
    
    // 并发指标
    val concurrentOperations: Int = 0,
    val maxConcurrentOperations: Int = 0,
    val lockContentions: Long = 0,
    
    // 时间戳
    val startTime: Long = System.currentTimeMillis(),
    val lastUpdateTime: Long = System.currentTimeMillis()
) {
    
    /**
     * 计算运行时长
     */
    fun getUptime(): Long = lastUpdateTime - startTime
    
    /**
     * 计算平均事务处理时间
     */
    fun getAvgTransactionTime(): Long {
        val totalTransactions = activityTransitions + fragmentTransitions + serviceTransitions
        return if (totalTransactions > 0) avgResponseTime / totalTransactions else 0
    }
    
    /**
     * 计算错误率
     */
    fun getErrorRate(): Float {
        val totalOperations = listenerNotifications + cleanupOperations
        return if (totalOperations > 0) errorCount.toFloat() / totalOperations else 0f
    }
    
    /**
     * 获取内存使用率
     */
    fun getMemoryUsageRatio(): Float {
        return if (maxMemoryUsage > 0) currentMemoryUsage.toFloat() / maxMemoryUsage else 0f
    }
    
    /**
     * 转换为Map格式
     */
    fun toMap(): Map<String, Any> = mapOf(
        "activityTransitions" to activityTransitions,
        "fragmentTransitions" to fragmentTransitions,
        "serviceTransitions" to serviceTransitions,
        "listenerNotifications" to listenerNotifications,
        "cleanupOperations" to cleanupOperations,
        "avgCleanupTime" to avgCleanupTime,
        "avgResponseTime" to avgResponseTime,
        "errorRate" to getErrorRate(),
        "memoryUsageRatio" to getMemoryUsageRatio(),
        "uptime" to getUptime(),
        "concurrentOperations" to concurrentOperations
    )
}

/**
 * 内存泄漏信息
 */
data class LeakInfo(
    val componentType: String,
    val componentName: String,
    val leakTime: Long,
    val retainedSize: Long,
    val stackTrace: String? = null,
    val suggestions: List<String> = emptyList()
)

/**
 * 使用模式统计
 */
data class UsagePattern(
    val avgActivityLifetime: Long,
    val avgFragmentLifetime: Long,
    val peakConcurrency: Int,
    val memoryGrowthRate: Float,
    val cleanupEfficiency: Float
)