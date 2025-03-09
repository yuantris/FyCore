package com.core.fy.android.util

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import io.core.common.util.extensions.cool.GSON
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

val MMKVAir by lazy { MMKVHelper.getInstance() }

object MMKVHelper {

    private const val DEFAULT_INSTANCE_ID = "default_core_mmkv"
    @Volatile
    private var internalGson = GSON

    // 允许自定义Gson实例，建议在初始化阶段设置
    var gson: Gson
        get() = internalGson
        set(value) {
            internalGson = value
        }

    fun init(context: Context, rootDir: String? = null) {
        rootDir?.let { MMKV.initialize(context, it) } ?: MMKV.initialize(context)
    }

    fun getInstance(
        instanceId: String = DEFAULT_INSTANCE_ID,
        mode: Int = MMKV.SINGLE_PROCESS_MODE,
        cryptKey: String? = null
    ): MMKVWrapper {
        return MMKVWrapper(instanceId, mode, cryptKey)
    }

    class MMKVWrapper(
        private val instanceId: String,
        private val mode: Int,
        private val cryptKey: String?
    ) {
        private val mmkv: MMKV by lazy {
            when (cryptKey) {
                is String -> MMKV.mmkvWithID(instanceId, mode, cryptKey)
                else -> MMKV.mmkvWithID(instanceId, mode)
            }
        }

        // region 基本类型操作
        fun putInt(key: String, value: Int) = mmkv.encode(key, value)
        fun getInt(key: String, default: Int = 0) = mmkv.decodeInt(key, default)

        fun putLong(key: String, value: Long) = mmkv.encode(key, value)
        fun getLong(key: String, default: Long = 0L) = mmkv.decodeLong(key, default)

        fun putFloat(key: String, value: Float) = mmkv.encode(key, value)
        fun getFloat(key: String, default: Float = 0f) = mmkv.decodeFloat(key, default)

        fun putDouble(key: String, value: Double) = mmkv.encode(key, value)
        fun getDouble(key: String, default: Double = 0.0) = mmkv.decodeDouble(key, default)

        fun putBoolean(key: String, value: Boolean) = mmkv.encode(key, value)
        fun getBoolean(key: String, default: Boolean = false) = mmkv.decodeBool(key, default)

        fun putString(key: String, value: String?) = value?.let { mmkv.encode(key, it) } ?: remove(key)
        fun getString(key: String, default: String? = null) = mmkv.decodeString(key, default)

        fun putStringSet(key: String, value: Set<String>?) = value?.let { mmkv.encode(key, it) } ?: remove(key)
        fun getStringSet(key: String, default: Set<String> = emptySet()) = mmkv.decodeStringSet(key, default)

        fun putBytes(key: String, value: ByteArray?) = value?.let { mmkv.encode(key, it) } ?: remove(key)
        fun getBytes(key: String, default: ByteArray? = null) = mmkv.decodeBytes(key, default)
        // endregion

        // region 高级操作
        inline fun <reified T : Any> putObject(key: String, value: T?) {
            value?.let {
                try {
                    putString(key, gson.toJson(it))
                } catch (e: JsonIOException) {
                    Log.e("MMKVHelper", "序列化失败 key=$key", e)
                    remove(key)
                } catch (e: Exception) {
                    Log.e("MMKVHelper", "未知序列化错误 key=$key", e)
                    remove(key)
                }
            } ?: remove(key)
        }

        inline fun <reified T : Any> getObject(key: String): T? {
            return getString(key)?.let { json ->
                try {
                    gson.fromJson(json, T::class.java)
                } catch (e: JsonSyntaxException) {
                    Log.w("MMKVHelper", "反序列化失败 key=$key", e)
                    null
                } catch (e: Exception) {
                    Log.w("MMKVHelper", "未知反序列化错误 key=$key", e)
                    null
                }
            }
        }

        inline fun <reified T> putList(key: String, list: List<T>) {
            try {
                putString(key, gson.toJson(list))
            } catch (e: JsonIOException) {
                Log.e("MMKVHelper", "序列化列表失败 key=$key", e)
                remove(key)
            } catch (e: Exception) {
                Log.e("MMKVHelper", "未知序列化列表错误 key=$key", e)
                remove(key)
            }
        }

        inline fun <reified T> getList(key: String): List<T> {
            return getString(key)?.let { json ->
                val type = TypeToken.getParameterized(List::class.java, T::class.java).type
                try {
                    gson.fromJson<List<T>>(json, type)
                } catch (e: JsonSyntaxException) {
                    Log.w("MMKVHelper", "反序列化列表失败 key=$key", e)
                    emptyList()
                } catch (e: Exception) {
                    Log.w("MMKVHelper", "未知反序列化列表错误 key=$key", e)
                    emptyList()
                }
            } ?: emptyList()
        }

        inline fun <reified T : Enum<T>> putEnum(
            key: String,
            value: T,
            storeByName: Boolean = true
        ) {
            if (storeByName) {
                putString(key, value.name)
            } else {
                putInt(key, value.ordinal)
            }
        }

        inline fun <reified T : Enum<T>> getEnum(
            key: String,
            default: T,
            storeByName: Boolean = true
        ): T {
            return if (storeByName) {
                getString(key)?.let { name ->
                    try {
                        enumValueOf<T>(name)
                    } catch (e: IllegalArgumentException) {
                        Log.w("MMKVHelper", "无效的枚举值 key=$key value=$name", e)
                        default
                    }
                } ?: default
            } else {
                val ordinal = getInt(key, -1)
                enumValues<T>().getOrNull(ordinal) ?: default
            }
        }
        // endregion

        // region 通用操作
        fun contains(key: String) = mmkv.containsKey(key)
        fun remove(key: String) = mmkv.removeValueForKey(key)
        fun clearAll() = mmkv.clearAll()
        fun getAll() = mmkv.all?.toMap() ?: emptyMap<String, Any>()
        // endregion

        // region 属性委托
        inline fun <reified T : Any> delegate(
            key: String,
            defaultValue: T,
            crossinline validator: (T) -> Boolean = { true }
        ): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return when (T::class) {
                    Int::class -> getInt(key, defaultValue as Int) as T
                    Long::class -> getLong(key, defaultValue as Long) as T
                    Float::class -> getFloat(key, defaultValue as Float) as T
                    Double::class -> getDouble(key, defaultValue as Double) as T
                    Boolean::class -> getBoolean(key, defaultValue as Boolean) as T
                    String::class -> getString(key, defaultValue as? String) as T
                    ByteArray::class -> getBytes(key, defaultValue as? ByteArray) as T
                    else -> getObject<T>(key) ?: defaultValue
                }
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                if (!validator(value)) return
                when (value) {
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                    is ByteArray -> putBytes(key, value)
                    else -> putObject(key, value)
                }
            }
        }

        inline fun <reified T : Any> nullableDelegate(key: String): ReadWriteProperty<Any?, T?> =
            object : ReadWriteProperty<Any?, T?> {
                override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
                    return when {
                        T::class == String::class -> getString(key) as? T?
                        T::class == ByteArray::class -> getBytes(key) as? T?
                        else -> getObject(key)
                    }
                }

                override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
                    when (value) {
                        null -> remove(key)
                        is String -> putString(key, value)
                        is ByteArray -> putBytes(key, value)
                        else -> putObject(key, value)
                    }
                }
            }
        // endregion
    }
}