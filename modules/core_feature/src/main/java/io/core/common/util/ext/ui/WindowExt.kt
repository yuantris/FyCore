package io.core.common.util.ext.ui

import android.graphics.Color
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gyf.immersionbar.ImmersionBar

enum class BarColor {
    BLACK,
    WHITE
}

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

    // 初始化 ImmersionBar
    ImmersionBar.with(activity)
        .transparentBar()
        .statusBarDarkFont(statusBarColor == BarColor.BLACK)
        .navigationBarDarkIcon(navigationBarColor == BarColor.BLACK)
        .init()
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
