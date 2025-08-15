@file:Suppress("DEPRECATION")

package io.core.common.util.extensions.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.core.common.util.extensions.cool.isDarkColor
import io.core.common.util.tools.buildMainHandler
import io.core.common.util.tools.isAndroid11Plus
import io.core.common.util.tools.isAndroid6Plus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ========================================
// 1. Activity生命周期相关扩展
// ========================================

/**
 * 处理双击返回键退出应用的逻辑
 *
 * @param interval 两次点击的时间间隔，默认2000毫秒
 * @param promptMessage 提示消息，默认"再按一次退出APP"
 * @param onShowPrompt 显示提示消息的回调
 * @param onExit 退出应用的回调
 */
fun AppCompatActivity.handleDoubleBackPressExit(
    interval: Long = 2000,
    promptMessage: String = "再按一次退出APP",
    onShowPrompt: (String) -> Unit,
    onExit: () -> Unit
) {
    var backPressedTime = 0L
    val handler = buildMainHandler()
    val resetTask = Runnable { backPressedTime = 0 }

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val currentTime = System.currentTimeMillis()

            when {
                // 首次点击或超过间隔时间
                backPressedTime == 0L || currentTime - backPressedTime > interval -> {
                    onShowPrompt(promptMessage)
                    backPressedTime = currentTime
                    handler.postDelayed(resetTask, interval)
                }
                // 在间隔时间内第二次点击
                else -> {
                    handler.removeCallbacks(resetTask)
                    onExit()
                    backPressedTime = 0
                }
            }
        }
    }

    // 绑定Activity生命周期
    onBackPressedDispatcher.addCallback(this, callback)
}

/**
 * 检查Activity是否仍然存活（未被销毁或正在销毁）
 *
 * @return true表示Activity存活，false表示已销毁或正在销毁
 */
fun Activity.isAlive(): Boolean {
    return !(isFinishing || isDestroyed)
}

/**
 * 将Activity移到前台
 * 注意：需将launchMode设置为SingleTop，否则会创建新实例
 *
 * @param context 上下文对象
 */
fun Activity.moveTaskToFront(context: Context) {
    val intent = Intent(context, this.javaClass)
    intent.flags =
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
    context.startActivity(intent)
}

/**
 * 在Activity的onResume生命周期回调中执行指定代码块
 *
 * @param executeImmediatelyIfResumed 如果Activity已处于Resume状态，是否立即执行代码块
 * @param callback 要执行的代码块
 */
fun AppCompatActivity.onResumeCallback(
    executeImmediatelyIfResumed: Boolean = true,
    callback: () -> Unit
) {
    // 如果已经是Resume状态且需要立即执行
    if (executeImmediatelyIfResumed && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        callback.invoke()
        return
    }

    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            callback.invoke()
            lifecycle.removeObserver(this)
        }
    })
}

/**
 * 在Activity的生命周期中执行指定代码块
 *
 * @param event 要监听的生命周期事件
 * @param callback 要执行的代码块
 */
fun AppCompatActivity.onLifecycleEvent(
    event: Lifecycle.Event,
    callback: () -> Unit
) {
    lifecycle.addObserver(object : DefaultLifecycleObserver {

        override fun onCreate(owner: LifecycleOwner) {
            if (event == Lifecycle.Event.ON_CREATE) {
                callback.invoke()
                lifecycle.removeObserver(this)
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            if (event == Lifecycle.Event.ON_START) {
                callback.invoke()
                lifecycle.removeObserver(this)
            }
        }

        override fun onResume(owner: LifecycleOwner) {
            if (event == Lifecycle.Event.ON_RESUME) {
                callback.invoke()
                lifecycle.removeObserver(this)
            }
        }

        override fun onPause(owner: LifecycleOwner) {
            if (event == Lifecycle.Event.ON_PAUSE) {
                callback.invoke()
                lifecycle.removeObserver(this)
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            if (event == Lifecycle.Event.ON_STOP) {
                callback.invoke()
                lifecycle.removeObserver(this)
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                callback.invoke()
                lifecycle.removeObserver(this)
            }
        }

    })
}


// ========================================
// 2. Activity跳转相关扩展
// ========================================

/**
 * 启动Activity并且不使用过渡动画（泛型版本）
 *
 * @param finish 是否结束当前Activity，默认为true
 */
inline fun <reified T : Activity> Activity.startNoTransition(finish: Boolean = true) {
    startActivity(Intent(this, T::class.java))
    overridePendingTransition(0, 0)
    if (finish) finish()
}

/**
 * 启动Activity并且不使用过渡动画（Class版本）
 *
 * @param clazz 目标Activity的Class对象
 * @param finish 是否结束当前Activity，默认为true
 */
fun Activity.startNoTransition(clazz: Class<*>, finish: Boolean = true) {
    startActivity(Intent(this, clazz))
    overridePendingTransition(0, 0)
    if (finish) finish()
}

// ========================================
// 3. DialogFragment相关扩展
// ========================================

/**
 * 显示DialogFragment（泛型版本，支持参数传递）
 *
 * @param arguments 传递给DialogFragment的参数配置
 */
inline fun <reified T : DialogFragment> AppCompatActivity.showDialogFragment(
    arguments: Bundle.() -> Unit = {}
) {
    val dialog = T::class.java.newInstance()
    val bundle = Bundle()
    bundle.apply(arguments)
    dialog.arguments = bundle
    dialog.show(supportFragmentManager, T::class.simpleName)
}

/**
 * 显示DialogFragment（实例版本）
 *
 * @param dialogFragment 要显示的DialogFragment实例
 */
fun AppCompatActivity.showDialogFragment(dialogFragment: DialogFragment) {
    dialogFragment.show(supportFragmentManager, dialogFragment::class.simpleName)
}

// ========================================
// 4. 屏幕显示相关扩展
// ========================================

/**
 * 设置Activity为全屏模式
 * 兼容Android R及以上版本
 */
fun Activity.fullScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(true)
    }
    window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    window.clearFlags(
        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
    )
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
}

