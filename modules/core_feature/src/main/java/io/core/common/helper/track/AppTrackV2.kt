package io.core.common.helper.track

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.app.Service
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.material.snackbar.Snackbar
import io.core.common.helper.ReflectHelper
import io.core.common.helper.track.activity.TimeTracker
import io.core.common.helper.track.ui.DialogOperation
import io.core.common.helper.track.ui.SafeUIManager
import io.core.common.helper.track.ui.SnackbarOperation
import io.core.common.helper.track.ui.ToastOperation
import io.core.common.helper.track.ui.UIOperation
import io.core.common.util.extensions.cool.HandlerGT
import io.core.common.util.extensions.cool.isMainThread
import io.core.common.util.extensions.ui.isAlive
import io.core.common.util.log.LogPure
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 应用程序全局生命周期追踪管理器 - 优化版
 *
 * 优化特性：
 * - 智能内存管理（ReferenceQueue + 定时清理）
 * - 高性能并发控制（读写锁 + 无锁数据结构）
 * - 模块化架构设计（分离关注点）
 * - 完善的性能监控和异常处理
 * - 可配置的清理策略和阈值
 *
 * 线程安全：
 * - 使用读写锁优化并发读性能
 * - 原子操作保证计数器线程安全
 * - 无锁算法优化热点路径
 *
 * @author [Yuantris] - 优化版 v2.1
 * @since 2025/8/10
 */
