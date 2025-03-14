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
    var POSITIVE_COLOR = accentColor
        private set  // 限制直接修改
    var NEGATIVE_COLOR = accentColor
        private set

    /*运行环境*/
    @JvmStatic
    var DEBUG = BuildConfig.DEBUG && appCtx.isDebuggable

    @JvmStatic
    var RELEASE = !DEBUG

    @JvmStatic
    var CRASH_AFTER_JUMP: Class<*>? = null // 设置闪退后要跳转的Activity
        private set  // 限制直接设置

    @JvmStatic
    var CRASH_MULTI_PROCESS = false // 设置是否允许多进程闪退
        private set  // 限制直接设置

    // 新增 DSL 配置方法
    @JvmStatic
    fun configure(block: ConfigBuilder.() -> Unit) {
        ConfigBuilder().apply(block).applyToConfig()
    }

    class ConfigBuilder {
        var crashMultiProcess: Boolean = CRASH_MULTI_PROCESS
        var crashAfterJump: Class<*>? = CRASH_AFTER_JUMP
        var positiveColor: Int = POSITIVE_COLOR
        var negativeColor: Int = NEGATIVE_COLOR

        fun applyToConfig() {
            CRASH_MULTI_PROCESS = crashMultiProcess
            CRASH_AFTER_JUMP = crashAfterJump
            POSITIVE_COLOR = positiveColor
            NEGATIVE_COLOR = negativeColor
        }
    }
}