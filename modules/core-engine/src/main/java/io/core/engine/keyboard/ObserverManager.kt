package io.core.engine.keyboard

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.view.View
import android.view.View.OnLayoutChangeListener
import io.core.engine.keyboard.KeyboardObserver.Companion.create
import java.util.WeakHashMap

/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2024/12/24 8:47
 * @description
 * @author Yuan
 */
internal class ObserverManager : ActivityLifecycleCallbacks {

    private val observersMap by lazy {
        WeakHashMap<Activity, KeyboardObserver>()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val observer = create(activity, SoftKeyboardGlobal.isDebug)
        observer.addCallback(SoftKeyboardGlobal)
        observersMap[activity] = observer
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        // avoid bad window token problem
        activity.window.decorView.addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                v?.removeOnLayoutChangeListener(this)
                observersMap[activity]?.watch(false)
            }
        })
    }

    override fun onActivityPaused(activity: Activity) {
        observersMap[activity]?.unwatch()
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        val observer = observersMap.remove(activity)
        observer?.removeCallback(SoftKeyboardGlobal)
    }
}