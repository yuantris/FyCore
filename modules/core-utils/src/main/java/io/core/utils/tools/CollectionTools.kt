@file:Suppress("UNCHECKED_CAST")

package io.core.utils.tools

import io.core.utils.extensions.cool.jsonToMap


/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/2/14 8:49
 * @description
 * @author Yuan
 */

object CollectionTools {
    @JvmStatic
    fun <K, V> mapValuesToList(map: Map<K, V>): List<V> {
        return map.values.toList()
    }

    @JvmStatic
    fun <K, V> mapKeysToList(map: Map<K, V>): List<K> {
        return map.keys.toList()
    }


    // 添加类型安全的重载版�?
    @JvmStatic
    fun <V> jsonToMap(jsonStr: String?): Map<String, V> {
        return jsonStrToMap(jsonStr) as? Map<String, V> ?: emptyMap()
    }

    /**
     * 在给定的 Map 中查找第一个与指定值关联的�?
     *
     * @param map 要搜索的键值对集合，类型为 [Map]<[K], [V]>
     * @param value 需要查找的目标值，类型�?[V]
     * @return 第一个匹配项的键，如果未找到匹配项则返回 `null`
     *
     * @param K Map 键的类型
     * @param V Map 值的类型，必须实现相等性判�?
     */
    @JvmStatic
    fun <K, V> findKeyByValue(map: Map<K, V>, value: V): K? {
        return map.entries.firstOrNull { it.value == value }?.key
    }

    /**
     * 将JSON字符串转换为Map（Java兼容版）
     * 注意：需要Gson依赖
     * @param jsonStr 要转换的JSON字符�?
     * @return 转换后的Map（可能为null或空Map�?
     *
     * Java调用示例�?
     * Map<String, Object> map = CollectionTools.jsonToMap(jsonStr);
     */
    private fun jsonStrToMap(jsonStr: String?): Map<String, Any> {
        if (jsonStr.isNullOrBlank()) return emptyMap()

        return try {
            jsonStr.jsonToMap<Map<String,Any>>()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}