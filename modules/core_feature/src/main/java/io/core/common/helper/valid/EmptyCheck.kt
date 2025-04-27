package io.core.common.helper.valid

/**
 * 空检查工具类
 * 1. 支持基础类型、数组、集合、Map的 null/empty 检查
 * 2. 完全兼容 Java 调用
 * 3. 符合高内聚低耦合设计原则
 */
object EmptyCheck {

    // region 基础类型检查
    
    @JvmStatic
    fun isNullOrEmpty(value: CharSequence?): Boolean {
        return value.isNullOrEmpty()
    }

    @JvmStatic
    fun isNullOrBlank(value: CharSequence?): Boolean {
        return value.isNullOrBlank()
    }

    // endregion

    // region 数组检查
    
    @JvmStatic
    fun <T> isNullOrEmpty(array: Array<T>?): Boolean {
        return array == null || array.isEmpty()
    }

    @JvmStatic
    fun isNullOrEmpty(array: IntArray?): Boolean {
        return array == null || array.isEmpty()
    }

    @JvmStatic
    fun isNullOrEmpty(array: LongArray?): Boolean {
        return array == null || array.isEmpty()
    }

    // 其他基本类型数组检查（ByteArray, ShortArray...）
    
    // endregion

    // region 集合检查
    
    @JvmStatic
    fun <T> isNullOrEmpty(collection: Collection<T>?): Boolean {
        return collection == null || collection.isEmpty()
    }

    @JvmStatic
    fun <K, V> isNullOrEmpty(map: Map<K, V>?): Boolean {
        return map == null || map.isEmpty()
    }

    @JvmStatic
    fun <T> isNullOrEmpty(iterable: Iterable<T>?): Boolean {
        return when {
            iterable == null -> true
            iterable is Collection<*> -> (iterable as Collection<*>).isEmpty()
            else -> !iterable.iterator().hasNext()
        }
    }

    // endregion

    // region 对象扩展检查
    
    @JvmStatic
    fun <T> isNullOrEmpty(obj: T?, checker: (T) -> Boolean = { false }): Boolean {
        return obj == null || checker(obj)
    }

    // endregion

    // region 链式检查（可选）
    
    class CheckChain<T>(private val target: T?) {
        fun addCheck(check: (T) -> Boolean): CheckChain<T> {
            return if (target == null || !check(target)) {
                CheckChain(null)
            } else {
                this
            }
        }

        fun getResult(): Boolean {
            return target != null
        }
    }

    @JvmStatic
    fun <T> beginCheck(target: T?): CheckChain<T> {
        return CheckChain(target)
    }
    
    // endregion
}