package com.core.fy.android.core.mmkv

import android.os.Parcelable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class MMKVDelegate<T>(
    private val key: String? = null,          // null 时使用属性名作为 key
    private val default: T                    // 默认值
) : ReadWriteProperty<MMKVProvider, T> {

    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
    override fun getValue(
        thisRef: MMKVProvider,
        property: KProperty<*>
    ): T = with(thisRef.mmkv) {
        val k = key ?: property.name
        when (default) {
            is Boolean -> decodeBool(k, default)
            is Int     -> decodeInt(k, default)
            is Long    -> decodeLong(k, default)
            is Float   -> decodeFloat(k, default)
            is Double  -> decodeDouble(k, default)
            is String  -> decodeString(k, default) ?: default
            is Set<*>  -> decodeStringSet(k, default as Set<String>) ?: default
            is Parcelable -> (default as? Parcelable)?.let { decodeParcelable(k, it::class.java) } ?: default
            else -> throw IllegalArgumentException("unsupported type ${default!!::class.java}")
        }
    } as T

    override fun setValue(
        thisRef: MMKVProvider,
        property: KProperty<*>,
        value: T
    ): Unit = with(thisRef.mmkv) {
        val k = key ?: property.name
        when (value) {
            is Boolean -> encode(k, value)
            is Int     -> encode(k, value)
            is Long    -> encode(k, value)
            is Float   -> encode(k, value)
            is Double  -> encode(k, value)
            is String  -> encode(k, value)
            is Set<*>  -> encode(k, value as Set<String>)
            is Parcelable -> encode(k, value)
            else -> throw IllegalArgumentException("unsupported type ${value!!::class.java}")
        }
    }
}