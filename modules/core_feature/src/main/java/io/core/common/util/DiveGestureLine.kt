package io.core.common.util

import android.graphics.Color
import android.view.View
import android.view.Window
import android.view.WindowManager
import io.core.common.util.extensions.ui.setPaddingBottom
import io.core.common.util.tools.SizeTools
import io.core.constant.DeviceOS


/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/11 14:02
 * @description
 * @author Yuan
 */

fun View.processNavigationBar() {
    val navigationBarHeight = SizeTools.getNavigationBarHeight()
    when (DeviceOS.romName) {
        DeviceOS.Rom.HyperOS -> {
            this.setPaddingBottom(navigationBarHeight)
        }

        DeviceOS.Rom.ColorOS -> {
            this.setPaddingBottom(navigationBarHeight)
        }

        else -> {}
    }
}

object DiveGestureLine {

    @JvmStatic
    fun setImmerse(window: Window) {
        if (DeviceOS.isHyperOS) {
            adaptXiaomi(window)
        } else if (DeviceOS.isColorOS) {
            adaptOPPO(window)
        }
    }

    @JvmOverloads
    fun adaptXiaomi(
        window: Window,
        navigationBarColor: Int = 0
    ) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (navigationBarColor == 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)      //设置沉浸式状态栏，在MIUI系统中，状态栏背景透明。原生系统中，状态栏背景半透明。
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)   //设置沉浸式虚拟键，在MIUI系统中，虚拟键背景透明。原生系统中，虚拟键背景半透明。
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.navigationBarColor = navigationBarColor
        }

    }

    fun adaptOPPO(window: Window) {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT // 状态栏透明
        window.navigationBarColor = Color.TRANSPARENT // 虚拟键栏透明
    }
}