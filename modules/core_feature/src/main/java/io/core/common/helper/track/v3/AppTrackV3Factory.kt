package io.core.common.helper.track.v3

import android.app.Activity
import android.app.Application
import androidx.fragment.app.Fragment

/**
 * AppTrackV3 工厂类和使用示例
 * 
 * @author [Yuantris] - v3.0
 * @since 2025/8/10
 */
object AppTrackV3Factory {
    
    /**
     * 创建默认配置的追踪器
     */
    fun createDefault(): IAppTracker {
        return AppTrackV3
    }
    
    /**
     * 创建高性能配置的追踪器
     */
    fun createHighPerformance(): IAppTracker {
        val config = TrackerConfig.createPerformanceOptimized()
        AppTrackV3.updateConfig(config)
        return AppTrackV3
    }
    
    /**
     * 创建内存优化配置的追踪器
     */
    fun createMemoryOptimized(): IAppTracker {
        val config = TrackerConfig.createMemoryOptimized()
        AppTrackV3.updateConfig(config)
        return AppTrackV3
    }
    
    /**
     * 创建调试模式的追踪器
     */
    fun createDebugMode(): IAppTracker {
        val config = TrackerConfig.createDebugMode()
        AppTrackV3.updateConfig(config)
        return AppTrackV3
    }
    
    /**
     * 创建自定义配置的追踪器
     */
    fun createCustom(config: TrackerConfig): IAppTracker {
        AppTrackV3.updateConfig(config)
        return AppTrackV3
    }
}

/**
 * 简化的初始化助手
 */
object AppTrackV3Helper {
    
    /**
     * 快速初始化 - 默认配置
     */
    fun quickInit(application: Application) {
        val tracker = AppTrackV3Factory.createDefault()
        tracker.init(application)
        
        // 添加默认过滤器
        if (tracker is AppTrackV3) {
            tracker.addComponentFilter(DefaultComponentFilter())
        }
    }
    
    /**
     * 高性能初始化
     */
    fun initForPerformance(application: Application) {
        val tracker = AppTrackV3Factory.createHighPerformance()
        tracker.init(application)
        
        // 添加默认过滤器
        if (tracker is AppTrackV3) {
            tracker.addComponentFilter(DefaultComponentFilter())
        }
    }
    
    /**
     * 调试模式初始化
     */
    fun initForDebug(application: Application) {
        val tracker = AppTrackV3Factory.createDebugMode()
        tracker.init(application)
        
        // 添加默认过滤器和调试钩子
        if (tracker is AppTrackV3) {
            tracker.addComponentFilter(DefaultComponentFilter())
            tracker.registerLifecycleHook("debug", DebugLifecycleHook())
        }
    }
    
    /**
     * 获取当前追踪器实例
     */
    fun getInstance(): IAppTracker = AppTrackV3
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean {
        return try {
            AppTrackV3.getActiveActivities()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }
}

/**
 * 调试生命周期钩子
 */
class DebugLifecycleHook : LifecycleHook {
    
    override fun beforeActivityCreate(activity: Activity): Boolean {
        println("🚀 Activity即将创建: ${activity::class.simpleName}")
        return true
    }
    
    override fun afterActivityCreate(activity: Activity) {
        println("✅ Activity已创建: ${activity::class.simpleName}")
    }
    
    override fun beforeActivityDestroy(activity: Activity): Boolean {
        println("🗑️ Activity即将销毁: ${activity::class.simpleName}")
        return true
    }
    
    override fun afterActivityDestroy(activity: Activity) {
        println("💀 Activity已销毁: ${activity::class.simpleName}")
    }
    
    override fun beforeFragmentCreate(fragment: Fragment): Boolean {
        println("🧩 Fragment即将创建: ${fragment::class.simpleName}")
        return true
    }
    
    override fun afterFragmentCreate(fragment: Fragment) {
        println("✨ Fragment已创建: ${fragment::class.simpleName}")
    }
    
    override fun beforeFragmentDestroy(fragment: Fragment): Boolean {
        println("🔥 Fragment即将销毁: ${fragment::class.simpleName}")
        return true
    }
    
