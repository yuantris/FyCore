@file:Suppress("UNCHECKED_CAST")

package io.core.common.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.core.appCtx
import io.core.constant.SP_NAME

/**
 * SharedPreferences 工具类，提供类型安全的持久化存储操作
 * 支持类型：Long, String, Int, Boolean, Float, Set<String>
 */
object Preferences {

    val sp: SharedPreferences by lazy {
        appCtx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 获取存储的值（带默认值）
     * @param key 存储键名
     * @param default 默认值（类型会自动推断）
     * @return 存储的值或默认值
     * @throws IllegalArgumentException 当不支持该类型时抛出异常
     */
    @JvmStatic
    fun <T> getValue(key: String?, default: T): T = with(sp) {
        if (key == null) {
            return@with default
        }
        val res: Any = when (default) {
            is Long -> getLong(key, default)
            is String -> getString(key, default).orEmpty() // 处理空字符串情况
            is Int -> getInt(key, default)
            is Boolean -> getBoolean(key, default)
            is Float -> getFloat(key, default)
            is Set<*> -> getStringSet(key, default as Set<String>)?.toSet() ?: default // 处理空集合情况
            else -> throw IllegalArgumentException("This type is not supported")
        }
        res as T
    }

    /**
     * 存储值到SharedPreferences
     * @param key 存储键名
     * @param value 要存储的值
     * @throws IllegalArgumentException 当不支持该类型时抛出异常
     */
    @JvmStatic
    fun <T> putValue(key: String?, value: T) = with(sp.edit()) {
        if (key == null) {
            return@with
        }
        when (value) {
            is Long -> putLong(key, value)
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Boolean -> putBoolean(key, value)
            is Float -> putFloat(key, value)
            is Set<*> -> putStringSet(key, value as Set<String>)
            else -> throw IllegalArgumentException("This type can't be saved into Preferences")
        }.apply()
    }

    /**
     * 删除指定键的存储项
     * @param key 要删除的键名
     */
    @JvmStatic
    fun remove(key: String) = sp.edit { remove(key) }


    /**
     * 清空所有存储项（谨慎使用）
     */
    @JvmStatic
    fun clear() = sp.edit { clear() }

    /**
     * 是否包含指定键
     */
    @JvmStatic
    fun contains(key: String): Boolean = sp.contains(key)

    /**
     * 批量写入键值对。默认异步 apply()
     */
    @JvmStatic
    fun putAll(vararg pairs: Pair<String?, Any>) {
        if (pairs.isEmpty()) return
        with(sp.edit()) {
            for ((k, v) in pairs) {
                if (k == null) continue
                when (v) {
                    is Long -> putLong(k, v)
                    is String -> putString(k, v)
                    is Int -> putInt(k, v)
                    is Boolean -> putBoolean(k, v)
                    is Float -> putFloat(k, v)
                    is Set<*> -> putStringSet(k, v as Set<String>)
                    else -> throw IllegalArgumentException("Unsupported type in putAll: ${v::class.java}")
                }.apply()
            }
        }
    }

    /**
     * 原子性地自增 Int 值（若不存在则从 0 开始）。
     */
    @JvmStatic
    fun incrementInt(key: String, delta: Int = 1): Int {
        synchronized(sp) {
            val current = sp.getInt(key, 0)
            val next = current + delta
            with(sp.edit()) {
                putInt(key, next).apply()
            }
            return next
        }
    }

    /**
     * 原子性地自增 Long 值（若不存在则从 0 开始）。
     */
    @JvmStatic
    fun incrementLong(key: String, delta: Long = 1L): Long {
        synchronized(sp) {
            val current = sp.getLong(key, 0L)
            val next = current + delta
            with(sp.edit()) {
                putLong(key, next).apply()
            }
            return next
        }
    }

}
