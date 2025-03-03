@file:Suppress("PrivateApi")

package io.core.common.helper

import android.app.Activity
import android.app.Application
import android.app.Service
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.core.common.base.component.service.BaseService
import io.core.common.util.extensions.ui.isAlive
import io.core.common.util.log.LogPure
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 应用生命周期跟踪器，用于监控Activity/Service生命周期及应用前后台状态
 *
 * 1. 使用CopyOnWriteArrayList保证线程安全，避免遍历时修改导致的并发问题
 * 2. 自动维护有效的弱引用，防止内存泄漏
 * 3. 支持多个前后台状态监听器
 * 4. 优化无效引用清理逻辑
 */
object AppLifecycleTracker : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private const val TAG = "LifecycleTracker"

    // region 生命周期栈管理
    private val activityStack = CopyOnWriteArrayList<WeakReference<Activity>>()
    private val serviceStack = CopyOnWriteArrayList<WeakReference<Service>>()

    /**
     * 获取有效Activity列表（自动过滤已回收的引用）
     */
    private val activeActivities: List<Activity>
        get() = activityStack
            .mapNotNull { it.get() }
            .filter { it.isAlive() }

    /**
     * 获取有效Service列表（自动过滤已回收的引用）
     */
    private val activeServices: List<Service>
        get() = serviceStack
            .mapNotNull { it.get() }
    // endregion

    // region 前后台状态监听
    private val appForegroundListeners = CopyOnWriteArrayList<(Boolean) -> Unit>()
    private val listenerMap = ConcurrentHashMap<String, (Boolean) -> Unit>()

    @JvmStatic
    fun registerAppStatusListenerWithTag(tag: String, listener: (Boolean) -> Unit) {
        listenerMap[tag] = listener
        appForegroundListeners.add(listener)
    }

    @JvmStatic
    fun unregisterAppStatusListenerByTag(tag: String) {
        listenerMap[tag]?.let {
            appForegroundListeners.remove(it)
            listenerMap.remove(tag)
        }
    }

    /**
     * 注册应用前后台状态监听
     */
    @JvmStatic
    fun registerAppStatusListener(listener: (Boolean) -> Unit) {
        appForegroundListeners.add(listener)
    }

    /**
     * 注销应用前后台状态监听
     */
    @JvmStatic
    fun unregisterAppStatusListener(listener: (Boolean) -> Unit) {
        appForegroundListeners.remove(listener)
    }
    // endregion

    // region 生命周期观察者实现
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        appForegroundListeners.forEach { it.invoke(true) }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        appForegroundListeners.forEach { it.invoke(false) }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        logLifecycle(activity, "onCreate")
        activityStack.add(WeakReference(activity))
        cleanUpWeakReferences(activityStack)
    }

    override fun onActivityDestroyed(activity: Activity) {
        logLifecycle(activity, "onDestroy")
        activityStack.removeAll { it.get() == null || it.get() == activity }
    }

    override fun onActivityResumed(activity: Activity) = logLifecycle(activity, "onResume")
    override fun onActivityPaused(activity: Activity) = logLifecycle(activity, "onPause")
    override fun onActivityStarted(activity: Activity) = logLifecycle(activity, "onStart")
    override fun onActivityStopped(activity: Activity) = logLifecycle(activity, "onStop")
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        logLifecycle(activity, "onSaveInstanceState")
    }
    // endregion

    // region Service生命周期跟踪
    fun onServiceCreate(service: BaseService) {
        logLifecycle(service, "onCreate")
        serviceStack.add(WeakReference(service))
        cleanUpWeakReferences(serviceStack)
    }

    fun onServiceDestroy(service: BaseService) {
        logLifecycle(service, "onDestroy")
        serviceStack.removeAll { it.get() == null || it.get() == service }
    }
    // endregion

    // region 公共API
    /**
     * 获取栈顶Activity（可能为null）
     */
    @JvmStatic
    fun getTopActivity(): Activity? = activeActivities.lastOrNull()

    /**
     * 当前存活的Activity数量
     */
    fun activityCount(): Int = activeActivities.size

    /**
     * 检查指定类型的Activity是否存在
     */
    @JvmStatic
    fun hasActivity(activityClass: Class<*>): Boolean =
        activeActivities.any { it.javaClass == activityClass }

    /**
     * 关闭指定类型的所有Activity
     */
    @JvmStatic
    fun finishActivity(vararg activityClasses: Class<*>) {
        activeActivities
            .filter { activity -> activityClasses.any { it == activity.javaClass } }
            .forEach { it.finish() }
    }

    /**
     * 关闭所有Activity
     */
    @JvmStatic
    fun finishAllActivities() {
        activeActivities.forEach { it.finish() }
    }

    /**
     * 反射获取当前应用的Application实例
     */
    @JvmStatic
    fun getApplicationByReflect(): Application? {
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

    // endregion

    // region 工具方法
    /**
     * 统一生命周期日志记录
     */
    private fun logLifecycle(obj: Any, event: String) {
        LogPure.d(TAG, "${obj::class.simpleName} $event")
    }

    /**
     * 清理无效的弱引用（自动维护列表健康）
     */
    private fun <T> cleanUpWeakReferences(list: CopyOnWriteArrayList<WeakReference<T>>) {
        list.removeAll { it.get() == null }
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

    // endregion

    // region 初始化
    /**
     * 需要在Application.onCreate中初始化
     */
    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    // endregion
}