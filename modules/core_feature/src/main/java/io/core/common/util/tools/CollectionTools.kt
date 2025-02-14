package io.core.common.util.tools

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


object CollectionTools {
    @JvmStatic
    fun <K, V> mapValuesToList(map: Map<K, V>): List<V> {
        return map.values.toList()
    }

    @JvmStatic
    fun <K, V> mapKeysToList(map: Map<K, V>): List<K> {
        return map.keys.toList()
    }


}