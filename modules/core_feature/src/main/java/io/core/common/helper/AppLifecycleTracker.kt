package io.core.common.helper

import android.app.Activity
import android.app.Application
import android.app.Service
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.bumptech.glide.Glide.init
import io.core.common.base.component.service.BaseService
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.extensions.logD
import io.core.common.util.extensions.ui.isAlive
import io.core.common.util.log.LogPure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

object AppLifecycleTracker : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private const val TAG = "LifecycleTracker"

    // Activity生命周期队列（线程安全）
    private val activityStack = CopyOnWriteArrayList<WeakReference<Activity>>()

    // Service生命周期记录
    private val serviceStack = CopyOnWriteArrayList<WeakReference<Service>>()

    private var appFinishedListener: (() -> Unit)? = null

    private val _isInForeground = MutableStateFlow(false)
    val isInForeground: StateFlow<Boolean> = _isInForeground

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        _isInForeground.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        _isInForeground.value = false
    }

    /**
     * App退出监听
     */
    fun setOnAppFinishedListener(appFinishedListener: (() -> Unit)) {
        this.appFinishedListener = appFinishedListener
    }

    fun activitySize(): Int {
        return activityStack.size
    }

    fun getTopActivity(): Activity? {
        // 判断活动栈是否为空
        return activityStack.lastOrNull()?.get()?.takeIf { it.isAlive() }
    }

    /**
     * 判断指定Activity是否存在
     */
    @JvmStatic
    fun isExistActivity(activityClass: Class<*>): Boolean {
        activityStack.forEach { item ->
            if (item.get()?.javaClass == activityClass) {
                return true
            }
        }
        return false
    }

    /**
     * 关闭指定 activity(class)
     */
    fun finishActivity(vararg activityClasses: Class<*>) {
        val waitFinish = ArrayList<WeakReference<Activity>>()
        for (temp in activityStack) {
            for (activityClass in activityClasses) {
                if (temp.get()?.javaClass == activityClass) {
                    waitFinish.add(temp)
                    break
                }
            }
        }
        waitFinish.forEach {
            it.get()?.finish()
        }
    }

    /**
     * 关闭所有activity(class)
     */
    fun finishAllActivity() {
        for (temp in activityStack) {
            temp.get()?.finish()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        LogPure.d(TAG, "${activity.javaClass.simpleName} onPause")
    }

    override fun onActivityResumed(activity: Activity) {
        LogPure.d(TAG, "${activity.javaClass.simpleName} onResume")
    }

    override fun onActivityStarted(activity: Activity) {
        LogPure.d(TAG, "${activity.javaClass.simpleName} onStart")
    }

    override fun onActivityDestroyed(activity: Activity) {
        LogPure.d(TAG, "${activity.javaClass.simpleName} onDestroy")
        for (temp in activityStack) {
            if (temp.get() != null && temp.get() === activity) {
                activityStack.remove(temp)
                if (serviceStack.size == 0 && activityStack.size == 0) {
                    onAppFinished()
                }
                break
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        LogPure.d(TAG, "${activity.javaClass.simpleName} onSaveInstanceState")
    }

    override fun onActivityStopped(activity: Activity) {
        LogPure.d(TAG, "${activity.javaClass.simpleName} onStop")
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        LogPure.d(TAG, "${activity.javaClass.simpleName} onCreate")
        activityStack.add(WeakReference(activity))
    }

    @Synchronized
    fun onServiceCreate(service: BaseService) {
        LogPure.d(TAG, "${service::class.simpleName} onCreate")
        serviceStack.add(WeakReference(service))
    }

    @Synchronized
    fun onServiceDestroy(service: BaseService) {
        LogPure.d(TAG, "${service::class.simpleName} onDestroy")
        for (temp in serviceStack) {
            if (temp.get() != null && temp.get() === service) {
                serviceStack.remove(temp)
                if (serviceStack.size == 0 && activityStack.size == 0) {
                    onAppFinished()
                }
                break
            }
        }
    }

    private fun onAppFinished() {
        appFinishedListener?.invoke()
    }
}