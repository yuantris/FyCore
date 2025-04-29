package io.core.common.helper.track

import android.app.Activity
import android.app.Application
import android.app.Service
import android.os.Bundle
import android.util.Log
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
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque


/**
 * 应用程序全局生命周期追踪管理器
 *
 * 实现特性：
 * - 全量Activity生命周期事件监听（基于Application.ActivityLifecycleCallbacks）
 * - 应用前后台状态追踪（基于ProcessLifecycleOwner）
 * - 线程安全的组件栈管理（使用ConcurrentLinkedDeque实现）
 * - 带标签的前后台状态监听器注册机制
 *
 * 线程安全：
 * - 所有公共API均已实现线程安全
 * - 组件栈使用并发集合实现（ConcurrentLinkedDeque）
 * - 状态监听通知自带主线程切换保障
 *
 * @see Application.ActivityLifecycleCallbacks
 * @see ProcessLifecycleOwner
 */
object AppTrackV2 : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private const val TAG = "AppTrackV2"
    private const val MAX_STACK_SIZE = 100 // ✅ 推荐值：满足常规业务场景

    // region 核心数据存储
    /**
     * 线程安全的Activity弱引用栈
     *
     * 存储规则：
     * - 使用WeakReference避免内存泄漏
     * - 栈顶始终为最近resumed的Activity
     * - 自动清理无效引用（cleanUpWeakReferences）
     */
    private val activityStack = ConcurrentLinkedDeque<WeakReference<Activity>>()
    private val serviceStack = ConcurrentLinkedDeque<WeakReference<Service>>()
    private val fragmentStack = ConcurrentLinkedDeque<WeakReference<Fragment>>()

    // 前台状态监听器
    private val foregroundListeners = ConcurrentHashMap<String, (Boolean) -> Unit>()
    // Activity暂停监听器
    private val pausedListeners = ConcurrentHashMap<String, (Activity) -> Unit>()
    // Fragment生命周期回调存储
    private val fragmentCallbacks =
        ConcurrentHashMap<FragmentActivity, FragmentManager.FragmentLifecycleCallbacks>()

    /** 存储Fragment的Resume时间 */
    private val fragmentResumeTimes = ConcurrentHashMap<String, Long>()
    @Volatile
    var maxFragmentResumeRecords = 200
        private set

    /**
     * 设置最大Fragment记录数。
     * @param max 要设置的最大Fragment记录数。该值将被强制调整为至少为50。
     */
    @JvmStatic
    fun setMaxFragmentRecords(max: Int) {
        maxFragmentResumeRecords = max.coerceAtLeast(50)
    }

    /**
     * Fragment类名排除列表（线程安全）
     *
     * 包含规则：
     * - 内部预设系统级Fragment类名
     * - 外部可动态添加三方库Fragment类名
     * - 使用全限定类名进行匹配
     */
    private val excludedFragments = ConcurrentHashMap<String, Unit>().apply {
        // 内部预设值
        put("com.gyf.immersionbar.SupportRequestBarManagerFragment", Unit) // Immersionbar
        put("com.gyf.immersionbar.RequestBarManagerFragment", Unit) // Immersionbar
    }
    // endregion

    // region 公共API
    /**
     * 初始化追踪器（需在Application.onCreate中调用）
     *
     * @param application 应用程序实例
     *
     * 实现机制：
     * - 注册Activity生命周期全局监听
     * - 绑定ProcessLifecycleOwner观察应用前后台状态
     */
    internal fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }


    /**
     * 注册带标签的前后台状态监听器
     *
     * @param tag 监听器标识（建议使用调用方类名）
     * @param listener 状态回调（true: 前台, false: 后台）
     *
     * 特性：
     * - 自动处理线程切换（保证在主线程回调）
     * - 支持多个独立监听器共存
     * - 使用ConcurrentHashMap保证线程安全
     */
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
        pausedListeners[key] = { _ ->
            listener.invoke()
            // 自动触发一次清理（双重保障）
            pausedListeners.remove(key)
        }
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
        if (activity is FragmentActivity) {
            // 监听Fragment生命周期
            setupFragmentTracking(activity)
        }
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
        activityStack.removeAll {
            val ref = it.get()
            ref == null || ref == activity || !ref.isAlive()
        }
        // 新增内存泄漏修复代码
        pausedListeners.remove(activity::class.java.name)
        if (activity is FragmentActivity) {
            fragmentCallbacks.remove(activity)?.let { callback ->
                activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(callback)
            }
        }
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

    // region 扩展工具方法
    /**
     * 获取当前栈顶Activity（线程安全）
     *
     * @return 可能为null（当没有存活Activity时）
     *
     * 注意事项：
     * - 返回前会自动执行弱引用清理
     * - 需检查Activity.isAlive()状态
     */
    @JvmStatic
    fun getTopActivity(): Activity? {
        cleanUpWeakReferences(activityStack) // ✅ 确保数据有效性
        return activityStack.peekFirst()?.get()?.takeIf { it.isAlive() }
    }

    /**
     * 安全结束指定类型的Activity集合
     *
     * @param activities 需要结束的Activity class数组
     *
     * 实现特性：
     * - 自动过滤已销毁的Activity实例
     * - 确保在主线程执行finish操作
     * - 支持批量结束多个类型Activity
     */
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

    /**
     * 获取当前栈顶Fragment（线程安全）
     * @return 可能为null（当没有有效Fragment时）
     *
     * 注意事项：
     * - 返回前会自动清理无效引用
     * - 如果在Fragment的OnResume方法调用时，存在竞态条件，可能出现还未加入Fragment栈，而获取到之前的Fragment实例
     *   因此此种情况需要延时获取
     */
    @JvmStatic
    fun getTopFragment(): Fragment? {
        cleanUpWeakReferences(fragmentStack)
        return synchronized(fragmentStack) {
            val topActivity = getTopActivity()
            fragmentStack.mapNotNull { it.get() }
                .firstOrNull { fragment ->
                    fragment.isAdded && !fragment.isDetached &&
                            // 新增宿主Activity匹配检查
                            fragment.activity == topActivity
                }
        }
    }

    /**
     * 获取当前关联的Fragment集合
     * @param activity 宿主Activity
     */
    @JvmStatic
    fun getFragmentsByActivity(activity: Activity): List<Fragment> {
        return fragmentStack.mapNotNull { it.get() }.filter {
            it.isAdded && it.activity == activity
        }.distinct()
    }


    /**
     * 批量添加需要排除的Fragment类名（线程安全）
     * @param classNames 全限定类名集合（例如：["com.example.Fragment1", "com.lib.Fragment2"]）
     */
    @JvmStatic
    fun addExcludedFragments(classNames: Collection<String>) {
        classNames.forEach { excludedFragments[it] = Unit }
    }

    /**
     * 批量添加需要排除的Fragment类名（可变参数版本）
     * @param classNames 全限定类名数组（例如："com.example.Fragment1", "com.lib.Fragment2"）
     */
    @JvmStatic
    fun addExcludedFragments(vararg classNames: String) {
        classNames.forEach { excludedFragments[it] = Unit }
    }

    /**
     * 移除已排除的Fragment类名
     * @param className 全限定类名
     */
    @JvmStatic
    fun removeExcludedFragment(className: String) {
        excludedFragments.remove(className)
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

    /** 当前存活Activity列表 */
    private val activeActivities: List<Activity>
        get() {
            cleanUpWeakReferences(activityStack) // ✅ 数据过滤前置条件
            return activityStack.mapNotNull { it.get() }.filter { it.isAlive() }
        }

    /** 主线程执行 */
    private inline fun executeOnMain(crossinline action: () -> Unit) {
        if (isMainThread()) action() else HandlerGT.handler.post { action.invoke() }
    }
    // endregion

    // region 私有方法
    /** 带异常捕获的监听通知 */
    private fun notifyListeners(isForeground: Boolean) {
        foregroundListeners.values.forEach { listener ->
            try {
                if (isMainThread()) listener(isForeground) else {
                    HandlerGT.handler.post { listener(isForeground) }
                }
            } catch (e: Exception) {
                LogPure.e(TAG, "Listener error ${e.message}")
            }
        }
    }

    /** 更新Activity栈顺序 */
    private fun updateActivityStack(activity: Activity) {
        cleanUpWeakReferences(activityStack) // ✅ 必要前置清理
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

    /** 将Fragment移至栈顶 */
    private fun bringFragmentToFront(fragment: Fragment) {
        synchronized(fragmentStack) {
            // 检查栈顶是否已经是当前Fragment
            val topRef = fragmentStack.peekFirst()
            if (topRef?.get() == fragment) return@synchronized

            // 非栈顶时执行移除和添加操作
            fragmentStack.removeAll { it.get() == fragment }
            fragmentStack.addFirst(WeakReference(fragment))
        }
    }

    /** 设置Fragment生命周期监听 */
    private fun setupFragmentTracking(activity: FragmentActivity) {
        val callback = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentCreated(
                fm: FragmentManager,
                f: Fragment,
                savedInstanceState: Bundle?
            ) {
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

    /** 处理Fragment生命周期事件 */
    private fun handleFragmentEvent(fragment: Fragment, event: String) {
        // 统一过滤逻辑
        if (excludedFragments.containsKey(fragment::class.java.name)) {
            LogPure.d(TAG, "Filtered fragment: ${fragment::class.simpleName}")
            return
        }
        logLifecycle(fragment, event)
        when (event) {
            "onCreate" -> addToStack(fragmentStack, fragment)
            "onDestroy" -> fragmentStack.removeAll { it.get() == fragment }
            "onResume" -> {
                // 新增容量清理逻辑
                if (fragmentResumeTimes.size >= maxFragmentResumeRecords) {
                    val iterator = fragmentResumeTimes.entries.iterator()
                    while (iterator.hasNext() && fragmentResumeTimes.size > maxFragmentResumeRecords * 0.9) {
                        iterator.next()
                        iterator.remove()
                    }
                }
                val containsKey = fragmentResumeTimes.containsKey(fragment.javaClass.simpleName)
                fragmentResumeTimes[fragment.javaClass.simpleName] = System.currentTimeMillis()
                // 确保相同Fragment只保留最新实例在栈顶
                synchronized(fragmentStack) {
                    fragmentStack.removeAll { it.get() == fragment }
                    addToStack(fragmentStack, fragment)
                    bringFragmentToFront(fragment)

                    // TODO: 补充代码：fragmentResumeTimes按照value时间戳从大到小排序，选择出value时间戳之差小于100ms的key(可能有多个)，
                    // TODO: 按照时间戳时间最早排序取第一个key，然后将fragmentStack中该key的Fragment移至栈顶

                    // 完成新增代码：处理时间戳相近的Fragment
                    val now = System.currentTimeMillis()
                    val candidates = fragmentResumeTimes.entries
                        .sortedByDescending { it.value } // 1. 按时间戳降序排序
                        .takeWhile { now - it.value < 100 } // 2. 筛选100ms内的记录
                        .sortedBy { it.value } // 3. 按时间戳升序（最早的排前）

                    // 找到最早触发的有效Fragment实例
                    candidates.firstOrNull()?.let { entry ->
                        fragmentStack.firstOrNull { ref ->
                            ref.get()?.javaClass?.simpleName == entry.key
                        }?.get()?.let { candidate ->
                            if (!containsKey) return@handleFragmentEvent
                            // 将候选Fragment移至栈顶
                            bringFragmentToFront(candidate)
                        }
                    }
                }
            }
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

    /** 添加到栈中，并清理超出最大容量的元素 */
    private fun <T> addToStack(stack: ConcurrentLinkedDeque<WeakReference<T>>, item: T) {
        stack.addFirst(WeakReference(item))
        while (stack.size > MAX_STACK_SIZE) {
            stack.removeLast()
        }
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