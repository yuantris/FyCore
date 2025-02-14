@file:Suppress("UNCHECKED_CAST")

package io.core.common.util.tools

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import io.core.appCtx

object Preferences {

    val sp: SharedPreferences by lazy {
        // appCtx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        PreferenceManager.getDefaultSharedPreferences(appCtx)
    }

    fun <T> getValue(name: String, default: T): T = with(sp) {
        val res: Any = when (default) {
            is Long -> getLong(name, default)
            is String -> getString(name, default).orEmpty()
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            else -> throw java.lang.IllegalArgumentException()
        }
        res as T
    }

    fun <T> putValue(name: String, value: T) = with(sp.edit()) {
        when (value) {
            is Long -> putLong(name, value)
            is String -> putString(name, value)
            is Int -> putInt(name, value)
            is Boolean -> putBoolean(name, value)
            is Float -> putFloat(name, value)
            else -> throw IllegalArgumentException("This type can't be saved into Preferences")
        }.apply()
    }

    fun clear() {
        sp.edit().clear().apply()
    }
}