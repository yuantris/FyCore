package io.core.common.util.log

import io.core.appCtx
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.tools.toastOnUI

object AppLog {

    private val mLogs = arrayListOf<Triple<Long, String, Throwable?>>()

    val logs get() = mLogs.toList()

    @Synchronized
    fun put(message: String?, throwable: Throwable? = null, toast: Boolean = false) {
        message ?: return
        if (toast) {
            appCtx.toastOnUI(message)
        }
        if (mLogs.size > 100) {
            mLogs.removeLastOrNull()
        }
        mLogs.add(0, Triple(currentTimeMillis, message, throwable))
        LogCat.e(message, TAG, throwable)
    }

    @Synchronized
    fun clear() {
        mLogs.clear()
    }

    fun putDebug(message: String?, throwable: Throwable? = null) {
        put(message, throwable)
    }

}