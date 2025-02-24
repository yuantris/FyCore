package io.core.common.util

import android.view.View
import android.view.Window
import android.view.WindowManager
import com.gyf.immersionbar.ktx.navigationBarHeight
import io.core.appCtx
import io.core.common.util.extensions.ui.setPaddingBottom
import io.core.common.util.tools.DeviceOSUtils

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
    when(DeviceOSUtils.deviceBrand){
        DeviceOSUtils.DeviceBrand.XIAOMI -> {
            this.setPaddingBottom(appCtx.navigationBarHeight)
        }

        else -> {}
    }
}

object DiveGestureLine {

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
}