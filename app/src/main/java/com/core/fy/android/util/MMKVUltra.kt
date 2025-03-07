package com.core.fy.android.util

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVLogLevel
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal val MMKVs by lazy { MMKVUltra.getInstance() }

class MMKVUltra private constructor(
    val mmkv: MMKV,
    private val config: MMKVConfig
) {
    companion object {
        private val instances = ConcurrentHashMap<String, MMKVUltra>()
        private lateinit var globalConfig: MMKVConfig

        /**
         * 初始化全局配置
         * @param context Context对象
         * @param config 全局配置项（可选）
         */
        @JvmStatic
        @JvmOverloads
        fun init(context: Context, config: MMKVConfig = MMKVConfig()) {
            globalConfig = config
            MMKV.initialize(context, config.rootDir)
            applyGlobalSettings()
        }

        /**
         * 获取MMKV实例（带自定义配置）
         * @param mmapID 存储实例ID（默认全局实例）
         * @param config 自定义配置（可选）
         */
        @JvmStatic
        @JvmOverloads
        fun getInstance(mmapID: String = "global_mmkv", config: MMKVConfig = globalConfig): MMKVUltra {
            return instances.getOrPut(mmapID) {
                val kv = when {
                    config.cryptKey.isNotEmpty() -> MMKV.mmkvWithID(
                        mmapID,
                        config.mode,
                        config.cryptKey,
                        config.rootDir ?: globalConfig.rootDir
                    )
                    else -> MMKV.mmkvWithID(mmapID, config.mode)
                }

                applyInstanceSettings(kv, config)
                MMKVUltra(kv, config)
            }
        }

        private fun applyGlobalSettings() {
            with(globalConfig) {
                MMKV.setLogLevel(logLevel)
            }
        }

        private fun applyInstanceSettings(kv: MMKV, config: MMKVConfig) {
            kv.reKey(config.cryptKey)
        }
    }

    // region 核心操作方法
    fun put(key: String, value: Any?) = value?.let {
        when (it) {
            is String -> mmkv.encode(key, it)
            is Int -> mmkv.encode(key, it)
            is Long -> mmkv.encode(key, it)
            is Float -> mmkv.encode(key, it)
            is Double -> mmkv.encode(key, it)
            is Boolean -> mmkv.encode(key, it)
            is ByteArray -> mmkv.encode(key, it)
            else -> throw IllegalArgumentException("Unsupported type: ${it.javaClass.name}")
        }
    } ?: mmkv.removeValueForKey(key)

    inline fun <reified T> get(key: String): T? = when (T::class) {
        String::class -> mmkv.decodeString(key) as? T
        Int::class -> mmkv.decodeInt(key, -1) as? T
        Long::class -> mmkv.decodeLong(key, -1L) as? T
        Float::class -> mmkv.decodeFloat(key, -1f) as? T
        Double::class -> mmkv.decodeDouble(key, -1.0) as? T
        Boolean::class -> mmkv.decodeBool(key, false) as? T
        ByteArray::class -> mmkv.decodeBytes(key) as? T
        else -> throw IllegalArgumentException("Unsupported type: ${T::class.java.name}")
    }

    fun contains(key: String) = mmkv.containsKey(key)
    fun clear() = mmkv.clearAll()
    // endregion

    // region 配置管理
    fun reconfigure(newConfig: MMKVConfig) {
        mmkv.reKey(newConfig.cryptKey)
    }

    fun currentConfig() = config.copy()
    // endregion
}

/**
 * MMKV配置项
 * @property cryptKey 加密密钥（16/32字节）
 * @property mode 存储模式（默认单进程）
 * @property rootDir 自定义存储路径
 * @property logLevel 日志级别（默认INFO）
 */
data class MMKVConfig(
    val cryptKey: String = "",
    val mode: Int = MMKV.SINGLE_PROCESS_MODE,
    val rootDir: String? = null,
    val logLevel: MMKVLogLevel = MMKVLogLevel.LevelInfo,
) {
    fun isValidKey() = cryptKey.isEmpty() || cryptKey.toByteArray().let {
        it.size == 16 || it.size == 32
    }
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
