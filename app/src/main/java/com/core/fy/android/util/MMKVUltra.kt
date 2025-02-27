@file:Suppress("UNCHECKED_CAST")

package com.core.fy.android.util

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

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

interface IMMKVOwner {
    val mmapID: String
    val kv: MMKV
}

open class MMKVOwner(override val mmapID: String) : IMMKVOwner {
    override val kv: MMKV by lazy { MMKV.mmkvWithID(mmapID) }
}

fun IMMKVOwner.mmkvInt(default: Int = 0) =
    MMKVProperty({ kv.decodeInt(it, default) }, { kv.encode(first, second) })

fun IMMKVOwner.mmkvLong(default: Long = 0L) =
    MMKVProperty({ kv.decodeLong(it, default) }, { kv.encode(first, second) })

fun IMMKVOwner.mmkvBool(default: Boolean = false) =
    MMKVProperty({ kv.decodeBool(it, default) }, { kv.encode(first, second) })

fun IMMKVOwner.mmkvFloat(default: Float = 0f) =
    MMKVProperty({ kv.decodeFloat(it, default) }, { kv.encode(first, second) })

fun IMMKVOwner.mmkvDouble(default: Double = 0.0) =
    MMKVProperty({ kv.decodeDouble(it, default) }, { kv.encode(first, second) })

fun IMMKVOwner.mmkvString() =
    MMKVProperty({ kv.decodeString(it) }, { kv.encode(first, second) })

fun IMMKVOwner.mmkvString(default: String) =
    MMKVProperty({ kv.decodeString(it) ?: default }, { kv.encode(first, second) })

fun IMMKVOwner.mmkvStringSet() =
    MMKVProperty({ kv.decodeStringSet(it) }, { kv.encode(first, second) })

fun IMMKVOwner.mmkvStringSet(default: Set<String>) =
    MMKVProperty({ kv.decodeStringSet(it) ?: default }, { kv.encode(first, second) })

fun IMMKVOwner.mmkvBytes() =
    MMKVProperty({ kv.decodeBytes(it) }, { kv.encode(first, second) })

fun IMMKVOwner.mmkvBytes(default: ByteArray) =
    MMKVProperty({ kv.decodeBytes(it) ?: default }, { kv.encode(first, second) })

inline fun <reified T : Parcelable> IMMKVOwner.mmkvParcelable() =
    MMKVProperty({ kv.decodeParcelable(it, T::class.java) }, { kv.encode(first, second) })

inline fun <reified T : Parcelable> IMMKVOwner.mmkvParcelable(default: T) =
    MMKVProperty({ kv.decodeParcelable(it, T::class.java) ?: default }, { kv.encode(first, second) })

fun <V> MMKVProperty<V>.asLiveData() = object : ReadOnlyProperty<IMMKVOwner, MutableLiveData<V>> {
    private var cache: MutableLiveData<V>? = null

    override fun getValue(thisRef: IMMKVOwner, property: KProperty<*>): MutableLiveData<V> =
        cache ?: object : MutableLiveData<V>() {
            override fun getValue() = this@asLiveData.getValue(thisRef, property)

            override fun setValue(value: V) {
                if (super.getValue() == value) return
                this@asLiveData.setValue(thisRef, property, value)
                super.setValue(value)
            }

            override fun onActive() = super.setValue(value)
        }.also { cache = it }
}

fun <V> MMKVProperty<V>.asFlow() = object : ReadOnlyProperty<IMMKVOwner, MutableSharedFlow<V>> {
    private val cache = MutableSharedFlow<V>()
    override fun getValue(thisRef: IMMKVOwner, property: KProperty<*>): MutableSharedFlow<V> {
        return cache
    }
}


class MMKVProperty<V>(
    private val decode: (String) -> V,
    private val encode: Pair<String, V>.() -> Boolean,
    var key: String? = null
) : ReadWriteProperty<IMMKVOwner, V> {

    override fun getValue(thisRef: IMMKVOwner, property: KProperty<*>): V =
        decode(key ?: property.name)

    override fun setValue(thisRef: IMMKVOwner, property: KProperty<*>, value: V) {
        encode((key ?: property.name) to value)
    }
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