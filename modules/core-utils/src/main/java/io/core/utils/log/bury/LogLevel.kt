package io.core.utils.log.bury

import androidx.annotation.IntDef

@IntDef(LogLevel.VERBOSE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR)
@Retention(AnnotationRetention.SOURCE)
annotation class LogLevel {
    companion object {
        const val VERBOSE = 0
        const val DEBUG = 1
        const val INFO = 2
        const val WARN = 3
        const val ERROR = 4
    }
}