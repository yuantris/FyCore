@file:Suppress("AnnotateVersionCheck")

package io.core.common.util.tools

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

val isAndroid15Plus
    get() = OsUtils.atLeastV()
val isAndroid14Plus
    get() = OsUtils.atLeastU()
val isAndroid13Plus
    get() = OsUtils.atLeastS()
val isAndroid12Plus
    get() = OsUtils.atLeastS()
val isAndroid11Plus
    get() = OsUtils.atLeastR()
val isAndroid10Plus
    get() = OsUtils.atLeastQ()
val isAndroid9Plus
    get() = OsUtils.atLeastP()
val isAndroid8Plus
    get() = OsUtils.atLeastO()

object OsUtils {
    /**
     * 检查是否至少是 Android 15 (Vanilla Ice Cream, API 35)
     */
    // @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @JvmStatic
    fun atLeastV(): Boolean {
        return Build.VERSION.SDK_INT >= 35
    }

    /**
     * 检查是否至少是 Android 14 (Upside Down Cake, API 34)
     */
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun atLeastU(): Boolean {
        return Build.VERSION.SDK_INT >= 34
    }

    /**
     * 检查是否至少是 Android 13 (Tiramisu, API 33)
     */
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun atLeastT(): Boolean {
        return Build.VERSION.SDK_INT >= 33
    }

    /**
     * 检查是否至少是 Android 12 (S, API 31)
     */
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun atLeastS(): Boolean {
        return Build.VERSION.SDK_INT >= 31
    }

    /**
     * 检查是否至少是 Android 11 (R, API 30)
     */
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun atLeastR(): Boolean {
        return Build.VERSION.SDK_INT >= 30
    }

    /**
     * 检查是否至少是 Android 10 (Q, API 29)
     */
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    fun atLeastQ(): Boolean {
        return Build.VERSION.SDK_INT >= 29
    }

    /**
     * 检查是否至少是 Android 9 (Pie, API 28)
     */
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    fun atLeastP(): Boolean {
        return Build.VERSION.SDK_INT >= 28
    }

    /**
     * 检查是否至少是 Android 8 (Oreo, API 26)
     */
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    fun atLeastO(): Boolean {
        return Build.VERSION.SDK_INT >= 26
    }

    /**
     * 检查是否高于指定的 API 版本
     * @param api 目标 API 版本号
     */
    @JvmStatic
    fun higherThan(api: Int): Boolean {
        return Build.VERSION.SDK_INT > api
    }
}
