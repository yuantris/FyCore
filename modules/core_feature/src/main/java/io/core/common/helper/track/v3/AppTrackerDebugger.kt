package io.core.common.helper.track.v3

import android.app.Activity
import androidx.fragment.app.Fragment
import io.core.common.util.extensions.ui.isAlive
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 应用追踪器调试器
 * 
 * @author [Yuantris] - v3.0
 * @since 2025/8/10
 */
class AppTrackerDebugger(
    private val tracker: IAppTracker
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val debugLogs = mutableListOf<DebugLogEntry>()
    private val maxLogEntries = 1000
    
    data class DebugLogEntry(
        val timestamp: Long,
        val level: String,
        val category: String,
        val message: String,
        val details: Map<String, Any> = emptyMap()
    )
    
    /**
     * 生成详细的调试报告
     */
    fun generateReport(): String {
        return buildString {
            appendLine("=== AppTrackV3 调试报告 ===")
            appendLine("生成时间: ${dateFormat.format(Date())}")
            appendLine()
            
            // 基础信息
            appendLine("## 基础信息")
            appendLine("活跃Activity数量: ${tracker.getActiveActivities().size}")
            appendLine("栈顶Activity: ${tracker.getTopActivity()?.javaClass?.simpleName ?: "无"}")
            appendLine("栈顶Fragment: ${tracker.getTopFragment()?.javaClass?.simpleName ?: "无"}")
            appendLine()
            
            // 性能统计
            appendLine("## 性能统计")
            val stats = tracker.getPerformanceStats()
            appendLine("Activity转换次数: ${stats.activityTransitions}")
            appendLine("Fragment转换次数: ${stats.fragmentTransitions}")
            appendLine("监听器通知次数: ${stats.listenerNotifications}")
            appendLine("清理操作次数: ${stats.cleanupOperations}")
            appendLine("平均响应时间: ${stats.avgResponseTime}ms")
            appendLine("最大响应时间: ${stats.maxResponseTime}ms")
            appendLine("错误次数: ${stats.errorCount}")
            appendLine("当前内存使用: ${formatBytes(stats.currentMemoryUsage)}")
            appendLine("最大内存使用: ${formatBytes(stats.maxMemoryUsage)}")
            appendLine("并发操作数: ${stats.concurrentOperations}")
            appendLine("运行时长: ${formatDuration(stats.getUptime())}")
            appendLine()
            
            // Activity栈信息
            appendLine("## Activity栈")
            val activities = tracker.getActiveActivities()
            if (activities.isEmpty()) {
                appendLine("无活跃Activity")
            } else {
                activities.forEachIndexed { index, activity ->
                    appendLine("${index + 1}. ${activity.javaClass.simpleName}")
                    appendLine("   状态: ${getActivityState(activity)}")
                    appendLine("   任务ID: ${activity.taskId}")
                    appendLine("   是否正在结束: ${activity.isFinishing}")
                }
            }
            appendLine()
            
            // 内存泄漏检测
            if (tracker is AppTrackV3) {
                appendLine("## 内存泄漏检测")
                val leaks = tracker.detectLeaks()
                if (leaks.isEmpty()) {
                    appendLine("未发现内存泄漏")
                } else {
                    appendLine("发现 ${leaks.size} 个潜在内存泄漏:")
                    leaks.forEach { leak ->
                        appendLine("- ${leak.componentType}: ${leak.componentName}")
                        appendLine("  泄漏时间: ${formatDuration(leak.leakTime)}")
                        appendLine("  预估大小: ${formatBytes(leak.retainedSize)}")
                        if (leak.suggestions.isNotEmpty()) {
                            appendLine("  建议:")
                            leak.suggestions.forEach { suggestion ->
                                appendLine("    * $suggestion")
                            }
                        }
                    }
                }
                appendLine()
            }
            
            // 最近的调试日志
            appendLine("## 最近的调试日志")
            val recentLogs = debugLogs.takeLast(20)
            if (recentLogs.isEmpty()) {
                appendLine("无调试日志")
            } else {
                recentLogs.forEach { log ->
                    appendLine("${dateFormat.format(Date(log.timestamp))} [${log.level}] ${log.category}: ${log.message}")
                    if (log.details.isNotEmpty()) {
                        log.details.forEach { (key, value) ->
                            appendLine("  $key: $value")
                        }
                    }
                }
            }
            
            appendLine("\n=== 报告结束 ===")
        }
    }
    
    /**
     * 导出性能指标
     */
    fun exportMetrics(): Map<String, Any> {
        val stats = tracker.getPerformanceStats()
        val activities = tracker.getActiveActivities()
        
        return mapOf(
            "timestamp" to System.currentTimeMillis(),
            "performance" to stats.toMap(),
            "activities" to activities.map { activity ->
                mapOf(
                    "name" to activity.javaClass.simpleName,
                    "state" to getActivityState(activity),
                    "taskId" to activity.taskId,
                    "isFinishing" to activity.isFinishing,
                    "hashCode" to activity.hashCode()
                )
            },
            "topActivity" to (tracker.getTopActivity()?.javaClass?.simpleName ?: "none"),
            "topFragment" to (tracker.getTopFragment()?.javaClass?.simpleName ?: "none"),
            "memoryLeaks" to if (tracker is AppTrackV3) {
                tracker.detectLeaks().map { leak ->
                    mapOf(
                        "type" to leak.componentType,
                        "name" to leak.componentName,
                        "leakTime" to leak.leakTime,
                        "retainedSize" to leak.retainedSize
                    )
                }
            } else emptyList()
        )
    }
    
    /**
     * 可视化Activity栈
     */
    fun visualizeStack(): String {
        return buildString {
            appendLine("Activity栈可视化:")
            appendLine("┌─────────────────────────────────┐")
            
            val activities = tracker.getActiveActivities()
            if (activities.isEmpty()) {
                appendLine("│           栈为空                │")
            } else {
                activities.forEachIndexed { index, activity ->
                    val name = activity.javaClass.simpleName
                    val state = getActivityState(activity)
                    val isTop = index == 0
                    val prefix = if (isTop) "► " else "  "
                    val line = "$prefix$name ($state)"
                    
                    appendLine("│ ${line.padEnd(31)} │")
                    
                    if (index < activities.size - 1) {
                        appendLine("│               ↓                 │")
                    }
                }
            }
            
            appendLine("└─────────────────────────────────┘")
        }
    }
    
    /**
     * 记录调试日志
     */
    fun log(level: String, category: String, message: String, details: Map<String, Any> = emptyMap()) {
        synchronized(debugLogs) {
            debugLogs.add(
                DebugLogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = level,
                    category = category,
                    message = message,
                    details = details
                )
            )
            
            // 保持日志数量在限制内
            if (debugLogs.size > maxLogEntries) {
                debugLogs.removeAt(0)
            }
        }
    }
    
    /**
     * 获取系统信息
     */
    fun getSystemInfo(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        
        return mapOf(
            "availableProcessors" to runtime.availableProcessors(),
            "maxMemory" to runtime.maxMemory(),
            "totalMemory" to runtime.totalMemory(),
            "freeMemory" to runtime.freeMemory(),
            "usedMemory" to (runtime.totalMemory() - runtime.freeMemory()),
            "javaVersion" to System.getProperty("java.version"),
            "androidVersion" to android.os.Build.VERSION.RELEASE,
            "apiLevel" to android.os.Build.VERSION.SDK_INT,
            "manufacturer" to android.os.Build.MANUFACTURER,
            "model" to android.os.Build.MODEL,
            "device" to android.os.Build.DEVICE
        )
    }
    
    /**
     * 执行健康检查
     */
    fun performHealthCheck(): HealthCheckResult {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val stats = tracker.getPerformanceStats()
        
        // 检查错误率
        val errorRate = stats.getErrorRate()
        if (errorRate > 0.1f) {
            issues.add("错误率过高: ${String.format("%.2f%%", errorRate * 100)}")
        } else if (errorRate > 0.05f) {
            warnings.add("错误率较高: ${String.format("%.2f%%", errorRate * 100)}")
        }
        
        // 检查内存使用
        val memoryRatio = stats.getMemoryUsageRatio()
        if (memoryRatio > 0.9f) {
            issues.add("内存使用率过高: ${String.format("%.2f%%", memoryRatio * 100)}")
        } else if (memoryRatio > 0.7f) {
            warnings.add("内存使用率较高: ${String.format("%.2f%%", memoryRatio * 100)}")
        }
        
        // 检查响应时间
        if (stats.avgResponseTime > 100) {
            issues.add("平均响应时间过长: ${stats.avgResponseTime}ms")
        } else if (stats.avgResponseTime > 50) {
            warnings.add("平均响应时间较长: ${stats.avgResponseTime}ms")
        }
        
        // 检查内存泄漏
        if (tracker is AppTrackV3) {
            val leaks = tracker.detectLeaks()
            if (leaks.size > 5) {
                issues.add("发现大量内存泄漏: ${leaks.size}个")
            } else if (leaks.isNotEmpty()) {
                warnings.add("发现内存泄漏: ${leaks.size}个")
            }
        }
        
        val status = when {
            issues.isNotEmpty() -> HealthStatus.CRITICAL
            warnings.isNotEmpty() -> HealthStatus.WARNING
            else -> HealthStatus.HEALTHY
        }
        
        return HealthCheckResult(
            status = status,
            issues = issues,
            warnings = warnings,
            checkTime = System.currentTimeMillis(),
            stats = stats
        )
    }
    
    /**
     * 清理调试日志
     */
    fun clearLogs() {
        synchronized(debugLogs) {
            debugLogs.clear()
        }
    }
    
    private fun getActivityState(activity: Activity): String {
        return when {
            activity.isFinishing -> "FINISHING"
            activity.isDestroyed -> "DESTROYED"
            !activity.isAlive() -> "DEAD"
            else -> "ALIVE"
        }
    }
    
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return String.format("%.2f %s", size, units[unitIndex])
    }
    
    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}天 ${hours % 24}小时"
            hours > 0 -> "${hours}小时 ${minutes % 60}分钟"
            minutes > 0 -> "${minutes}分钟 ${seconds % 60}秒"
            else -> "${seconds}秒"
        }
    }
}

