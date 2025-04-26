package io.core.common.util.log

/** 拦截日志 */
interface LogHook {
    fun hook(info: LogInfo)
}