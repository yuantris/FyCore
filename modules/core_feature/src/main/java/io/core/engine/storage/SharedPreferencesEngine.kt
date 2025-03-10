package io.core.engine.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.reflect.KClass

// SharedPreferences实现
class SharedPreferencesEngine(private val prefs: SharedPreferences) : StorageEngine {
    override fun <T : Any> putData(key: String, value: T?, type: KClass<out T>) {
        prefs.edit() {
            when (type) {
                String::class -> putString(key, value as? String)
                Int::class -> putInt(key, value as? Int ?: 0)
                Boolean::class -> putBoolean(key, value as? Boolean ?: false)
                Float::class -> putFloat(key, value as? Float ?: 0f)
                Long::class -> putLong(key, value as? Long ?: 0L)
                else -> throw IllegalArgumentException("Unsupported type: ${type.simpleName}")
            }
        }
    }

    override fun <T : Any> getData(key: String, type: KClass<T>, default: T): T {
        return when (type) {
            String::class -> prefs.getString(key, default as? String) as T
            Int::class -> prefs.getInt(key, (default as? Int) ?: 0) as T
            Boolean::class -> prefs.getBoolean(key, (default as? Boolean) ?: false) as T
            Float::class -> prefs.getFloat(key, (default as? Float) ?: 0f) as T
            Long::class -> prefs.getLong(key, (default as? Long) ?: 0L) as T
            else -> throw IllegalArgumentException("Unsupported type: ${type.simpleName}")
        }
    }

    override fun remove(key: String) = prefs.edit() { remove(key) }
    override fun clear() = prefs.edit() { clear() }
    override fun contains(key: String) = prefs.contains(key)
    override fun getAllKeys(): Set<String> = prefs.all.keys
}