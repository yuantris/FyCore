package io.core.common.util.ext.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.WindowMetrics
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import io.core.common.util.tools.buildMainHandler
import io.core.other.CustomToast

inline fun <reified T : DialogFragment> AppCompatActivity.showDialogFragment(
    arguments: Bundle.() -> Unit = {}
) {
    @Suppress("DEPRECATION")
    val dialog = T::class.java.newInstance()
    val bundle = Bundle()
    bundle.apply(arguments)
    dialog.arguments = bundle
    dialog.show(supportFragmentManager, T::class.simpleName)
}

fun AppCompatActivity.showDialogFragment(dialogFragment: DialogFragment) {
    dialogFragment.show(supportFragmentManager, dialogFragment::class.simpleName)
}

val WindowManager.windowSize: DisplayMetrics
    get() {
        val displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = currentWindowMetrics
            val insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val windowWidth = windowMetrics.bounds.width()
            val windowHeight = windowMetrics.bounds.height()
            var insetsWidth = insets.left + insets.right
            var insetsHeight = insets.top + insets.bottom
            if (windowWidth > windowHeight) {
                val tmp = insetsWidth
                insetsWidth = insetsHeight
                insetsHeight = tmp
            }
            displayMetrics.widthPixels = windowWidth - insetsWidth
            displayMetrics.heightPixels = windowHeight - insetsHeight
        } else {
            @Suppress("DEPRECATION")
            defaultDisplay.getMetrics(displayMetrics)
        }
        return displayMetrics
    }

@Suppress("DEPRECATION")
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


@SuppressLint("ObsoleteSdkInt")
fun Activity.setLightStatusBar(isLightBar: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
    @Suppress("DEPRECATION")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

/////以下方法需要在View完全被绘制出来之后调用，否则判断不了,在比如 onWindowFocusChanged（）方法中可以得到正确的结果/////

/**
 * 返回NavigationBar
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
 * 返回NavigationBar是否存在
 */
val Activity.isNavigationBarExist: Boolean
    get() = navigationBar != null

/**
 * 返回NavigationBar高度
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
 * 返回navigationBar位置
 */
val Activity.navigationBarGravity: Int
    get() {
        val gravity = (navigationBar?.layoutParams as? FrameLayout.LayoutParams)?.gravity
        return gravity ?: Gravity.BOTTOM
    }

/////------------------------------------------------------------------------------------------------------/////

@Suppress("DEPRECATION")
fun Activity.startNoTransition(clazz: Class<*>, finish: Boolean = true) {
    startActivity(Intent(this, clazz))
    overridePendingTransition(0, 0)
    if (finish) finish()
}

/**
 * @return 上下文中的Activity对象
 */
fun Context.getActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

/**
 * 显示Toast
 */
fun Context.toast(message: String) {
    CustomToast.Builder(this)
        .setMessage(message)
        .setDuration(1800)
        .build()
        .show()
}

/**
 * 长显示Toast
 */
fun Context.toastLong(message: String) {
    CustomToast.Builder(this)
        .setMessage(message)
        .setDuration(3600)
        .build()
        .show()
}

/**
 * 将Activity移到前台，需将launchMode设置为SingleTop，否则会创建新实例
 */
fun Activity.moveTaskToFront(context: Context) {
    val intent = Intent(context, this.javaClass)
    intent.flags =
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
    context.startActivity(intent)
}

fun <T> T.postUI(action: () -> Unit) {

    // 创建一个Handler实例指向主线程的Looper
    val mainHandler = MainLooper.handler

    // 如果当前已经是主线程，直接执行action
    if (isMainThread()) {
        action()
        return
    }

    // 对于Fragment和Activity的特定检查
    when (this) {
        is Fragment -> {
            if (!isAdded || activity == null || activity!!.isFinishing) {
                return
            }
        }

        is Activity -> {
            if (isFinishing) {
                return
            }
        }
    }

    // 利用Handler将操作post到主线程执行
    mainHandler.post { action() }
}

fun <T> T.postDelayUI(duration: Long, action: () -> Unit) {
    val mainHandler = MainLooper.handler

    // 判断执行线程如果已经是主线程，直接使用Handler处理延迟
    if (isMainThread()) {
        mainHandler.postDelayed({ action() }, duration)
        return
    }

    // Fragment
    if (this is Fragment) {
        if (!isAdded) return
        val activity = activity ?: return
        if (activity.isFinishing) return
        mainHandler.postDelayed({ activity.runOnUiThread(action) }, duration)
        return
    }

    // Activity
    if (this is Activity) {
        if (isFinishing) return
        mainHandler.postDelayed({ runOnUiThread(action) }, duration)
        return
    }

    // 在子线程中，无需检查线程，直接使用Handler处理延迟
    mainHandler.postDelayed({ action() }, duration)
}

object MainLooper {
    val handler: Handler by lazy { buildMainHandler() }
}

fun isMainThread(): Boolean {
    return Looper.getMainLooper().thread == Thread.currentThread()
}