    override fun afterFragmentDestroy(fragment: Fragment) {
        println("💨 Fragment已销毁: ${fragment::class.simpleName}")
    }
}

/**
 * 使用示例
 */
class AppTrackV3Examples {
    
    /**
     * 基础使用示例
     */
    fun basicUsage(application: Application) {
        // 1. 初始化
        AppTrackV3Helper.quickInit(application)
        
        // 2. 注册监听器
        val tracker = AppTrackV3Helper.getInstance()
        tracker.registerLifecycleListener("main", object : LifecycleListener {
            override fun onActivityCreated(activity: Activity) {
                println("Activity创建: ${activity::class.simpleName}")
            }
            
            override fun onActivityDestroyed(activity: Activity) {
                println("Activity销毁: ${activity::class.simpleName}")
            }
            
            override fun onAppForeground() {
                println("应用进入前台")
            }
            
            override fun onAppBackground() {
                println("应用进入后台")
            }
        })
        
        // 3. 获取信息
        val topActivity = tracker.getTopActivity()
        val activeActivities = tracker.getActiveActivities()
        val stats = tracker.getPerformanceStats()
        
        println("栈顶Activity: ${topActivity?.javaClass?.simpleName}")
        println("活跃Activity数量: ${activeActivities.size}")
        println("性能统计: $stats")
    }
    
    /**
     * 高级使用示例
     */
    fun advancedUsage(application: Application) {
        // 1. 自定义配置
        val customConfig = TrackerConfig(
            maxStackSize = 50,
            enableLockFree = true,
            enableDetailedMetrics = true,
            enableLeakDetection = true,
            enableDebugMode = true
        )
        
        val tracker = AppTrackV3Factory.createCustom(customConfig)
        tracker.init(application)
        
        // 2. 添加自定义过滤器
        if (tracker is AppTrackV3) {
            tracker.addComponentFilter(object : ComponentFilter {
                override fun shouldTrackActivity(activity: Activity): Boolean {
                    // 只追踪主要的Activity
                    return !activity.javaClass.name.contains("Test")
                }
                
                override fun shouldTrackFragment(fragment: Fragment): Boolean {
                    // 过滤掉Dialog Fragment
                    return !fragment.javaClass.name.contains("Dialog")
                }
                
                override fun shouldTrackService(service: android.app.Service): Boolean {
                    return true
                }
                
                override fun getPriority(component: Any): Int {
                    return when (component) {
                        is Activity -> 100
                        is Fragment -> 50
                        else -> 0
                    }
                }
            })
            
            // 3. 添加生命周期钩子
            tracker.registerLifecycleHook("analytics", object : LifecycleHook {
                override fun afterActivityCreate(activity: Activity) {
                    // 发送分析事件
                    sendAnalyticsEvent("activity_created", activity.javaClass.simpleName)
                }
                
                override fun afterActivityDestroy(activity: Activity) {
                    // 发送分析事件
                    sendAnalyticsEvent("activity_destroyed", activity.javaClass.simpleName)
                }
            })
        }
        
        // 4. 定期检查性能和内存泄漏
        scheduleHealthCheck(tracker)
    }
    
    /**
     * 调试和诊断示例
     */
    fun debuggingExample(application: Application) {
        // 1. 启用调试模式
        AppTrackV3Helper.initForDebug(application)
        val tracker = AppTrackV3Helper.getInstance()
        
        // 2. 获取调试器
        if (tracker is AppTrackV3) {
            val debugger = tracker.getDebugger()
            
            // 生成调试报告
            val report = debugger?.generateReport()
            println("调试报告:\n$report")
            
            // 导出性能指标
            val metrics = debugger?.exportMetrics()
            println("性能指标: $metrics")
            
            // 可视化栈
            val stackVisualization = debugger?.visualizeStack()
            println("栈可视化:\n$stackVisualization")
            
            // 健康检查
            val healthCheck = debugger?.performHealthCheck()
            println("健康检查: ${healthCheck?.getSummary()}")
            
            // 检测内存泄漏
            val leaks = tracker.detectLeaks()
            if (leaks.isNotEmpty()) {
                println("发现内存泄漏:")
                leaks.forEach { leak ->
                    println("- ${leak.componentType}: ${leak.componentName}")
                    println("  建议: ${leak.suggestions.joinToString(", ")}")
                }
            }
        }
    }
    
