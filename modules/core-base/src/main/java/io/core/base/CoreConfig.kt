package io.core.base

/**
 * FyCore 全局配置类
 * 提供框架级别的配置管理
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

    // region 环境配置 ----------------------------------------------------------------------------
    object Environment {
        @JvmStatic
        var isDebug = true
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
        private val debugBuilder = EnvironmentBuilder()
        private val crashBuilder = CrashBuilder()

        fun environment(block: EnvironmentBuilder.() -> Unit) {
            debugBuilder.apply(block)
        }

        fun crash(block: CrashBuilder.() -> Unit) {
            crashBuilder.apply(block)
        }

        internal fun applyToConfig() {
            Environment.applyFrom(debugBuilder)
            Crash.applyFrom(crashBuilder)
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
