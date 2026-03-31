package io.core.engine.location

import android.util.Log

internal object LocationLogger {
    private const val TAG = "LocationKit"
    private var debugEnabled = false

    fun setDebugEnabled(enabled: Boolean) {
        debugEnabled = enabled
    }

    fun debug(message: String) {
        if (debugEnabled) {
            Log.d(TAG, message)
        }
    }

    fun error(message: String) {
        Log.e(TAG, message)
    }

    fun error(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }
}