/**
 * 设置状态栏为亮色或暗色模式
 * 兼容Android 6.0及以上版本
 *
 * @param isLightBar true表示亮色状态栏（深色文字），false表示暗色状态栏（浅色文字）
 */
fun Activity.setLightStatusBar(isLightBar: Boolean) {
    if (isAndroid11Plus) {
        window.insetsController?.let {
            if (isLightBar) {
                it.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                it.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        }
    }
    if (isAndroid6Plus) {
        val decorView = window.decorView
        val systemUiVisibility = decorView.systemUiVisibility
        if (isLightBar) {
            decorView.systemUiVisibility =
                systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            decorView.systemUiVisibility =
                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }
}

/**
 * 控制屏幕常亮状态
 *
 * @param on true表示保持屏幕常亮，false表示取消屏幕常亮
 */
fun Activity.keepScreenOn(on: Boolean) {
    val isScreenOn =
        (window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
    if (on == isScreenOn) return
    if (on) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

// ========================================
// 5. NavigationBar相关属性扩展
// 注意：以下方法需要在View完全被绘制出来之后调用，否则判断不了
// 建议在onWindowFocusChanged()方法中调用可以得到正确的结果
// ========================================

/**
 * 获取NavigationBar视图对象
 *
 * @return NavigationBar的View对象，如果不存在则返回null
 */
val Activity.navigationBar: View?
    get() {
        val viewGroup = (window.decorView as? ViewGroup) ?: return null
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            val childId = child.id
            if (childId != View.NO_ID
                && resources.getResourceEntryName(childId) == "navigationBarBackground"
            ) {
                return child
            }
        }
        return null
    }

/**
 * 检查NavigationBar是否存在
 *
 * @return true表示NavigationBar存在，false表示不存在
 */
val Activity.isNavigationBarExist: Boolean
    get() = navigationBar != null

/**
 * 获取NavigationBar的高度
 *
 * @return NavigationBar的高度（像素），如果不存在则返回0
 */
val Activity.navigationBarHeight: Int
    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    get() {
        if (isNavigationBarExist) {
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return resources.getDimensionPixelSize(resourceId)
        }
        return 0
    }

/**
 * 获取NavigationBar的位置
 *
 * @return NavigationBar的Gravity位置，默认为Gravity.BOTTOM
 */
val Activity.navigationBarGravity: Int
    get() {
        val gravity = (navigationBar?.layoutParams as? FrameLayout.LayoutParams)?.gravity
        return gravity ?: Gravity.BOTTOM
    }

// ========================================
// 6. 状态栏自适应相关扩展
// ========================================

/**
 * 根据指定视图自适应状态栏颜色
 * 通过分析视图背景色的亮度来自动设置状态栏为亮色或暗色模式
 *
 * @param rootView 根视图，用于获取状态栏区域的颜色
 * @param targetView 目标视图，优先使用其背景色，如果为null则从rootView采样
 */
@SuppressLint("DiscouragedApi", "InternalInsetResource")
fun Activity.adaptStatusBarToView(rootView: View, targetView: View? = null) {
    // 监听视图变化
    rootView.doOnPreDraw {
        val statusBarHeight = resources.getIdentifier(
            "status_bar_height", "dimen", "android"
        ).takeIf { it > 0 }?.let { resources.getDimensionPixelSize(it) } ?: 0

        // 获取目标视图的背景颜色（优先使用指定视图）
        val color = targetView?.backgroundAsColor() ?: run {
            val location = IntArray(2)
            rootView.getLocationInWindow(location)
            rootView.getPixelColor(
                x = location[0] + rootView.width / 2,
                y = location[1] + statusBarHeight / 2
            )
        }

        // 计算亮度并设置状态栏模式
        val isDark = color.isDarkColor()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
        }
    }
}

/**
 * 根据当前Activity的decorView自适应状态栏颜色
 *
 * @param targetView 目标视图，优先使用其背景色，如果为null则从decorView采样
 */
fun Activity.adaptStatusBarToView(targetView: View? = null) {
    val rootView = this.window.decorView
    adaptStatusBarToView(rootView, targetView)
}

/**
 * 根据图片资源自适应状态栏颜色
 * 通过分析图片在状态栏区域的颜色来自动设置状态栏模式
 *
 * @param imageRes 图片资源ID
 */
@SuppressLint("DiscouragedApi", "InternalInsetResource")
fun AppCompatActivity.adaptStatusBarToImage(@DrawableRes imageRes: Int) {
    lifecycleScope.launch(Dispatchers.IO) {
        val bitmap = try {
            BitmapFactory.decodeResource(resources, imageRes)?.also {
                if (it.isRecycled) return@launch  // 防止重复回收
            }
        } catch (e: Exception) {
            return@launch
        } ?: return@launch

        withContext(Dispatchers.Main) {
            window.decorView.doOnPreDraw {
                try {
                    val statusBarHeight = resources.getDimensionPixelSize(
                        resources.getIdentifier("status_bar_height", "dimen", "android")
                    ).coerceAtLeast(0)

                    val samplingY = (statusBarHeight * 0.5f).toInt().coerceAtMost(bitmap.height - 1)
                    val color = bitmap.getPixel(bitmap.width / 2, samplingY)

                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !color.isDarkColor()
                    }
                } finally {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()  // 确保回收位图
                    }
                }
            }
        }
    }
}


