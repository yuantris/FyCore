package io.core.common.util.tools

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import io.core.appCtx
import kotlin.math.absoluteValue

/**
 * 软键盘工具类
 */
object KeyboardTools {
    private const val TAG = "KeyboardTools"

    private var sDecorViewDelta = 0
    private var millis: Long = 0

    /**
     * 显示软键盘（无参通用方法）
     */
    @JvmStatic
    fun showSoftInput() {
        val imm = appCtx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    /**
     * 显示软键盘（Activity版本）
     * @param activity 目标Activity
     */
    @JvmStatic
    fun showSoftInput(activity: Activity?) {
        activity?.takeIf { isSoftInputVisible(it).not() }?.run {
            toggleSoftInput()
        }
    }

    /**
     * 显示软键盘（View版本）
     * @param view 目标视图
     */
    @JvmStatic
    fun showSoftInput(view: View) = showSoftInput(view, 0)

    /**
     * 显示软键盘（带标志位）
     * @param view 目标视图
     * @param flags 显示标志位
     */
    @JvmStatic
    fun showSoftInput(view: View, flags: Int) {
        val imm =
            appCtx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return

        view.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }

        imm.showSoftInput(view, flags, object : ResultReceiver(Handler()) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (resultCode == InputMethodManager.RESULT_UNCHANGED_HIDDEN ||
                    resultCode == InputMethodManager.RESULT_HIDDEN
                ) {
                    toggleSoftInput()
                }
            }
        })
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    /**
     * 隐藏软键盘（Activity版本）
     * @param activity 目标Activity
     */
    @JvmStatic
    fun hideSoftInput(activity: Activity?) {
        activity?.window?.let { hideSoftInput(it) }
    }

    /**
     * 隐藏软键盘（Window版本）
     * @param window 目标Window
     */
    @JvmStatic
    fun hideSoftInput(window: Window?) {
        window ?: return

        val view = window.currentFocus ?: run {
            val decorView = window.decorView
            decorView.findViewWithTag<View?>("keyboardTagView") ?: EditText(window.context).apply {
                tag = "keyboardTagView"
                (decorView as? ViewGroup)?.addView(this, 0, 0)
            }.also { it.requestFocus() }
        }
        hideSoftInput(view)
    }

    /**
     * 隐藏软键盘（View版本）
     * @param view 目标视图
     */
    @JvmStatic
    fun hideSoftInput(view: View) {
        val imm = appCtx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * 通过切换方式隐藏软键盘
     * @param activity 目标Activity
     */
    @JvmStatic
    fun hideSoftInputByToggle(activity: Activity?) {
        activity ?: return
        val nowMillis = SystemClock.elapsedRealtime()
        if ((nowMillis - millis).absoluteValue > 500 && isSoftInputVisible(activity)) {
            toggleSoftInput()
        }
        millis = nowMillis
    }

    /**
     * 切换软键盘状态
     */
    @JvmStatic
    fun toggleSoftInput() {
        val imm = appCtx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.toggleSoftInput(0, 0)
    }

    /**
     * 判断软键盘是否可见
     * @param activity 目标Activity
     */
    @JvmStatic
    fun isSoftInputVisible(activity: Activity?): Boolean {
        return activity?.window?.let { getDecorViewInvisibleHeight(it) }?.takeIf { it > 0 } != null
    }

    private fun getDecorViewInvisibleHeight(window: Window): Int {
        val decorView = window.decorView
        val outRect = Rect().apply { decorView.getWindowVisibleDisplayFrame(this) }
        val delta = (decorView.bottom - outRect.bottom).absoluteValue
        Log.d(TAG, "getDecorViewInvisibleHeight: $delta")

        return when {
            delta <= SizeTools.getNavigationBarHeight() + SizeTools.getStatusBarHeight() -> {
                sDecorViewDelta = delta
                0
            }

            else -> delta - sDecorViewDelta
        }
    }

    /**
     * 修复Android 5497 BUG（Activity版本）
     * @param activity 目标Activity
     */
    @JvmStatic
    fun fixAndroidBug5497(activity: Activity) = fixAndroidBug5497(activity.window)

    /**
     * 修复Android 5497 BUG（Window版本）
     * @param window 目标Window
     */
    @JvmStatic
    fun fixAndroidBug5497(window: Window) {
        window.apply {
            attributes = attributes.apply {
                softInputMode =
                    softInputMode and WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE.inv()
            }

            findViewById<FrameLayout>(android.R.id.content)?.let { contentView ->
                val contentViewChild = contentView.getChildAt(0) ?: return@let
                val paddingBottom = contentViewChild.paddingBottom
                var contentViewInvisibleHeight = getContentViewInvisibleHeight(this)

                contentView.viewTreeObserver.addOnGlobalLayoutListener {
                    val height = getContentViewInvisibleHeight(this)
                    if (contentViewInvisibleHeight != height) {
                        contentViewChild.setPadding(
                            contentViewChild.paddingLeft,
                            contentViewChild.paddingTop,
                            contentViewChild.paddingRight,
                            paddingBottom + getDecorViewInvisibleHeight(this)
                        )
                        contentViewInvisibleHeight = height
                    }
                }
            }
        }
    }

    private fun getContentViewInvisibleHeight(window: Window): Int {
        return window.findViewById<View>(android.R.id.content)?.let { contentView ->
            val outRect = Rect().apply { contentView.getWindowVisibleDisplayFrame(this) }
            val delta = (contentView.bottom - outRect.bottom).absoluteValue
            Log.d(TAG, "getContentViewInvisibleHeight: $delta")

            when {
                delta <= SizeTools.getStatusBarHeight() + SizeTools.getNavigationBarHeight() -> 0
                else -> delta
            }
        } ?: 0
    }

    /**
     * 修复输入法内存泄漏（Activity版本）
     * @param activity 目标Activity
     */
    @JvmStatic
    fun fixSoftInputLeaks(activity: Activity) = fixSoftInputLeaks(activity.window)

    /**
     * 修复输入法内存泄漏（Window版本）
     * @param window 目标Window
     */
    @JvmStatic
    fun fixSoftInputLeaks(window: Window) {
        val imm =
            appCtx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return

        arrayOf(
            "mLastSrvView",
            "mCurRootView",
            "mServedView",
            "mNextServedView"
        ).forEach { fieldName ->
            try {
                val field = InputMethodManager::class.java.getDeclaredField(fieldName).apply {
                    isAccessible = true
                }
                (field.get(imm) as? View)?.takeIf {
                    it.rootView == window.decorView.rootView
                }?.let {
                    field.set(imm, null)
                }
            } catch (e: Throwable) {
                // 忽略反射异常
            }
        }
    }

}