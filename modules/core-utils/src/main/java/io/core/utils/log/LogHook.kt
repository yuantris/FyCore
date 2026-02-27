package io.core.utils.log

/** 拦截日志 */
interface LogHook {
    fun hook(info: LogInfo)
}