/**
 * 健康检查结果
 */
data class HealthCheckResult(
    val status: HealthStatus,
    val issues: List<String>,
    val warnings: List<String>,
    val checkTime: Long,
    val stats: PerformanceStats
) {
    
    fun isHealthy(): Boolean = status == HealthStatus.HEALTHY
    
    fun hasIssues(): Boolean = issues.isNotEmpty()
    
    fun hasWarnings(): Boolean = warnings.isNotEmpty()
    
    fun getSummary(): String {
        return when (status) {
            HealthStatus.HEALTHY -> "系统运行正常"
            HealthStatus.WARNING -> "发现 ${warnings.size} 个警告"
            HealthStatus.CRITICAL -> "发现 ${issues.size} 个严重问题"
        }
    }
}

/**
 * 健康状态
 */
enum class HealthStatus {
    HEALTHY,    // 健康
    WARNING,    // 警告
    CRITICAL    // 严重
}

/**
 * 默认组件过滤器
 */
class DefaultComponentFilter : ComponentFilter {
    
    private val excludedActivities = setOf(
        "com.android.internal.app.ChooserActivity",
        "com.android.internal.app.ResolverActivity"
    )
    
    private val excludedFragments = setOf(
        "com.gyf.immersionbar.SupportRequestBarManagerFragment",
        "com.gyf.immersionbar.RequestBarManagerFragment",
        "androidx.fragment.app.DialogFragment"
    )
    
