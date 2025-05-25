package io.core.common

import io.core.BuildConfig
import io.core.R
import io.core.appCtx
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

    @JvmStatic
    var token: String = "FFGreatKing"
        private set

    /**
     * 一次性设置token值
     * @param newToken 新的token值
     * @throws IllegalStateException 如果token已被设置过
     */
    @JvmStatic
    fun setToken(newToken: String) {
        if (token != "FFGreatKing") {
            throw IllegalStateException("Token can only be set once")
        }
        token = newToken
    }

    // region 颜色配置 -------------------------------------------------------------------------------
    object Alert {
        private val defaultAccentColor = appCtx.getCompatColor(R.color.common_accent_color)

        var positiveColor = defaultAccentColor
            private set
        var negativeColor = defaultAccentColor
            private set

        internal fun applyFrom(builder: ConfigBuilder.AlertBuilder) {
            builder.positiveColor?.let { positiveColor = it }
            builder.negativeColor?.let { negativeColor = it }
        }
    }
    // endregion

    // region 环境配置 ----------------------------------------------------------------------------
    object Environment {
        @JvmStatic
        var isDebug = BuildConfig.DEBUG && appCtx.isDebuggable
            private set

        @JvmStatic
        val isRelease get() = !isDebug

        internal fun applyFrom(builder: ConfigBuilder.EnvironmentBuilder) {
            builder.isDebug?.let { isDebug = it }
        }
    }
    // endregion

    // region 崩溃配置 -------------------------------------------------------------------------------
    object Crash {
        @JvmStatic
        var afterJumpActivity: Class<*>? = null
            private set

        @JvmStatic
        var allowMultiProcess = false
            private set

        internal fun applyFrom(builder: ConfigBuilder.CrashBuilder) {
            builder.afterJumpActivity?.let { afterJumpActivity = it }
            builder.allowMultiProcess?.let { allowMultiProcess = it }
        }
    }
    // endregion

    // region DSL配置构建器 --------------------------------------------------------------------------
    @JvmStatic
    fun configure(block: ConfigBuilder.() -> Unit) {
        ConfigBuilder().apply(block).applyToConfig()
    }

    class ConfigBuilder {
        private val alertBuilder = AlertBuilder()
        private val debugBuilder = EnvironmentBuilder()
        private val crashBuilder = CrashBuilder()

        fun alert(block: AlertBuilder.() -> Unit) {
            alertBuilder.apply(block)
        }

        fun environment(block: EnvironmentBuilder.() -> Unit) {
            debugBuilder.apply(block)
        }

        fun crash(block: CrashBuilder.() -> Unit) {
            crashBuilder.apply(block)
        }

        internal fun applyToConfig() {
            Alert.applyFrom(alertBuilder)
            Environment.applyFrom(debugBuilder)
            Crash.applyFrom(crashBuilder)
        }

        inner class AlertBuilder {
            var positiveColor: Int? = null
            var negativeColor: Int? = null
        }

        inner class EnvironmentBuilder {
            var isDebug: Boolean? = null
        }

        inner class CrashBuilder {
            var afterJumpActivity: Class<*>? = null
            var allowMultiProcess: Boolean? = null
        }
    }
    // endregion
}