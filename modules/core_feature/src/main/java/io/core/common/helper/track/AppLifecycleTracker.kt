package io.core.common.helper.track

import android.app.Activity
import android.app.Application
import android.app.Service
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.core.common.helper.ReflectHelper
import io.core.common.util.extensions.cool.isMainThread
import io.core.common.util.extensions.ui.isAlive
import io.core.common.util.log.LogPure
import io.core.common.util.tools.buildMainHandler
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * 应用生命周期跟踪器，用于监控Activity/Service生命周期及应用前后台状态
 *
 * 1. 使用CopyOnWriteArrayList保证线程安全，避免遍历时修改导致的并发问题
 * 2. 自动维护有效的弱引用，防止内存泄漏
 * 3. 支持多个前后台状态监听器
 * 4. 优化无效引用清理逻辑
 */
object AppLifecycleTracker : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private const val TAG = "AppLifecycleTracker"

    // region 核心数据存储
    private val activityStack = ConcurrentLinkedDeque<WeakReference<Activity>>()
    private val serviceStack = ConcurrentLinkedDeque<WeakReference<Service>>()
    private val foregroundListeners = ConcurrentHashMap<String, (Boolean) -> Unit>()

    // 新增暂停监听器相关成员
    private val pausedListeners = ConcurrentHashMap<String, (Activity) -> Unit>()
    // endregion

    // region 公共API
    /** 初始化（Application中调用） */
    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }


    /** 注册带标签的前后台监听器 */
    @JvmStatic
    @JvmOverloads
    fun registerAppStatusListener(tag: String = TAG, listener: (Boolean) -> Unit) {
        foregroundListeners[tag] = listener
    }

    /** 注销监听器 */
    @JvmStatic
    fun unregisterAppStatusListener(tag: String = TAG) {
        foregroundListeners.remove(tag)
    }

    /**
     * 添加Activity暂停监听器
     * @param activity 要监听的Activity
     * @param listener 暂停回调
     */
    @JvmStatic
    fun addOnActivityPausedListener(activity: Activity, listener: () -> Unit) {
        val key = activity::class.java.name
        pausedListeners[key] = { _ -> listener() }
    }

    /**
     * 跟踪Activity生命周期，统计时长
     * @param activity 要跟踪的Activity
     */
    @JvmStatic
    fun trackActivityTime(activity: Activity) {
        val startTime = System.currentTimeMillis()

        addOnActivityPausedListener(activity) {
            val duration = System.currentTimeMillis() - startTime
            TimeTracker.updateStats(activity, duration)
        }
    }
    // endregion

    // region 生命周期跟踪
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        logLifecycle(activity, "onCreate")
        updateActivityStack(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        logLifecycle(activity, "onResume")
        bringToFront(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        val key = activity::class.java.name
        pausedListeners[key]?.invoke(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        logLifecycle(activity, "onDestroy")
        activityStack.removeAll { it.get() == activity }
    }

    override fun onStart(owner: LifecycleOwner) {
        logAppState("Foreground")
        notifyListeners(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        logAppState("Background")
        notifyListeners(false)
    }
    // endregion

    // region Service生命周期扩展
    fun onServiceCreate(service: Service) {
        logLifecycle(service, "onCreate")
        serviceStack.add(WeakReference(service))
        cleanUpWeakReferences(serviceStack)
    }

    fun onServiceDestroy(service: Service) {
        logLifecycle(service, "onDestroy")
        serviceStack.removeAll { it.get() == service }
    }
    // endregion

    // region 私有方法
    /** 带异常捕获的监听通知 */
    private fun notifyListeners(isForeground: Boolean) {
        foregroundListeners.values.forEach { listener ->
            try {
                if (isMainThread()) listener(isForeground) else {
                    buildMainHandler().post { listener(isForeground) }
                }
            } catch (e: Exception) {
                LogPure.e(TAG, "Listener error ${e.message}")
            }
        }
    }

    /** 更新Activity栈顺序 */
    private fun updateActivityStack(activity: Activity) {
        cleanUpWeakReferences(activityStack)
        activityStack.addFirst(WeakReference(activity))
    }

    /** 将Activity移至栈顶 */
    private fun bringToFront(activity: Activity) {
        synchronized(activityStack) {
            // 检查栈顶是否已经是当前Activity
            val topRef = activityStack.peekFirst()
            if (topRef?.get() == activity) return@synchronized

            // 非栈顶时执行移除和添加操作
            activityStack.removeAll { it.get() == activity }
            activityStack.addFirst(WeakReference(activity))
        }
    }

    /**
     * 反射获取当前应用的Application实例
     */
    @JvmStatic
    fun getApplicationReflect(): Application? {
        return try {
            // 获取 ActivityThread 实例
            val thread = getActivityThread() ?: return null
            // 通过 ActivityThread 实例调用 getApplication() 方法
            val app = ReflectHelper.on("android.app.ActivityThread")
                .getMethod("getApplication")
                .invoke(thread) ?: return null
            return app as Application
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 反射获取ActivityThread实例
     */
    private fun getActivityThread(): Any? {
        // 优先尝试通过静态字段获取
        val fromField = try {
            ReflectHelper.on("android.app.ActivityThread")
                .getField("sCurrentActivityThread")
        } catch (e: Exception) {
            Log.e(TAG, "getActivityThreadInActivityThreadStaticField: ${e.message}")
            null
        }

        return fromField ?: try {
            // 字段获取失败后尝试通过静态方法获取
            ReflectHelper.on("android.app.ActivityThread")
                .chainInvoke("currentActivityThread")
                .get() as Any?
        } catch (e: Exception) {
            Log.e(TAG, "getActivityThreadInActivityThreadStaticMethod: ${e.message}")
            null
        }
    }

    /** 清理无效弱引用 */
    private fun <T> cleanUpWeakReferences(list: ConcurrentLinkedDeque<WeakReference<T>>) {
        list.removeAll { it.get() == null }
    }
    // endregion

    // region 扩展工具方法
    /** 获取栈顶Activity（线程安全） */
    @JvmStatic
    fun getTopActivity(): Activity? {
        cleanUpWeakReferences(activityStack)
        return activityStack.peekFirst()?.get()?.takeIf { it.isAlive() }
    }

    /** 安全结束Activity集合 */
    @JvmStatic
    fun finishActivities(vararg activities: Class<*>) {
        val targets = activeActivities.filter { it.javaClass in activities }
        executeOnMain { targets.forEach { it.finish() } }
    }

    /** 结束所有非指定Activity */
    @JvmStatic
    fun finishAllExcept(clazz: Class<*>) {
        val targets = activeActivities.filter { it.javaClass != clazz }
        executeOnMain { targets.forEach { it.finish() } }
    }

    /** 关闭所有Activity */
    @JvmStatic
    fun finishAllActivities() {
        executeOnMain { activeActivities.forEach { it.finish() } }
    }

    /** 检查指定类型的Activity是否存在 */
    @JvmStatic
    fun hasActivity(activityClass: Class<*>): Boolean =
        activeActivities.any { it.javaClass == activityClass }

    /** 当前存活的Activity数量*/
    fun aliveActivityCount(): Int = activeActivities.size

    /** 当前存活Activity列表 */
    private val activeActivities: List<Activity>
        get() = activityStack.mapNotNull { it.get() }.filter { it.isAlive() }

    /** 主线程执行 */
    private inline fun executeOnMain(crossinline action: () -> Unit) {
        if (isMainThread()) action() else buildMainHandler().post { action.invoke() }
    }
    // endregion

    // region 生命周期日志
    private fun logLifecycle(obj: Any, event: String) {
        LogPure.d(TAG, "${obj::class.simpleName?.substringAfterLast('.')} $event")
    }

    private fun logAppState(state: String) {
        LogPure.d(TAG, "App state: $state")
    }
    // endregion

    // region 默认实现（简化）
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    // endregion

}