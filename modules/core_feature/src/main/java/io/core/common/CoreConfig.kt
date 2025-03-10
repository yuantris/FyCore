package io.core.common

import io.core.BuildConfig
import io.core.R
import io.core.appCtx
import io.core.common.helper.AppLifecycleTracker
import io.core.common.util.extensions.ui.getCompatColor
import io.core.common.util.extensions.ui.isDebuggable

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/20 18:01
 * @description
 * @author Yuan
 */
object CoreConfig {
    private val accentColor = appCtx.getCompatColor(R.color.common_accent_color)

    /*AndroidAlertBuilder的按钮色值*/
    var alert_positive_color = accentColor
    var alert_negative_color = accentColor

    /*运行环境*/
    @JvmStatic
    var DEBUG = BuildConfig.DEBUG && appCtx.isDebuggable

    @JvmStatic
    var RELEASE = !DEBUG

    @JvmStatic
    var CRASH_AFTER_JUMP: Class<*>? = null // 设置闪退后要跳转的Activity
        @JvmName("setCrashAfterJumpPage")
        set

    @JvmStatic
    var CRASH_MULTI_PROCESS = false // 设置是否允许多进程闪退
}