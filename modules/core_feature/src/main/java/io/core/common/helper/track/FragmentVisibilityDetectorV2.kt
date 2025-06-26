package io.core.common.helper.track

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowInsets
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import io.core.common.util.extensions.cool.runDelayedMain
import io.core.common.util.extensions.logE
import io.core.common.util.extensions.logI
import io.core.common.util.extensions.logV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Fragment可见性检测器V2
 *
 * 功能特性：
 * 1. 继承标准生命周期管理接口
 * 2. 支持多层级可见性检查（Fragment -> Parent -> Activity）
 * 3. 可配置的检测策略
 * 4. 支持ViewPager2集成
 * 5. 提供构造器和工厂方法两种初始化方式
 */
class FragmentVisibilityDetectorV2 private constructor(
    private val fragment: Fragment,
    private val config: VisibilityConfig
) : DefaultLifecycleObserver {

    private var isCurrentlyVisible = false
    private var callback: VisibilityCallback? = null

    // ViewPager2集成
    private var currentViewPager2: WeakReference<ViewPager2>? = null
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) = checkVisibility()
    }

    /** 可见性配置项 */
    data class VisibilityConfig(
        val checkParentVisibility: Boolean = true,
        val checkWindowVisibility: Boolean = true,
        val debugTag: String? = null
    )

    init {
        fragment.lifecycle.addObserver(this)
        scheduleInitialCheck()
    }

    /** 延迟初始检查确保Fragment已正确附加 */
    private fun scheduleInitialCheck() {
        fragment.view?.post {
            checkVisibility("InitialCheck")
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        checkVisibility("Lifecycle-onResume")
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (isCurrentlyVisible) {
            handleVisibilityChange(false, "Lifecycle-onPause")
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        debugLog("Lifecycle-onDestroy")
        detach()
    }

    // 外部触发方法（保持简洁API）
    fun onUserVisibleHintChanged(isVisibleToUser: Boolean) =
        checkVisibility("setUserVisibleHint($isVisibleToUser)")

    fun onHiddenChanged(hidden: Boolean) =
        checkVisibility("onHiddenChanged($hidden)")

    /**
     * 核心可见性检测逻辑
     * @param triggerSource 触发源
     * @param retryCount 当前重试次数，默认0
     */
    private fun checkVisibility(triggerSource: String? = null, retryCount: Int = 0) {
        val shouldBeVisible = calculateActualVisibility()

        if (shouldBeVisible != isCurrentlyVisible) {
            handleVisibilityChange(shouldBeVisible, triggerSource)
        } else if (retryCount < 2) { // 最多重试2次
            fragment.lifecycleScope.launch {
                delay(300)
                checkVisibility(triggerSource, retryCount + 1)
            }
        }
    }

    /** 执行可见性状态变更 */
    private fun handleVisibilityChange(newState: Boolean, source: String?) {
        isCurrentlyVisible = newState
        debugLog("VisibilityChanged: $newState via ${source ?: "internal"}")
        callback?.onVisibilityChanged(newState)
    }

    /** 多层级可见性计算 */
    private fun calculateActualVisibility(): Boolean {
        return fragment.run {
            // 基础可见性检查
            val baseVisible = isAdded && !isHidden && userVisibleHint
                    && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            // 可配置的扩展检查
            baseVisible && checkParentVisibility() && checkWindowVisibility()
        }
    }

    /** 检查父Fragment可见性 */
    private fun checkParentVisibility(): Boolean {
        return if (config.checkParentVisibility) {
            fragment.parentFragment?.isVisible != false
        } else true
    }

    /** 检查Activity窗口可见性 */
    private fun checkWindowVisibility(): Boolean {
        return if (config.checkWindowVisibility) {
            val activity = fragment.activity ?: return true
            activity.window.decorView.isVisible
        } else true
    }

    /** 调试日志输出 */
    private fun debugLog(message: String) {
        config.debugTag?.let { Log.d(it, "[${fragment.javaClass.simpleName}] $message") }
    }

    /** 清理资源 */
    private fun detach() {
        callback = null
        currentViewPager2?.get()?.unregisterOnPageChangeCallback(pageChangeCallback)
        fragment.lifecycle.removeObserver(this)
    }

    /** 公开API：设置回调监听 */
    fun setVisibilityCallback(callback: VisibilityCallback) {
        this.callback = callback
    }

    /** 公开API：关联ViewPager2 */
    fun attachToViewPager2(viewPager: ViewPager2) {
        currentViewPager2 = WeakReference(viewPager).also {
            viewPager.registerOnPageChangeCallback(pageChangeCallback)
        }
    }

    fun interface VisibilityCallback {
        fun onVisibilityChanged(visible: Boolean)
    }

    companion object {
        /** 工厂方法：快速附加到Fragment */
        @JvmStatic
        @JvmOverloads
        fun attachToFragment(
            fragment: Fragment,
            config: VisibilityConfig = VisibilityConfig(
                checkParentVisibility = true,
                checkWindowVisibility = true,
                debugTag = fragment.javaClass.simpleName
            ),
            callback: VisibilityCallback
        ): FragmentVisibilityDetectorV2 {
            return FragmentVisibilityDetectorV2(fragment, config).apply {
                setVisibilityCallback(callback)
            }
        }
    }
}