    /**
     * 性能优化示例
     */
    fun performanceOptimizationExample(application: Application) {
        // 1. 初始化高性能配置
        AppTrackV3Helper.initForPerformance(application)
        val tracker = AppTrackV3Helper.getInstance()
        
        // 2. 定期监控性能
        val optimizer = PerformanceOptimizer()
        
        // 获取当前性能统计
        val stats = tracker.getPerformanceStats()
        
        // 生成优化建议
        val suggestions = optimizer.generateOptimizationSuggestions(stats)
        println("优化建议:")
        suggestions.forEach { suggestion ->
            println("- $suggestion")
        }
        
        // 生成最优配置
        val optimalConfig = optimizer.generateOptimalConfig(stats)
        tracker.updateConfig(optimalConfig)
        
        println("已应用最优配置: ${optimalConfig.configVersion}")
    }
    
    private fun sendAnalyticsEvent(event: String, data: String) {
        // 模拟发送分析事件
        println("Analytics: $event -> $data")
    }
    
    private fun scheduleHealthCheck(tracker: IAppTracker) {
        // 模拟定期健康检查
        if (tracker is AppTrackV3) {
            val debugger = tracker.getDebugger()
            val healthCheck = debugger?.performHealthCheck()
            
            when (healthCheck?.status) {
                HealthStatus.CRITICAL -> {
                    println("⚠️ 系统状态严重: ${healthCheck.getSummary()}")
                    // 执行紧急清理或重启
                    tracker.forceCleanup()
                }
                HealthStatus.WARNING -> {
                    println("⚡ 系统状态警告: ${healthCheck.getSummary()}")
                    // 执行优化措施
                }
                HealthStatus.HEALTHY -> {
                    println("✅ 系统状态良好")
                }
                null -> {
                    println("❓ 无法获取系统状态")
                }
            }
        }
    }
}

/**
 * 扩展函数，简化使用
 */

/**
 * Application扩展函数，快速初始化AppTrackV3
 */
fun Application.initAppTrackV3(config: TrackerConfig = TrackerConfig.createDefault()) {
    val tracker = AppTrackV3Factory.createCustom(config)
    tracker.init(this)
}

/**
 * 获取当前栈顶Activity
 */
fun getTopActivity(): Activity? {
    return if (AppTrackV3Helper.isInitialized()) {
        AppTrackV3Helper.getInstance().getTopActivity()
    } else null
}

/**
 * 获取当前栈顶Fragment
 */
fun getTopFragment(): Fragment? {
    return if (AppTrackV3Helper.isInitialized()) {
        AppTrackV3Helper.getInstance().getTopFragment()
    } else null
}

/**
 * 获取所有活跃Activity
 */
fun getActiveActivities(): List<Activity> {
    return if (AppTrackV3Helper.isInitialized()) {
        AppTrackV3Helper.getInstance().getActiveActivities()
    } else emptyList()
}

/**
 * 快速注册生命周期监听器
 */
fun registerLifecycleListener(
    tag: String = "default",
    onActivityCreated: ((Activity) -> Unit)? = null,
    onActivityDestroyed: ((Activity) -> Unit)? = null,
    onAppForeground: (() -> Unit)? = null,
    onAppBackground: (() -> Unit)? = null
) {
    if (!AppTrackV3Helper.isInitialized()) return
    
    val listener = object : LifecycleListener {
        override fun onActivityCreated(activity: Activity) {
            onActivityCreated?.invoke(activity)
        }
        
        override fun onActivityDestroyed(activity: Activity) {
            onActivityDestroyed?.invoke(activity)
        }
        
        override fun onAppForeground() {
            onAppForeground?.invoke()
        }
        
        override fun onAppBackground() {
            onAppBackground?.invoke()
        }
    }
    
    AppTrackV3Helper.getInstance().registerLifecycleListener(tag, listener)
}