    override fun shouldTrackActivity(activity: Activity): Boolean {
        return !excludedActivities.contains(activity.javaClass.name)
    }
    
    override fun shouldTrackFragment(fragment: Fragment): Boolean {
        return !excludedFragments.contains(fragment.javaClass.name)
    }
    
    override fun shouldTrackService(service: android.app.Service): Boolean {
        return true // 默认追踪所有Service
    }
    
    override fun getPriority(component: Any): Int {
        return when (component) {
            is Activity -> 100
            is Fragment -> 50
            else -> 0
        }
    }
}

/**
 * 性能优化建议生成器
 */
class PerformanceOptimizer {
    
    fun generateOptimizationSuggestions(stats: PerformanceStats): List<String> {
        val suggestions = mutableListOf<String>()
        
        // 响应时间优化
        if (stats.avgResponseTime > 50) {
            suggestions.add("考虑启用无锁数据结构以提升响应速度")
            suggestions.add("增加批量处理大小以减少系统调用")
        }
        
        // 内存优化
        val memoryRatio = stats.getMemoryUsageRatio()
        if (memoryRatio > 0.7f) {
            suggestions.add("降低清理阈值以更频繁地回收内存")
            suggestions.add("减少栈大小限制")
            suggestions.add("启用自适应清理策略")
        }
        
        // 错误率优化
        val errorRate = stats.getErrorRate()
        if (errorRate > 0.05f) {
            suggestions.add("检查监听器实现，确保异常处理完善")
            suggestions.add("启用详细日志以定位错误原因")
        }
        
        // 并发优化
        if (stats.maxConcurrentOperations > 32) {
            suggestions.add("考虑限制最大并发操作数")
            suggestions.add("使用批量通知减少并发压力")
        }
        
        // 清理优化
        if (stats.avgCleanupTime > 100) {
            suggestions.add("优化清理策略，考虑分批清理")
            suggestions.add("调整清理阈值以避免大量积压")
        }
        
        return suggestions
    }
    
    fun generateOptimalConfig(stats: PerformanceStats): TrackerConfig {
        val memoryRatio = stats.getMemoryUsageRatio()
        val errorRate = stats.getErrorRate()
        val avgResponseTime = stats.avgResponseTime
        
        return when {
            // 高性能场景
            avgResponseTime < 10 && errorRate < 0.01f -> {
                TrackerConfig.createPerformanceOptimized()
            }
            // 内存敏感场景
            memoryRatio > 0.8f -> {
                TrackerConfig.createMemoryOptimized()
            }
            // 调试场景
            errorRate > 0.1f -> {
                TrackerConfig.createDebugMode()
            }
            // 默认场景
            else -> {
                TrackerConfig.createDefault()
            }
        }
    }
}