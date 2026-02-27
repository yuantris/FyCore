package io.core.utils.extensions.ui

import android.graphics.Color
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gyf.immersionbar.ImmersionBar
import io.core.utils.tools.KeyboardTools

enum class BarColor {
    BLACK,
    WHITE
}

enum class ScreenOrientation {
    VERTICAL,
    HORIZONTAL
}

val WindowInsetsCompat.navigationBarHeight
    get() = (getInsets(WindowInsetsCompat.Type.systemBars()).bottom - imeHeight).coerceAtLeast(0)

val WindowInsetsCompat.imeHeight
    get() = getInsets(WindowInsetsCompat.Type.ime()).bottom

fun Window.setupImmersiveBars(
    activity: AppCompatActivity,
    statusBarColor: BarColor,
    navigationBarColor: BarColor
) {

    // 处理窗口 insets
    ViewCompat.setOnApplyWindowInsetsListener(decorView) { v, insets ->
        val navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        v.setPadding(0, 0, 0, navigationBarHeight)
        insets
    }

    // 初始�?ImmersionBar
    ImmersionBar.with(activity)
        .transparentBar()
        .statusBarDarkFont(statusBarColor == BarColor.BLACK)
        .navigationBarDarkIcon(navigationBarColor == BarColor.BLACK)
        .init()
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

fun Window.transparentStatusBar() {
    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    var systemUiVisibility = decorView.systemUiVisibility
    systemUiVisibility =
        systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    decorView.systemUiVisibility = systemUiVisibility
    statusBarColor = Color.TRANSPARENT
    navigationBarColor = Color.TRANSPARENT

    // Set status bar text color
    setStatusBarTextColor(false)
}

fun Window.setStatusBarTextColor(light: Boolean) {
    var systemUiVisibility = decorView.systemUiVisibility
    systemUiVisibility = if (light) { // White text
        systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    } else { // Black text
        systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
    decorView.systemUiVisibility = systemUiVisibility
}

fun Window.hideSoftInput() {
    KeyboardTools.hideSoftInput(this)
}
