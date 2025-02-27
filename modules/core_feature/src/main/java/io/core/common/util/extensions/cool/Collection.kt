package io.core.common.util.extensions.cool

import com.google.gson.reflect.TypeToken

fun List<Float>.fastSum(): Float {
    var sum = 0f
    for (i in indices) {
        sum += this[i]
    }
    return sum
}

inline fun <T> List<T>.fastBinarySearch(
    fromIndex: Int = 0,
    toIndex: Int = size,
    comparison: (T) -> Int
): Int {
    when {
        fromIndex > toIndex ->
            throw IllegalArgumentException(
                "fromIndex ($fromIndex) is greater than toIndex ($toIndex)."
            )

        fromIndex < 0 ->
            throw IndexOutOfBoundsException("fromIndex ($fromIndex) is less than zero.")

        toIndex > size ->
            throw IndexOutOfBoundsException("toIndex ($toIndex) is greater than size ($size).")
    }

    var low = fromIndex
    var high = toIndex - 1

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = get(mid)
        val cmp = comparison(midVal)

        if (cmp < 0)
            low = mid + 1
        else if (cmp > 0)
            high = mid - 1
        else
            return mid // key found
    }
    return -(low + 1)  // key not found
}

inline fun <T, K : Comparable<K>> List<T>.fastBinarySearchBy(
    key: K?,
    fromIndex: Int = 0,
    toIndex: Int = size,
    crossinline selector: (T) -> K?
): Int = fastBinarySearch(fromIndex, toIndex) { compareValues(selector(it), key) }

fun <T> MutableList<T>.removeLastElement(): T {
    return if (isEmpty()) {
        throw NoSuchElementException("List is empty.")
    } else {
        removeAt(lastIndex)
    }
}

fun HashMap<String, *>.has(key: String, ignoreCase: Boolean = false): Boolean {
    for (item in this) {
        if (key.equals(item.key, ignoreCase)) {
            return true
        }
    }
    return false
}

fun <T> HashMap<String, T>.get(key: String, ignoreCase: Boolean = false): T? {
    for (item in this) {
        if (key.equals(item.key, ignoreCase)) {
            return item.value
        }
    }
    return null
}

inline fun <reified T> String.jsonToMap(): T {
    val type = object : TypeToken<T>() {}.type
    return GSON.fromJson(this, type)
}

/**
 * 为Kotlin [List]扩展排序函数（返回新列表）
 * @param selector 提供需要转换成整数的字段（如：Wallpaper::getTypeId）
 */
fun <T> List<T>.sortedByInt(selector: (T) -> String) =
    this.sortedBy { selector(it).toInt() }

/**
 * 为Kotlin [MutableList]扩展排序函数（原地排序）
 * @param selector 提供需要转换成整数的字段（如：Wallpaper::getTypeId）
 */
fun <T> MutableList<T>.sortByInt(selector: (T) -> String) =
    this.sortBy { selector(it).toInt() }

fun <T> createSet(vararg items: T): Set<T> {
    val set = HashSet<T>()
    for (item in items) {
        set.add(item)
    }
    return set
}


/**
 * Java专用重载方法（支持链式调用）
 * 示例：MapUtils.createMap("key1", "value1", "key2", 2)
 */

@JvmName("create")
fun <K, V> createMap(vararg entries: Any): HashMap<K, V> {
    require(entries.size % 2 == 0) { "参数数量必须为偶数" }

    val map = hashMapOf<K, V>()
    for (i in entries.indices step 2) {
        @Suppress("UNCHECKED_CAST")
        map[entries[i] as K] = entries[i + 1] as V
    }
    return map
}


/**
 * 键值对构建器（Java链式调用专用）
 * 示例：
 * MapUtils.builder()
 *     .put("name", "John")
 *     .put("age", 25)
 *     .build();
 */
class MapBuilder<K, V> {
    private val map = hashMapOf<K, V>()

    fun put(key: K, value: V): MapBuilder<K, V> {
        map[key] = value
        return this
    }

    fun build(): HashMap<K, V> = HashMap(map)
}

fun <K, V> mapBuilder(): MapBuilder<K, V> = MapBuilder()
