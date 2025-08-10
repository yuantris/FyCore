package io.core.common.helper.track.v3

import android.app.Activity
import android.app.Application
import android.app.Service
import androidx.fragment.app.Fragment

/**
 * 应用追踪器核心接口
 * 
 * @author [Yuantris] - v3.0
 * @since 2025/8/10
 */
interface IAppTracker {
    
    /**
     * 初始化追踪器
     */
    fun init(application: Application)
    
    /**
     * 注册生命周期监听器
     */
    fun registerLifecycleListener(tag: String, listener: LifecycleListener)
    
    /**
     * 注销生命周期监听器
     */
    fun unregisterLifecycleListener(tag: String)
    
    /**
     * 获取栈顶Activity
     */
    fun getTopActivity(): Activity?
    
    /**
     * 获取栈顶Fragment
     */
    fun getTopFragment(): Fragment?
    
    /**
     * 获取所有活跃的Activity
     */
    fun getActiveActivities(): List<Activity>
    
    /**
     * 获取性能统计信息
     */
    fun getPerformanceStats(): PerformanceStats
    
    /**
     * 更新配置
     */
    fun updateConfig(config: TrackerConfig)
    
    /**
     * 销毁追踪器
     */
    fun destroy()
}

/**
 * 生命周期监听器
 */
interface LifecycleListener {
    fun onActivityCreated(activity: Activity) {}
    fun onActivityResumed(activity: Activity) {}
    fun onActivityPaused(activity: Activity) {}
    fun onActivityDestroyed(activity: Activity) {}
    fun onFragmentCreated(fragment: Fragment) {}
    fun onFragmentResumed(fragment: Fragment) {}
    fun onFragmentPaused(fragment: Fragment) {}
    fun onFragmentDestroyed(fragment: Fragment) {}
    fun onAppForeground() {}
    fun onAppBackground() {}
}

/**
 * 生命周期钩子
 */
interface LifecycleHook {
    fun beforeActivityCreate(activity: Activity): Boolean = true
    fun afterActivityCreate(activity: Activity) {}
    fun beforeActivityDestroy(activity: Activity): Boolean = true
    fun afterActivityDestroy(activity: Activity) {}
    fun beforeFragmentCreate(fragment: Fragment): Boolean = true
    fun afterFragmentCreate(fragment: Fragment) {}
    fun beforeFragmentDestroy(fragment: Fragment): Boolean = true
    fun afterFragmentDestroy(fragment: Fragment) {}
}

/**
 * 组件过滤器
 */
interface ComponentFilter {
    fun shouldTrackActivity(activity: Activity): Boolean
    fun shouldTrackFragment(fragment: Fragment): Boolean
    fun shouldTrackService(service: Service): Boolean
    fun getPriority(component: Any): Int = 0
}