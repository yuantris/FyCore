package io.core.utils.tools

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowManager
import android.view.WindowMetrics
import io.core.base.appCtx
import kotlin.math.sqrt

/**
 * 屏幕尺寸及单位转换全能工具集
 * 1. 屏幕信息获取（像�?DP/SP�?
 * 2. 系统组件尺寸（状态栏/导航栏）
 * 3. 单位转换（dp/px/sp互转�?
 * 4. 屏幕参数（密�?DPI/物理尺寸�?
 */
object SizeTools {

    /* ======================================================= */
    /*                      屏幕基本信息                        */
    /* ======================================================= */

    /**
     * 获取屏幕宽度（像素，使用应用级Context�?
     * 注意：可能包含系统装饰（如状态栏/导航栏）
     */
    @JvmStatic
    fun getScreenWidth(): Int {
        return getScreenSize(appCtx).first
    }

    /**
     * 获取屏幕高度（像素，使用应用级Context�?
     * 注意：可能包含系统装饰（如状态栏/导航栏）
     */
    @JvmStatic
    fun getScreenHeight(): Int {
        return getScreenSize(appCtx).second
    }

    /**
     * 获取屏幕尺寸（像素，Pair<�? �?�?
     * 兼容API 30+ 的WindowMetrics方式
     */
    @JvmStatic
    fun getScreenSize(context: Context): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
            Pair(windowMetrics.bounds.width(), windowMetrics.bounds.height())
        } else {
            val display = windowManager.defaultDisplay
            val point = Point()
            display.getRealSize(point) // 获取包含系统装饰的尺�?
            Pair(point.x, point.y)
        }
    }

    /**
     * 获取可用显示区域高度（像素，排除状态栏/导航栏）
     * 适用于Activity上下�?
     */
    @JvmStatic
    fun getAvailableScreenHeight(activity: Activity): Int {
        val rect = Rect()
        activity.window.decorView.getWindowVisibleDisplayFrame(rect)
        return rect.height()
    }

    /* ======================================================= */
    /*                      系统组件尺寸                        */
    /* ======================================================= */

    /**
     * 获取状态栏高度（像素）
     * 原理：通过系统资源标识符获�?
     */
    @JvmStatic
    fun getStatusBarHeight(): Int {
        return getSystemComponentSize(appCtx, "status_bar_height")
    }

    /**
     * 获取导航栏高度（像素�?
     * 注意：部分设备可能不存在导航�?
     */
    @JvmStatic
    fun getNavigationBarHeight(): Int {
        return getSystemComponentSize(appCtx, "navigation_bar_height")
    }

    /**
     * 动态获取状态栏高度（更精确，需要Activity上下文）
     * 通过Window可见区域计算
     */
    @JvmStatic
    fun getStatusBarHeight(activity: Activity): Int {
        val rect = Rect()
        activity.window.decorView.getWindowVisibleDisplayFrame(rect)
        return rect.top
    }

    /**
     * 检测导航栏是否可见
     * 原理：比较真实高度与可用显示高度
     */
    @JvmStatic
    fun isNavigationBarVisible(activity: Activity): Boolean {
        val realHeight = getScreenHeight()
        val availableHeight = getAvailableScreenHeight(activity)
        return realHeight - availableHeight > getNavigationBarHeight()
    }

    /* ======================================================= */
    /*                      单位转换工具                        */
    /* ======================================================= */

    /**
     * DP转PX（支持负数）
     */
    @JvmStatic
    fun dp2px(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            appCtx.resources.displayMetrics
        ).toInt()
    }

    /**
     * PX转DP（支持负数）
     */
    @JvmStatic
    fun px2dp(px: Float): Float {
        val density = appCtx.resources.displayMetrics.density
        return px / density
    }

    /**
     * SP转PX（考虑字体缩放系数�?
     */
    @JvmStatic
    fun sp2px(sp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            appCtx.resources.displayMetrics
        ).toInt()
    }

    /**
     * PX转SP（考虑字体缩放系数�?
     */
    @JvmStatic
    fun px2sp(px: Float): Float {
        val scaledDensity = appCtx.resources.displayMetrics.scaledDensity
        return px / scaledDensity
    }

    /* ======================================================= */
    /*                      屏幕物理参数                        */
    /* ======================================================= */

    /**
     * 获取屏幕密度�?.75 / 1.0 / 1.5等）
     */
    @JvmStatic
    fun getDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    /**
     * 获取DPI等级�?20 / 160 / 240等）
     */
    @JvmStatic
    fun getDensityDpi(context: Context): Int {
        return context.resources.displayMetrics.densityDpi
    }

    /**
     * 获取屏幕物理尺寸（英寸）
     * 需要API 17+ 获取精确的XDpi/YDpi
     */
    @JvmStatic
    fun getScreenInch(context: Context): Double {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val widthInch = metrics.xdpi
        val heightInch = metrics.ydpi
        val widthPx = getScreenWidth().toFloat()
        val heightPx = getScreenHeight().toFloat()
        val widthInchSize = widthPx / widthInch
        val heightInchSize = heightPx / heightInch
        return sqrt(
            (widthInchSize * widthInchSize + heightInchSize * heightInchSize).toDouble()
        )
    }

    /* ======================================================= */
    /*                      内部工具方法                        */
    /* ======================================================= */

    // 统一获取系统组件尺寸
    private fun getSystemComponentSize(context: Context, resourceName: String): Int {
        return try {
            val resources = context.resources
            val resourceId = resources.getIdentifier(resourceName, "dimen", "android")
            if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        } catch (e: Exception) {
            0
        }
    }
}