@file:Suppress("UNCHECKED_CAST")

package io.core.common.util.tools

import io.core.common.util.extensions.cool.jsonToMap


/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/14 8:49
 * @description
 * @author Yuan
 */

// region Kotlin 扩展方法
/**
 * 在集合中查找第一个具有指定属性值的元素
 * 场景1：直接匹配Long值本身
 * val result1 = longList.findFirstByProperty(
 *     selector = { it }, // 直接返回元素本身
 *     value = 200L
 * )
 *
 * 场景2：匹配Long值的某个派生属性（示例：判断是否为偶数）
 * val result2 = longList.findFirstByProperty(
 *     selector = { num -> num % 2 == 0L }, // 返回Boolean类型
 *     value = true
 * ) 返回第一个偶数 100L
 */
inline fun <K, T> List<T>.findFirstByProperty(selector: (T) -> K, value: K): T? {
    return this.firstOrNull { selector(it) == value }
}

/**
 * 在集合中查找所有具有指定属性值的元素
 *
 * 通过给定的属性选择器函数，筛选出属性值与目标值相等的所有元素
 *
 * @param selector 用于从元素中提取目标属性的函数，接收类型为[T]的元素，返回类型为[K]的属性值
 * @param value 需要匹配的目标属性值，类型为[K]
 * @return 包含所有匹配元素的不可变列表，列表中元素的顺序与原始集合保持一致
 *
 * @param K 属性值的类型，要求实现相等性比较
 * @param T 集合元素的类型
 */
inline fun <K, T> List<T>.findAllByProperty(selector: (T) -> K, value: K): List<T> {
    // 使用过滤函数保留属性匹配的元素
    return this.filter { selector(it) == value }
}


object CollectionTools {
    @JvmStatic
    fun <K, V> mapValuesToList(map: Map<K, V>): List<V> {
        return map.values.toList()
    }

    @JvmStatic
    fun <K, V> mapKeysToList(map: Map<K, V>): List<K> {
        return map.keys.toList()
    }


    // 添加类型安全的重载版本
    @JvmStatic
    fun <V> jsonToMap(jsonStr: String?): Map<String, V> {
        return jsonStrToMap(jsonStr) as? Map<String, V> ?: emptyMap()
    }

    /**
     * 在给定的 Map 中查找第一个与指定值关联的键
     *
     * @param map 要搜索的键值对集合，类型为 [Map]<[K], [V]>
     * @param value 需要查找的目标值，类型为 [V]
     * @return 第一个匹配项的键，如果未找到匹配项则返回 `null`
     *
     * @param K Map 键的类型
     * @param V Map 值的类型，必须实现相等性判断
     */
    @JvmStatic
    fun <K, V> findKeyByValue(map: Map<K, V>, value: V): K? {
        return map.entries.firstOrNull { it.value == value }?.key
    }

    /**
     * 将JSON字符串转换为Map（Java兼容版）
     * 注意：需要Gson依赖
     * @param jsonStr 要转换的JSON字符串
     * @return 转换后的Map（可能为null或空Map）
     *
     * Java调用示例：
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