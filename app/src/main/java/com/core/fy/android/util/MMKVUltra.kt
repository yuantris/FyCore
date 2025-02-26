@file:Suppress("UNCHECKED_CAST")

package com.core.fy.android.util

import android.content.Context
import com.tencent.mmkv.MMKV
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

internal val MMKVs by lazy { MMKVUltra.getInstance() }

class MMKVUltra private constructor(val mmkv: MMKV) {

    companion object {
        private val instances = ConcurrentHashMap<Pair<String, String?>, MMKVUltra>()
        private var isInitialized = false

        @JvmStatic
        @JvmOverloads
        fun init(
            context: Context,
            rootDir: String? = null,
            defaultCryptKey: String? = null
        ) {
            if (!isInitialized) {
                rootDir?.let { MMKV.initialize(context, it) } ?: MMKV.initialize(context)
                defaultCryptKey?.let { MMKV.defaultMMKV(MMKV.SINGLE_PROCESS_MODE, it) }
                isInitialized = true
            }
        }

        @JvmStatic
        @JvmOverloads
        fun getInstance(
            mmapID: String = "MMKV_FyCore",
            cryptKey: String? = null
        ): MMKVUltra {
            check(isInitialized) { "MMKVUltra must be initialized first" }

            return instances.getOrPut(Pair(mmapID, cryptKey)) {
                val kv = cryptKey?.let {
                    MMKV.mmkvWithID(mmapID, MMKV.SINGLE_PROCESS_MODE, it)
                } ?: MMKV.mmkvWithID(mmapID)
                MMKVUltra(kv)
            }
        }
    }

    // 泛型存取方法
    inline fun <reified T : Any> put(key: String, value: T) {
        when (T::class) {
            String::class -> mmkv.encode(key, value as String)
            Int::class -> mmkv.encode(key, value as Int)
            Long::class -> mmkv.encode(key, value as Long)
            Float::class -> mmkv.encode(key, value as Float)
            Double::class -> mmkv.encode(key, value as Double)
            Boolean::class -> mmkv.encode(key, value as Boolean)
            ByteArray::class -> mmkv.encode(key, value as ByteArray)
            else -> throw IllegalArgumentException("Unsupported type: ${T::class.java.name}")
        }
    }

    inline fun <reified T : Any> get(key: String, defaultValue: T): T {
        return when (T::class) {
            String::class -> mmkv.decodeString(key, defaultValue as String) as T
            Int::class -> mmkv.decodeInt(key, defaultValue as Int) as T
            Long::class -> mmkv.decodeLong(key, defaultValue as Long) as T
            Float::class -> mmkv.decodeFloat(key, defaultValue as Float) as T
            Double::class -> mmkv.decodeDouble(key, defaultValue as Double) as T
            Boolean::class -> mmkv.decodeBool(key, defaultValue as Boolean) as T
            ByteArray::class -> mmkv.decodeBytes(key, defaultValue as ByteArray) as T
            else -> throw IllegalArgumentException("Unsupported type: ${T::class.java.name}")
        }
    }

    // 带类型推断的快捷方法
    inline fun <reified T : Any> get(key: String): T? {
        return if (containsKey(key)) {
            get(key, T::class.getDefaultValue())
        } else {
            null
        }
    }

    // 类型安全校验的默认值获取
    fun <T : Any> KClass<T>.getDefaultValue(): T {
        return when (this.java) {
            String::class.java -> "" as T
            Int::class.java -> 0 as T
            Long::class.java -> 0L as T
            Float::class.java -> 0f as T
            Double::class.java -> 0.0 as T
            Boolean::class.java -> false as T
            ByteArray::class.java -> byteArrayOf() as T
            else -> throw IllegalArgumentException("Unsupported type: ${this.java.name}")
        }
    }

    // 常用扩展方法
    fun containsKey(key: String) = mmkv.containsKey(key)
    fun getAllKeys() = mmkv.allKeys()?.toSet() ?: emptySet()
    fun remove(key: String) = mmkv.removeValueForKey(key)
    fun clearAll() = mmkv.clearAll()
    fun sync() = mmkv.sync()
}

// 扩展函数增强易用性
inline fun <reified T : Any> MMKVUltra.put(key: String, value: T?) {
    if (value != null) {
        put(key, value)
    } else {
        remove(key)
    }
}

inline fun <reified T : Any> MMKVUltra.getOrPut(key: String, defaultValue: () -> T): T {
    return if (containsKey(key)) {
        get(key, defaultValue())
    } else {
        val value = defaultValue()
        put(key, value)
        value
    }
}