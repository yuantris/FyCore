package io.core.common.util.tools

import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object NavigationBarUtils {

    /**
     * 获取导航栏高度（实时获取）
     * @param view 需要附加到窗口的View（推荐使用Activity的decorView）
     * @param callback 获取结果回调
     */
    fun getNavigationBarHeight(view: View, callback: (height: Int) -> Unit) {
        when (DeviceOSUtils.deviceBrand) {
            DeviceOSUtils.DeviceBrand.XIAOMI -> {
                ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
                    val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    val height = navigationBars.bottom

                    // 处理手势导航情况
                    if (height == 0) {
                        callback(getNavigationBarHeightFromResources(view.context))
                    } else {
                        callback(height)
                    }

                    insets
                }
            }

            else -> {}
        }

    }

    /**
     * 从系统资源获取导航栏高度（静态值）
     */
    private fun getNavigationBarHeightFromResources(context: Context): Int {
        val resourceId = context.resources.getIdentifier(
            "navigation_bar_height",
            "dimen",
            "android"
        )
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    /**
     * 兼容旧版系统的获取方式
     */
    @Deprecated("For legacy systems")
    fun getLegacyNavigationBarHeight(view: View): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.rootWindowInsets?.getInsets(WindowInsets.Type.navigationBars())?.bottom ?: 0
        } else {
            val insets = ViewCompat.getRootWindowInsets(view)
            insets?.systemWindowInsetBottom ?: 0
        }
    }
}