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
 * 注意：！！！null不被支持！！！
 */
object Preferences {

    val sp: SharedPreferences by lazy {
        appCtx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 获取存储的值（带默认值）
     * @param name 存储键名
     * @param default 默认值（类型会自动推断）
     * @return 存储的值或默认值
     * @throws IllegalArgumentException 当不支持该类型时抛出异常
     */
    @JvmStatic
    fun <T> getValue(name: String, default: T): T = with(sp) {
        val res: Any = when (default) {
            is Long -> getLong(name, default)
            is String -> getString(name, default).orEmpty() // 处理空字符串情况
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            is Set<*> -> getStringSet(name, default as Set<String>)?.toSet() ?: default // 处理空集合情况
            else -> throw IllegalArgumentException("This type is not supported")
        }
        res as T
    }

    /**
     * 存储值到SharedPreferences
     * @param name 存储键名
     * @param value 要存储的值
     * @throws IllegalArgumentException 当不支持该类型时抛出异常
     */
    @JvmStatic
    fun <T> putValue(name: String, value: T) = with(sp.edit()) {
        when (value) {
            is Long -> putLong(name, value)
            is String -> putString(name, value)
            is Int -> putInt(name, value)
            is Boolean -> putBoolean(name, value)
            is Float -> putFloat(name, value)
            is Set<*> -> putStringSet(name, value as Set<String>)
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

}
