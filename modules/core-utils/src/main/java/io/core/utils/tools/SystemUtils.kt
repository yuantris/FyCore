package io.core.utils.tools

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.view.Display
import androidx.core.net.toUri
import io.core.base.appCtx
import io.core.utils.extensions.displayManager
import io.core.utils.extensions.powerManager
import io.core.utils.constant.ANDROID_6


object SystemUtils {

    @JvmStatic
    @SuppressLint("BatteryLife")
    fun ignoreBatteryOptimization(activity: Activity) {
        if (OSAir.lowerThan(ANDROID_6)) return

        val hasIgnored = powerManager.isIgnoringBatteryOptimizations(activity.packageName)
        //  判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框�?
        if (!hasIgnored) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = "package:${activity.packageName}".toUri()
                activity.startActivity(intent)
            } catch (ignored: Throwable) {
            }

        }
    }

    fun isScreenOn(): Boolean {
        return displayManager.displays.filterNotNull().any {
            it.state != Display.STATE_OFF
        }
    }

    /**
     * 屏幕像素宽度
     */
    @JvmStatic
    val screenWidthPx by lazy {
        appCtx.resources.displayMetrics.widthPixels
    }

    /**
     * 屏幕像素高度
     */
    @JvmStatic
    val screenHeightPx by lazy {
        appCtx.resources.displayMetrics.heightPixels
    }
}