object AppTrackV2 : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private const val TAG = "AppTrackV2"
    private const val MAX_STACK_SIZE = 100
    private const val CLEANUP_THRESHOLD = 0.3f // 30%无效引用时触发清理
    private const val CLEANUP_INTERVAL_MS = 30_000L // 30秒定时清理
    private const val FRAGMENT_TIME_WINDOW_MS = 100L // Fragment时间窗口

    // region 性能监控
    private val performanceStats = PerformanceStats()
    private val safeUIManager = SafeUIManager()
    
    private class PerformanceStats {
        val activityTransitions = AtomicLong(0)
        val fragmentTransitions = AtomicLong(0)
        val cleanupOperations = AtomicLong(0)
        val listenerNotifications = AtomicLong(0)
        val totalCleanupTime = AtomicLong(0)
        
        fun getStats(): Map<String, Long> = mapOf(
            "activityTransitions" to activityTransitions.get(),
            "fragmentTransitions" to fragmentTransitions.get(),
            "cleanupOperations" to cleanupOperations.get(),
            "listenerNotifications" to listenerNotifications.get(),
            "avgCleanupTime" to if (cleanupOperations.get() > 0) 
                totalCleanupTime.get() / cleanupOperations.get() else 0
        )
        
        fun reset() {
            activityTransitions.set(0)
            fragmentTransitions.set(0)
            cleanupOperations.set(0)
            listenerNotifications.set(0)
            totalCleanupTime.set(0)
        }
    }
    
    @JvmStatic
    fun getPerformanceStats(): Map<String, Long> = performanceStats.getStats()
    
    @JvmStatic
    fun resetPerformanceStats() = performanceStats.reset()
    // endregion

    // region 智能内存管理
    private class SmartWeakReference<T>(referent: T, queue: ReferenceQueue<T>) : WeakReference<T>(referent, queue) {
        val timestamp = System.currentTimeMillis()
    }
    
    private class StackManager<T> {
        private val stack = ConcurrentLinkedDeque<SmartWeakReference<T>>()
        private val referenceQueue = ReferenceQueue<T>()
        private val lock = ReentrantReadWriteLock()
        private val invalidCount = AtomicInteger(0)
        
        fun add(item: T) {
            lock.write {
                stack.addFirst(SmartWeakReference(item, referenceQueue))
                if (stack.size > MAX_STACK_SIZE) {
                    stack.removeLast()
                }
            }
            processReferenceQueue()
        }
        
        fun remove(item: T) {
            lock.write {
                stack.removeAll { it.get() == item }
            }
        }
        
        fun removeAll(predicate: (T?) -> Boolean) {
            lock.write {
                stack.removeAll { ref -> predicate(ref.get()) }
            }
        }
        
        fun getTop(): T? {
            cleanupIfNeeded()
            return lock.read {
                stack.firstOrNull { it.get() != null }?.get()
            }
        }
        
        fun getAll(): List<T> {
            cleanupIfNeeded()
            return lock.read {
                stack.mapNotNull { it.get() }
            }
        }
        
        fun size(): Int = lock.read { stack.size }
        
        private fun processReferenceQueue() {
            var processed = 0
            while (referenceQueue.poll() != null) {
                invalidCount.incrementAndGet()
                processed++
            }
            if (processed > 0) {
                performanceStats.cleanupOperations.addAndGet(processed.toLong())
            }
        }
        
        private fun cleanupIfNeeded() {
            val totalSize = stack.size
            if (totalSize == 0) return
            
            processReferenceQueue()
            val invalidRatio = invalidCount.get().toFloat() / totalSize
            
            if (invalidRatio > CLEANUP_THRESHOLD) {
                performCleanup()
            }
        }
        
        private fun performCleanup() {
            val startTime = System.nanoTime()
            lock.write {
                stack.removeAll { it.get() == null }
                invalidCount.set(0)
            }
            val duration = (System.nanoTime() - startTime) / 1_000_000 // 转换为毫秒
            performanceStats.totalCleanupTime.addAndGet(duration)
            performanceStats.cleanupOperations.incrementAndGet()
        }
        
        fun clear() {
            lock.write {
                stack.clear()
                invalidCount.set(0)
            }
        }
    }
    
    private val activityStackManager = StackManager<Activity>()
    private val serviceStackManager = StackManager<Service>()
    private val fragmentStackManager = StackManager<Fragment>()
    // endregion

    // region 监听器管理器
    private class ListenerManager<T> {
        private val listeners = ConcurrentHashMap<String, T>()
        private val lock = ReentrantReadWriteLock()
        
        fun register(tag: String, listener: T) {
            listeners[tag] = listener
        }
        
        fun unregister(tag: String) {
            listeners.remove(tag)
        }
        
        fun notifyAll(action: (T) -> Unit) {
            val snapshot = lock.read { listeners.values.toList() }
            snapshot.forEach { listener ->
                try {
                    action(listener)
                    performanceStats.listenerNotifications.incrementAndGet()
                } catch (e: Exception) {
                    LogPure.e(TAG, "Listener notification failed: ${e.message}")
                }
            }
        }
        
        fun size(): Int = listeners.size
        
        fun clear() {
            listeners.clear()
        }
    }
    
    private val foregroundListenerManager = ListenerManager<(Boolean) -> Unit>()
    private val activityLifecycleListenerManager = ListenerManager<(Activity, String) -> Unit>()
    private val pausedListenerManager = ListenerManager<(Activity) -> Unit>()
    // endregion

    // region 配置管理
    data class TrackConfig(
        val maxStackSize: Int = MAX_STACK_SIZE,
        val cleanupThreshold: Float = CLEANUP_THRESHOLD,
        val cleanupInterval: Long = CLEANUP_INTERVAL_MS,
        val fragmentTimeWindow: Long = FRAGMENT_TIME_WINDOW_MS,
        val enablePerformanceStats: Boolean = true,
        val enableAutoCleanup: Boolean = true
    )
    
    @Volatile
    private var config = TrackConfig()
    
    @JvmStatic
    fun updateConfig(newConfig: TrackConfig) {
        config = newConfig
        if (newConfig.enableAutoCleanup) {
            startPeriodicCleanup()
        } else {
            stopPeriodicCleanup()
        }
    }
    // endregion

    // region 核心数据存储
    private val fragmentCallbacks = ConcurrentHashMap<FragmentActivity, FragmentManager.FragmentLifecycleCallbacks>()
    private val fragmentResumeTimes = ConcurrentHashMap<String, Long>()
    
    @Volatile
    private var maxFragmentResumeRecords = 200
    
    // 使用 ConcurrentHashMap.newKeySet() 优化排除列表
    private val excludedFragments: MutableSet<String> = ConcurrentHashMap.newKeySet<String>().apply {
        add("com.gyf.immersionbar.SupportRequestBarManagerFragment")
        add("com.gyf.immersionbar.RequestBarManagerFragment")
    }
    
    private val excludedActivities: MutableSet<String> = ConcurrentHashMap.newKeySet()
    
    // 缓存反射结果
    @Volatile
    private var cachedApplication: Application? = null
    private val applicationLock = ReentrantReadWriteLock()
    
    // 定时清理任务
    private var cleanupExecutor: ScheduledExecutorService? = null
    // endregion

    // region 初始化和配置
    internal fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // 缓存 Application 实例
        applicationLock.write {
            cachedApplication = application
        }
        
        // 启动定时清理
        if (config.enableAutoCleanup) {
            startPeriodicCleanup()
        }
        
        LogPure.i(TAG, "AppTrackV2 initialized with config: $config")
    }
    
    private fun startPeriodicCleanup() {
        stopPeriodicCleanup()
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "AppTrack-Cleanup").apply { isDaemon = true }
        }.apply {
            scheduleWithFixedDelay({
                try {
                    performPeriodicCleanup()
                } catch (e: Exception) {
                    LogPure.e(TAG, "Periodic cleanup failed: ${e.message}")
                }
            }, config.cleanupInterval, config.cleanupInterval, TimeUnit.MILLISECONDS)
        }
    }
    
    private fun stopPeriodicCleanup() {
        cleanupExecutor?.shutdown()
        cleanupExecutor = null
    }
    
    private fun performPeriodicCleanup() {
        val startTime = System.currentTimeMillis()
        
        // 清理 Fragment 时间记录
        if (fragmentResumeTimes.size > maxFragmentResumeRecords) {
            val entries = fragmentResumeTimes.entries.sortedBy { it.value }
            val toRemove = entries.take(entries.size - (maxFragmentResumeRecords * 0.8).toInt())
            toRemove.forEach { fragmentResumeTimes.remove(it.key) }
        }
        
        val duration = System.currentTimeMillis() - startTime
        LogPure.d(TAG, "Periodic cleanup completed in ${duration}ms")
    }
    
    @JvmStatic
    fun setMaxFragmentRecords(max: Int) {
        maxFragmentResumeRecords = max.coerceAtLeast(50)
    }
    // endregion

    // region 公共API - 监听器管理
    @JvmStatic
    @JvmOverloads
    fun registerAppStatusListener(tag: String = TAG, listener: (Boolean) -> Unit) {
        foregroundListenerManager.register(tag, listener)
    }

    @JvmStatic
    fun unregisterAppStatusListener(tag: String = TAG) {
        foregroundListenerManager.unregister(tag)
    }

    @JvmStatic
    fun registerActivityTransitionListener(
        tag: String = TAG,
        listener: (Activity, ActivityTransitionEvent) -> Unit
    ) {
        activityLifecycleListenerManager.register(tag) { activity, event ->
            listener(activity, ActivityTransitionEvent.valueOf(event))
        }
    }

    @JvmStatic
    fun unregisterActivityTransitionListener(tag: String = TAG) {
        activityLifecycleListenerManager.unregister(tag)
    }

    @JvmStatic
    fun addOnActivityPausedListener(activity: Activity, listener: () -> Unit) {
        val key = activity::class.java.name
        pausedListenerManager.register(key) { _ ->
            listener.invoke()
            pausedListenerManager.unregister(key)
        }
    }

    @JvmStatic
    fun trackActivityTime(activity: Activity) {
        val startTime = System.currentTimeMillis()
        addOnActivityPausedListener(activity) {
            val duration = System.currentTimeMillis() - startTime
            TimeTracker.updateStats(activity, duration)
        }
    }

    /**
     * 安全显示Toast
     */
    @JvmStatic
    fun showToastSafely(activity: Activity, message: String, duration: Int = Toast.LENGTH_SHORT) {
        safeUIManager.executeUISafely(activity, ToastOperation(message, duration))
    }

    /**
     * 安全显示Dialog
     */
    @JvmStatic
    fun showDialogSafely(activity: Activity, dialogBuilder: (Activity) -> Dialog) {
        safeUIManager.executeUISafely(activity, DialogOperation(dialogBuilder))
    }

    /**
     * 安全显示SnackBar
     */
    @JvmStatic
    fun showSnackBarSafely(activity: Activity, view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        safeUIManager.executeUISafely(activity, SnackbarOperation(view, message, duration))
    }

    /**
     * 通用UI操作安全执行
     */
    @JvmStatic
    fun executeUISafely(activity: Activity, operation: UIOperation) {
        safeUIManager.executeUISafely(activity, operation)
    }

    /**
     * 自定义UI操作的便捷方法
     */
    @JvmStatic
    @JvmOverloads
    fun executeUISafely(
        activity: Activity,
        priority: Int = 0,
        timeout: Long = 30_000L,
        canExecuteCheck: (Activity) -> Boolean = { it.isAlive() && !it.isFinishing },
        uiAction: (Activity) -> Boolean
    ) {
        val operation = object : UIOperation {
            override val priority = priority
            override val timeout = timeout

            override fun execute(activity: Activity): Boolean = uiAction(activity)
            override fun canExecute(activity: Activity): Boolean = canExecuteCheck(activity)
            override fun onTimeout() {
                LogPure.w("UIOperation", "Custom UI operation timeout")
            }
        }
        safeUIManager.executeUISafely(activity, operation)
    }
    // endregion

    // region 生命周期跟踪
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        logLifecycle(activity, "onCreate")
        activityStackManager.add(activity)
        if (activity is FragmentActivity) {
            setupFragmentTracking(activity)
        }
        performanceStats.activityTransitions.incrementAndGet()
    }

    override fun onActivityResumed(activity: Activity) {
        logLifecycle(activity, "onResume")
        activityStackManager.remove(activity)
        activityStackManager.add(activity) // 移至栈顶

        // 处理延迟的UI操作 - 这是关键调用点！
        safeUIManager.processPendingOperations(activity)
        
        if (!activity.isChangingConfigurations) {
            notifyActivityLifecycleListeners(activity, ActivityTransitionEvent.ENTER)
        }
        performanceStats.activityTransitions.incrementAndGet()
    }

    override fun onActivityPaused(activity: Activity) {
        pausedListenerManager.notifyAll { listener -> listener(activity) }
        
        if (!activity.isChangingConfigurations) {
            notifyActivityLifecycleListeners(activity, ActivityTransitionEvent.EXIT)
        }
        performanceStats.activityTransitions.incrementAndGet()
    }

    override fun onActivityDestroyed(activity: Activity) {
        logLifecycle(activity, "onDestroy")
        activityStackManager.removeAll { ref -> 
            ref == null || ref == activity || !ref.isAlive() 
        }

        // 清理该Activity相关的待执行UI操作
        safeUIManager.clearPendingOperations(activity)
        
        // 清理相关资源
        pausedListenerManager.unregister(activity::class.java.name)
        if (activity is FragmentActivity) {
            fragmentCallbacks.remove(activity)?.let { callback ->
                try {
                    activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(callback)
                } catch (e: Exception) {
                    LogPure.w(TAG, "Failed to unregister fragment callback: ${e.message}")
                }
            }
        }
        performanceStats.activityTransitions.incrementAndGet()
    }

    override fun onStart(owner: LifecycleOwner) {
        logAppState("Foreground")
        notifyForegroundListeners(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        logAppState("Background")
        notifyForegroundListeners(false)
    }
    // endregion

    // region Service生命周期扩展
    fun onServiceCreate(service: Service) {
        logLifecycle(service, "onCreate")
        serviceStackManager.add(service)
    }

    fun onServiceDestroy(service: Service) {
        logLifecycle(service, "onDestroy")
        serviceStackManager.remove(service)
    }
    // endregion

    // region 扩展工具方法
    @JvmStatic
    fun getTopActivity(): Activity? {
        return activityStackManager.getTop()?.takeIf { it.isAlive() }
    }

    @JvmStatic
    fun finishActivities(vararg activities: Class<*>) {
        val targets = getActiveActivities().filter { it.javaClass in activities }
        executeOnMain { targets.forEach { it.finish() } }
    }

    @JvmStatic
    fun finishAllExcept(clazz: Class<*>) {
        val targets = getActiveActivities().filter { it.javaClass != clazz }
        executeOnMain { targets.forEach { it.finish() } }
    }

    @JvmStatic
    fun finishAllActivities() {
        executeOnMain { getActiveActivities().forEach { it.finish() } }
    }

    @JvmStatic
    fun hasActivity(activityClass: Class<*>): Boolean =
        getActiveActivities().any { it.javaClass == activityClass }

    @JvmStatic
    fun aliveActivityCount(): Int = getActiveActivities().size

    @JvmStatic
    fun getTopFragment(): Fragment? {
        val topActivity = getTopActivity()
        return fragmentStackManager.getAll()
            .firstOrNull { fragment ->
                fragment.isAdded && !fragment.isDetached && fragment.activity == topActivity
            }
    }

    @JvmStatic
    fun getFragmentsByActivity(activity: Activity): List<Fragment> {
        return fragmentStackManager.getAll().filter {
            it.isAdded && it.activity == activity
        }.distinct()
    }

    @JvmStatic
    fun addExcludedActivities(classNames: Collection<String>) {
        excludedActivities.addAll(classNames)
    }

    @JvmStatic
    fun addExcludedActivities(vararg classNames: String) {
        excludedActivities.addAll(classNames)
    }

    @JvmStatic
    fun removeExcludedActivity(className: String) {
        excludedActivities.remove(className)
    }

    @JvmStatic
    fun addExcludedFragments(classNames: Collection<String>) {
        excludedFragments.addAll(classNames)
    }

    @JvmStatic
    fun addExcludedFragments(vararg classNames: String) {
        excludedFragments.addAll(classNames)
    }

    @JvmStatic
    fun removeExcludedFragment(className: String) {
        excludedFragments.remove(className)
    }

    @JvmStatic
    fun getApplicationReflect(): Application? {
        return applicationLock.read { cachedApplication } ?: run {
            val app = getApplicationViaReflection()
            applicationLock.write { cachedApplication = app }
            app
        }
    }

    private fun getActiveActivities(): List<Activity> {
        return activityStackManager.getAll().filter { it.isAlive() }
    }

    private inline fun executeOnMain(crossinline action: () -> Unit) {
        if (isMainThread()) action() else HandlerGT.main.post { action.invoke() }
    }
    // endregion

    // region 私有方法
    private fun notifyForegroundListeners(isForeground: Boolean) {
        foregroundListenerManager.notifyAll { listener ->
            if (isMainThread()) {
                listener(isForeground)
            } else {
                HandlerGT.main.post { listener(isForeground) }
            }
        }
    }

    private fun notifyActivityLifecycleListeners(activity: Activity, event: ActivityTransitionEvent) {
        if (excludedActivities.contains(activity::class.java.name)) {
            LogPure.d(TAG, "Filtered activity transition: ${activity::class.simpleName}")
            return
        }
        
        activityLifecycleListenerManager.notifyAll { listener ->
            executeOnMain { listener(activity, event.name) }
        }
    }

    private fun setupFragmentTracking(activity: FragmentActivity) {
        val callback = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
                handleFragmentEvent(f, "onCreate")
            }

            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                handleFragmentEvent(f, "onResume")
            }

            override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                handleFragmentEvent(f, "onPause")
            }

            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                handleFragmentEvent(f, "onDestroy")
            }
        }
        
        fragmentCallbacks[activity] = callback
        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(callback, true)
    }

    private fun handleFragmentEvent(fragment: Fragment, event: String) {
        if (excludedFragments.contains(fragment::class.java.name)) {
            LogPure.d(TAG, "Filtered fragment: ${fragment::class.simpleName}")
            return
        }
        
        logLifecycle(fragment, event)
        performanceStats.fragmentTransitions.incrementAndGet()
        
        when (event) {
            "onCreate" -> fragmentStackManager.add(fragment)
            "onDestroy" -> {
                fragmentStackManager.remove(fragment)
                fragmentResumeTimes.remove(fragment.javaClass.simpleName)
            }
            "onResume" -> handleFragmentResume(fragment)
        }
    }

    private fun handleFragmentResume(fragment: Fragment) {
        // 优化的 Fragment Resume 处理逻辑
        val className = fragment.javaClass.simpleName
        val currentTime = System.currentTimeMillis()
        val wasExisting = fragmentResumeTimes.containsKey(className)
        
        fragmentResumeTimes[className] = currentTime
        fragmentStackManager.remove(fragment)
        fragmentStackManager.add(fragment)
        
        // 简化的时间窗口处理
        if (!wasExisting) {
            handleFragmentTimeWindow(currentTime, fragment)
        }
    }

    private fun handleFragmentTimeWindow(currentTime: Long, currentFragment: Fragment) {
        val candidates = fragmentResumeTimes.entries
            .filter { currentTime - it.value < config.fragmentTimeWindow }
            .minByOrNull { it.value }
        
        candidates?.let { entry ->
            fragmentStackManager.getAll()
                .find { it.javaClass.simpleName == entry.key }
                ?.let { candidate ->
                    if (candidate != currentFragment) {
                        fragmentStackManager.remove(candidate)
                        fragmentStackManager.add(candidate)
                    }
                }
        }
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

    // region 生命周期日志
    private fun logLifecycle(obj: Any, event: String) {
        if (config.enablePerformanceStats) {
            LogPure.d(TAG, "${obj::class.simpleName?.substringAfterLast('.')} $event")
        }
    }

    private fun logAppState(state: String) {
        LogPure.d(TAG, "App state: $state")
    }
    // endregion

    // region 清理和销毁
    fun destroy() {
        stopPeriodicCleanup()
        activityStackManager.clear()
        serviceStackManager.clear()
        fragmentStackManager.clear()
        foregroundListenerManager.clear()
        activityLifecycleListenerManager.clear()
        pausedListenerManager.clear()
        fragmentCallbacks.clear()
        fragmentResumeTimes.clear()
        excludedFragments.clear()
        excludedActivities.clear()
        
        applicationLock.write { cachedApplication = null }
        performanceStats.reset()
        
        LogPure.i(TAG, "AppTrackV2 destroyed and resources cleaned up")
    }
    // endregion

    // region 默认实现
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    // endregion
}

// endregion