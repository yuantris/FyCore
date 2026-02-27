package io.core.engine.storage

import android.content.SharedPreferences
import com.tencent.mmkv.MMKV
import kotlin.reflect.KClass

// MMKV实现
class MMKVEngine(private val mmkv: MMKV) : StorageEngine {
    override fun <T : Any> putData(key: String, value: T?, type: KClass<out T>) {
        when (type) {
            String::class -> mmkv.putString(key, value as? String)
            Int::class -> mmkv.putInt(key, value as? Int ?: 0)
            Boolean::class -> mmkv.putBoolean(key, value as? Boolean ?: false)
            Float::class -> mmkv.putFloat(key, value as? Float ?: 0f)
            Long::class -> mmkv.putLong(key, value as? Long ?: 0L)
            else -> throw IllegalArgumentException("Unsupported type: ${type.simpleName}")
        }
    }

    override fun <T : Any> getData(key: String, type: KClass<T>, default: T): T {
        return when (type) {
            String::class -> mmkv.getString(key, default as? String) as T
            Int::class -> mmkv.getInt(key, default as? Int ?: 0) as T
            Boolean::class -> mmkv.getBoolean(key, default as? Boolean ?: false) as T
            Float::class -> mmkv.getFloat(key, default as? Float ?: 0f) as T
            Long::class -> mmkv.getLong(key, default as? Long ?: 0L) as T
            else -> throw IllegalArgumentException("Unsupported type: ${type.simpleName}")
        }
    }

    override fun remove(key: String) = mmkv.removeValueForKey(key)
    override fun clear() = mmkv.clearAll()
    override fun contains(key: String) = mmkv.containsKey(key)
    override fun getAllKeys() = mmkv.allKeys()?.toSet() ?: emptySet()

    fun getMMKV(): MMKV {
        return mmkv
    }

    fun importFromSharedPreferences(preferences: SharedPreferences) {
        mmkv.importFromSharedPreferences(preferences)
    }
}