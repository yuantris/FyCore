package io.core.common.helper.track.v3

import android.app.Activity
import android.app.Application
import android.app.Service
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.core.common.helper.ReflectHelper
import io.core.common.util.extensions.cool.HandlerGT
import io.core.common.util.extensions.cool.isMainThread
import io.core.common.util.extensions.ui.isAlive
import io.core.common.util.log.LogPure
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 应用程序全局生命周期追踪管理器 - V3 重构版
 *
 * 重构特性：
 * - 模块化架构设计，职责分离
 * - 高性能无锁数据结构
 * - 智能内存管理和泄漏检测
 * - 详细的性能监控和统计
 * - 可配置的清理策略
 * - 插件化扩展支持
 * - 完善的错误处理和恢复机制
 *
 * @author [Yuantris] - v3.0 重构版
 * @since 2025/8/10
 */
object AppTrackV3 : IAppTracker, Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private const val TAG = "AppTrackV3"
    
    // 核心组件
    private val config = AtomicReference(TrackerConfig.createDefault())
    private val initialized = AtomicBoolean(false)
    private val destroyed = AtomicBoolean(false)
    
    // 管理器组件
    private lateinit var activityStackManager: IStackManager<Activity>
    private lateinit var fragmentStackManager: IStackManager<Fragment>
    private lateinit var serviceStackManager: IStackManager<Service>
    private lateinit var lifecycleListenerManager: IListenerManager<LifecycleListener>
    private lateinit var performanceMonitor: IPerformanceMonitor
    private lateinit var memoryManager: IMemoryManager
    
    // 扩展组件
    private val lifecycleHooks = ConcurrentHashMap<String, LifecycleHook>()
    private val componentFilters = CopyOnWriteArrayList<ComponentFilter>()
    private val fragmentCallbacks = ConcurrentHashMap<FragmentActivity, FragmentManager.FragmentLifecycleCallbacks>()
    
    // 缓存和优化
    private val cachedApplication = AtomicReference<Application?>(null)
    private val applicationLock = ReentrantReadWriteLock()
    
    // 定时任务
    private var cleanupExecutor: ScheduledExecutorService? = null
    private var adaptiveCleanupStrategy: AdaptiveCleanupStrategy? = null
    
    // 调试和诊断
    private var debugger: AppTrackerDebugger? = null
    
    override fun init(application: Application) {
        if (!initialized.compareAndSet(false, true)) {
            LogPure.w(TAG, "AppTrackV3 already initialized")
            return
        }
        
        try {
            val currentConfig = config.get()
            
            // 初始化核心组件
            initializeManagers(currentConfig)
            
            // 注册生命周期回调
            application.registerActivityLifecycleCallbacks(this)
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            
            // 缓存Application实例
            applicationLock.write {
                cachedApplication.set(application)
            }
            
            // 启动自适应清理
            if (currentConfig.enableAdaptiveCleanup) {
                startAdaptiveCleanup()
            }
            
            // 初始化调试器
            if (currentConfig.enableDebugMode) {
                debugger = AppTrackerDebugger(this)
            }
            
            // 记录初始化性能
            performanceMonitor.recordOperation("init", 0, true)
            
            LogPure.i(TAG, "AppTrackV3 initialized successfully with config: ${currentConfig.configVersion}")
            
        } catch (e: Exception) {
            initialized.set(false)
            LogPure.e(TAG, "Failed to initialize AppTrackV3: ${e.message}")
            throw e
        }
    }
    
    private fun initializeManagers(config: TrackerConfig) {
        // 初始化栈管理器
        activityStackManager = if (config.enableLockFree) {
            LockFreeRingBufferStackManager(config.ringBufferSize, config.cleanupThreshold)
        } else {
            LockBasedStackManager(config.maxStackSize, config.cleanupThreshold)
        }
        
        fragmentStackManager = if (config.enableLockFree) {
            LockFreeRingBufferStackManager(config.ringBufferSize, config.cleanupThreshold)
        } else {
            LockBasedStackManager(config.maxStackSize, config.cleanupThreshold)
        }
        
        serviceStackManager = if (config.enableLockFree) {
            LockFreeRingBufferStackManager(config.ringBufferSize, config.cleanupThreshold)
        } else {
            LockBasedStackManager(config.maxStackSize, config.cleanupThreshold)
        }
        
        // 初始化监听器管理器
        lifecycleListenerManager = if (config.batchSize > 1) {
            BatchNotificationListenerManager(config.batchSize, config.batchFlushInterval)
        } else {
            HighPerformanceListenerManager()
        }
        
        // 初始化性能监控器
        performanceMonitor = HighPerformanceMonitor()
        
        // 初始化内存管理器
        memoryManager = SmartMemoryManager(config)
        
        // 初始化自适应清理策略
        adaptiveCleanupStrategy = AdaptiveCleanupStrategy(memoryManager, performanceMonitor)
    }
    
    override fun registerLifecycleListener(tag: String, listener: LifecycleListener) {
        checkInitialized()
        lifecycleListenerManager.register(tag, listener)
        LogPure.d(TAG, "Registered lifecycle listener: $tag")
    }
    
    override fun unregisterLifecycleListener(tag: String) {
        checkInitialized()
        val removed = lifecycleListenerManager.unregister(tag)
        LogPure.d(TAG, "Unregistered lifecycle listener: $tag, success: $removed")
    }
    
    override fun getTopActivity(): Activity? {
        checkInitialized()
        return activityStackManager.getTop()?.takeIf { it.isAlive() }
    }
    
    override fun getTopFragment(): Fragment? {
        checkInitialized()
        val topActivity = getTopActivity()
        return fragmentStackManager.getAll()
            .firstOrNull { fragment ->
                fragment.isAdded && !fragment.isDetached && fragment.activity == topActivity
            }
    }
    
    override fun getActiveActivities(): List<Activity> {
        checkInitialized()
        return activityStackManager.getAll().filter { it.isAlive() }
    }
    
    override fun getPerformanceStats(): PerformanceStats {
        checkInitialized()
        val detailedStats = performanceMonitor.getStats()
        val memoryStats = memoryManager.getMemoryStats()
        
        return PerformanceStats(
            activityTransitions = detailedStats.totalOperations,
            fragmentTransitions = detailedStats.totalOperations,
            serviceTransitions = detailedStats.totalOperations,
            listenerNotifications = detailedStats.totalOperations,
            cleanupOperations = memoryStats.cleanupCount,
            totalCleanupTime = detailedStats.avgResponseTime * memoryStats.cleanupCount,
            avgCleanupTime = detailedStats.avgResponseTime,
            memoryReclaimed = 0L,
            avgResponseTime = detailedStats.avgResponseTime,
            maxResponseTime = detailedStats.maxResponseTime,
            minResponseTime = detailedStats.minResponseTime,
            errorCount = detailedStats.failedOperations,
            currentMemoryUsage = memoryStats.totalMemoryUsage,
            maxMemoryUsage = detailedStats.maxMemoryUsage,
            gcCount = detailedStats.gcCount,
            concurrentOperations = detailedStats.currentConcurrency,
            maxConcurrentOperations = detailedStats.maxConcurrency,
            lockContentions = 0L,
            startTime = System.currentTimeMillis() - detailedStats.uptime,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    override fun updateConfig(config: TrackerConfig) {
        checkInitialized()
        
        val errors = config.validate()
        if (errors.isNotEmpty()) {
            LogPure.e(TAG, "Invalid config: ${errors.joinToString(", ")}")
            return
        }
        
        val oldConfig = this.config.getAndSet(config)
        LogPure.i(TAG, "Config updated from ${oldConfig.configVersion} to ${config.configVersion}")
        
        // 重新配置组件
        memoryManager.setMemoryThreshold(config.maxMemoryUsage)
        memoryManager.enableLeakDetection(config.enableLeakDetection)
        
        // 重启自适应清理
        if (config.enableAdaptiveCleanup != oldConfig.enableAdaptiveCleanup) {
            if (config.enableAdaptiveCleanup) {
                startAdaptiveCleanup()
            } else {
                stopAdaptiveCleanup()
            }
        }
    }
    
    // region Activity生命周期回调
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (destroyed.get()) return
        
        val timer = performanceMonitor.startTimer("activity_created")
        try {
            // 检查过滤器
            if (!shouldTrackActivity(activity)) {
                LogPure.d(TAG, "Activity filtered: ${activity::class.simpleName}")
                return
            }
            
            // 执行前置钩子
            if (!executeBeforeHooks { it.beforeActivityCreate(activity) }) {
                return
            }
            
            // 添加到栈管理器
            activityStackManager.add(activity)
            memoryManager.trackObject(activity, "Activity")
            
            // 设置Fragment追踪
            if (activity is FragmentActivity) {
                setupFragmentTracking(activity)
            }
            
            // 通知监听器
            notifyLifecycleListeners { it.onActivityCreated(activity) }
            
            // 执行后置钩子
            executeAfterHooks { it.afterActivityCreate(activity) }
            
            LogPure.d(TAG, "Activity created: ${activity::class.simpleName}")
            
        } catch (e: Exception) {
            LogPure.e(TAG, "Error in onActivityCreated: ${e.message}")
            timer.stopAndRecord(false)
            return
        }
        
        timer.stopAndRecord(true)
    }
    
    override fun onActivityResumed(activity: Activity) {
        if (destroyed.get()) return
        
        val timer = performanceMonitor.startTimer("activity_resumed")
        try {
            if (!shouldTrackActivity(activity)) return
            
            // 移至栈顶
            activityStackManager.remove(activity)
            activityStackManager.add(activity)
            
            // 通知监听器
            if (!activity.isChangingConfigurations) {
                notifyLifecycleListeners { it.onActivityResumed(activity) }
            }
            
            LogPure.d(TAG, "Activity resumed: ${activity::class.simpleName}")
            
        } catch (e: Exception) {
            LogPure.e(TAG, "Error in onActivityResumed: ${e.message}")
            timer.stopAndRecord(false)
            return
        }
        
        timer.stopAndRecord(true)
    }
    
    override fun onActivityPaused(activity: Activity) {
        if (destroyed.get()) return
        
        val timer = performanceMonitor.startTimer("activity_paused")
        try {
            if (!shouldTrackActivity(activity)) return
            
            // 通知监听器
            if (!activity.isChangingConfigurations) {
                notifyLifecycleListeners { it.onActivityPaused(activity) }
            }
            
            LogPure.d(TAG, "Activity paused: ${activity::class.simpleName}")
            
        } catch (e: Exception) {
            LogPure.e(TAG, "Error in onActivityPaused: ${e.message}")
            timer.stopAndRecord(false)
            return
        }
        
        timer.stopAndRecord(true)
    }
    
    override fun onActivityDestroyed(activity: Activity) {
        if (destroyed.get()) return
        
        val timer = performanceMonitor.startTimer("activity_destroyed")
        try {
            if (!shouldTrackActivity(activity)) return
            
            // 执行前置钩子
            if (!executeBeforeHooks { it.beforeActivityDestroy(activity) }) {
                return
            }
            
            // 从栈中移除
            activityStackManager.removeAll { ref ->
                ref == null || ref == activity || !ref.isAlive()
            }
            
            // 停止追踪
            memoryManager.untrackObject(activity)
            
            // 清理Fragment追踪
            if (activity is FragmentActivity) {
                cleanupFragmentTracking(activity)
            }
            
            // 通知监听器
            notifyLifecycleListeners { it.onActivityDestroyed(activity) }
            
            // 执行后置钩子
            executeAfterHooks { it.afterActivityDestroy(activity) }
            
            LogPure.d(TAG, "Activity destroyed: ${activity::class.simpleName}")
            
        } catch (e: Exception) {
            LogPure.e(TAG, "Error in onActivityDestroyed: ${e.message}")
            timer.stopAndRecord(false)
            return
        }
        
        timer.stopAndRecord(true)
    }
    
    override fun onStart(owner: LifecycleOwner) {
        if (destroyed.get()) return
        
        val timer = performanceMonitor.startTimer("app_foreground")
        try {
            notifyLifecycleListeners { it.onAppForeground() }
            LogPure.d(TAG, "App entered foreground")
        } catch (e: Exception) {
            LogPure.e(TAG, "Error in onStart: ${e.message}")
            timer.stopAndRecord(false)
            return
        }
        timer.stopAndRecord(true)
    }
    
    override fun onStop(owner: LifecycleOwner) {
        if (destroyed.get()) return
        
        val timer = performanceMonitor.startTimer("app_background")
        try {
            notifyLifecycleListeners { it.onAppBackground() }
            LogPure.d(TAG, "App entered background")
        } catch (e: Exception) {
            LogPure.e(TAG, "Error in onStop: ${e.message}")
            timer.stopAndRecord(false)
            return
        }
        timer.stopAndRecord(true)
    }
    // endregion
    
    // region Fragment追踪
    private fun setupFragmentTracking(activity: FragmentActivity) {
        val callback = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
                handleFragmentEvent(f, "created")
            }
            
            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                handleFragmentEvent(f, "resumed")
            }
            
            override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                handleFragmentEvent(f, "paused")
            }
            
            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                handleFragmentEvent(f, "destroyed")
            }
        }
        
        fragmentCallbacks[activity] = callback
        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(callback, true)
    }
    
    private fun handleFragmentEvent(fragment: Fragment, event: String) {
        if (destroyed.get()) return
        
        val timer = performanceMonitor.startTimer("fragment_$event")
        try {
            if (!shouldTrackFragment(fragment)) {
                LogPure.d(TAG, "Fragment filtered: ${fragment::class.simpleName}")
                return
            }
            
            when (event) {
                "created" -> {
                    if (executeBeforeHooks { it.beforeFragmentCreate(fragment) }) {
                        fragmentStackManager.add(fragment)
                        memoryManager.trackObject(fragment, "Fragment")
                        notifyLifecycleListeners { it.onFragmentCreated(fragment) }
                        executeAfterHooks { it.afterFragmentCreate(fragment) }
                    }
                }
                "resumed" -> {
                    fragmentStackManager.remove(fragment)
                    fragmentStackManager.add(fragment)
                    notifyLifecycleListeners { it.onFragmentResumed(fragment) }
                }
                "paused" -> {
                    notifyLifecycleListeners { it.onFragmentPaused(fragment) }
                }
                "destroyed" -> {
                    if (executeBeforeHooks { it.beforeFragmentDestroy(fragment) }) {
                        fragmentStackManager.remove(fragment)
                        memoryManager.untrackObject(fragment)
                        notifyLifecycleListeners { it.onFragmentDestroyed(fragment) }
                        executeAfterHooks { it.afterFragmentDestroy(fragment) }
                    }
                }
            }
            
            LogPure.d(TAG, "Fragment $event: ${fragment::class.simpleName}")
            
        } catch (e: Exception) {
            LogPure.e(TAG, "Error in handleFragmentEvent($event): ${e.message}")
            timer.stopAndRecord(false)
            return
        }
        
        timer.stopAndRecord(true)
    }
    
    private fun cleanupFragmentTracking(activity: FragmentActivity) {
        fragmentCallbacks.remove(activity)?.let { callback ->
            try {
                activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(callback)
            } catch (e: Exception) {
                LogPure.w(TAG, "Failed to unregister fragment callback: ${e.message}")
            }
        }
    }
    // endregion
    
    // region Service追踪扩展
    fun onServiceCreate(service: Service) {
        if (destroyed.get()) return
        
        val timer = performanceMonitor.startTimer("service_created")
        try {
            if (shouldTrackService(service)) {
                serviceStackManager.add(service)
                memoryManager.trackObject(service, "Service")
                LogPure.d(TAG, "Service created: ${service::class.simpleName}")
            }
        } catch (e: Exception) {
            LogPure.e(TAG, "Error in onServiceCreate: ${e.message}")
            timer.stopAndRecord(false)
            return
        }
        timer.stopAndRecord(true)
    }
    
    fun onServiceDestroy(service: Service) {
        if (destroyed.get()) return
        
        val timer = performanceMonitor.startTimer("service_destroyed")
        try {
            if (shouldTrackService(service)) {
                serviceStackManager.remove(service)
                memoryManager.untrackObject(service)
                LogPure.d(TAG, "Service destroyed: ${service::class.simpleName}")
            }
        } catch (e: Exception) {
            LogPure.e(TAG, "Error in onServiceDestroy: ${e.message}")
            timer.stopAndRecord(false)
            return
        }
        timer.stopAndRecord(true)
    }
    // endregion
    
    // region 扩展API
    fun registerLifecycleHook(tag: String, hook: LifecycleHook) {
        lifecycleHooks[tag] = hook
        LogPure.d(TAG, "Registered lifecycle hook: $tag")
    }
    
    fun unregisterLifecycleHook(tag: String) {
        lifecycleHooks.remove(tag)
        LogPure.d(TAG, "Unregistered lifecycle hook: $tag")
    }
    
    fun addComponentFilter(filter: ComponentFilter) {
        componentFilters.add(filter)
        LogPure.d(TAG, "Added component filter: ${filter::class.simpleName}")
    }
    
    fun removeComponentFilter(filter: ComponentFilter) {
        componentFilters.remove(filter)
        LogPure.d(TAG, "Removed component filter: ${filter::class.simpleName}")
    }
    
    fun getDebugger(): AppTrackerDebugger? = debugger
    
    fun forceCleanup() {
        checkInitialized()
        memoryManager.forceCleanup()
        LogPure.d(TAG, "Force cleanup executed")
    }
    
    fun detectLeaks(): List<LeakInfo> {
        checkInitialized()
        return memoryManager.detectLeaks()
    }
    
    fun getApplicationReflect(): Application? {
        return applicationLock.read { cachedApplication.get() } ?: run {
            val app = getApplicationViaReflection()
            applicationLock.write { cachedApplication.set(app) }
            app
        }
    }
    // endregion
    
    override fun destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            LogPure.w(TAG, "AppTrackV3 already destroyed")
            return
        }
        
        try {
            // 停止定时任务
            stopAdaptiveCleanup()
            
            // 清理管理器
            activityStackManager.clear()
            fragmentStackManager.clear()
            serviceStackManager.clear()
            lifecycleListenerManager.clear()
            memoryManager.destroy()
            
            // 清理扩展组件
            lifecycleHooks.clear()
            componentFilters.clear()
            fragmentCallbacks.clear()
            
            // 清理缓存
            applicationLock.write { cachedApplication.set(null) }
            
            // 重置状态
            initialized.set(false)
            
            LogPure.i(TAG, "AppTrackV3 destroyed successfully")
            
        } catch (e: Exception) {
            LogPure.e(TAG, "Error during destroy: ${e.message}")
        }
    }
    
    // region 私有方法
    private fun checkInitialized() {
        if (!initialized.get() || destroyed.get()) {
            throw IllegalStateException("AppTrackV3 not initialized or already destroyed")
        }
    }
    
    private fun shouldTrackActivity(activity: Activity): Boolean {
        return componentFilters.all { it.shouldTrackActivity(activity) }
    }
    
    private fun shouldTrackFragment(fragment: Fragment): Boolean {
        return componentFilters.all { it.shouldTrackFragment(fragment) }
    }
    
    private fun shouldTrackService(service: Service): Boolean {
        return componentFilters.all { it.shouldTrackService(service) }
    }
    
    private fun executeBeforeHooks(action: (LifecycleHook) -> Boolean): Boolean {
        return lifecycleHooks.values.all { hook ->
            try {
                action(hook)
            } catch (e: Exception) {
                LogPure.e(TAG, "Error in before hook: ${e.message}")
                false
            }
        }
    }
    
    private fun executeAfterHooks(action: (LifecycleHook) -> Unit) {
        lifecycleHooks.values.forEach { hook ->
            try {
                action(hook)
            } catch (e: Exception) {
                LogPure.e(TAG, "Error in after hook: ${e.message}")
            }
        }
    }
    
    private fun notifyLifecycleListeners(action: (LifecycleListener) -> Unit) {
        try {
            if (isMainThread()) {
                lifecycleListenerManager.notifyAllSafe(action)
            } else {
                HandlerGT.main.post {
                    lifecycleListenerManager.notifyAllSafe(action)
                }
            }
        } catch (e: Exception) {
            LogPure.e(TAG, "Error notifying lifecycle listeners: ${e.message}")
        }
    }
    
    private fun startAdaptiveCleanup() {
        stopAdaptiveCleanup()
        
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "AppTrack-AdaptiveCleanup").apply { isDaemon = true }
        }.apply {
            scheduleWithFixedDelay({
                try {
                    adaptiveCleanupStrategy?.performAdaptiveCleanup()
                } catch (e: Exception) {
                    LogPure.e(TAG, "Adaptive cleanup failed: ${e.message}")
                }
            }, config.get().cleanupInterval, config.get().cleanupInterval, TimeUnit.MILLISECONDS)
        }
    }
    
    private fun stopAdaptiveCleanup() {
        cleanupExecutor?.shutdown()
        cleanupExecutor = null
    }
    
    private fun getApplicationViaReflection(): Application? {
        return try {
            val thread = getActivityThread() ?: return null
            ReflectHelper.on("android.app.ActivityThread")
                .getMethod("getApplication")
                .invoke(thread) as? Application
        } catch (e: Exception) {
            LogPure.e(TAG, "Failed to get Application via reflection: ${e.message}")
            null
        }
    }
    
    private fun getActivityThread(): Any? {
        return try {
            ReflectHelper.on("android.app.ActivityThread")
                .getField("sCurrentActivityThread")
        } catch (e: Exception) {
            try {
                ReflectHelper.on("android.app.ActivityThread")
                    .chainInvoke("currentActivityThread")
                    .get()
            } catch (e2: Exception) {
                LogPure.e(TAG, "Failed to get ActivityThread: ${e2.message}")
                null
            }
        }
    }
    // endregion
    
    // region 默认实现
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    // endregion
}