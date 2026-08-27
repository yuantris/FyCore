package io.core.common.helper.valid

import java.util.function.Consumer
import java.util.function.Supplier

/**
 * 针对Java环境优化的空值检查工具
 * 提供全面的空值、空集合、空数组、空字符串等检查功能
 */
object NullCheck {

    // region 基础判空

    /**
     * 检查对象是否为null
     * Java示例：NullCheck.isNull(obj)
     */
    @JvmStatic
    fun isNull(obj: Any?) = obj == null

    /**
     * 检查多个对象中是否存在null
     * Java示例：NullCheck.hasAnyNull(obj1, obj2)
     */
    @JvmStatic
    fun hasAnyNull(vararg objects: Any?) = objects.any { it == null }

    /**
     * 检查所有对象是否都不为null
     * Java示例：NullCheck.allNotNull(obj1, obj2)
     */
    @JvmStatic
    fun allNotNull(vararg objects: Any?) = objects.all { it != null }

    // endregion

    // region 字符串检查

    /**
     * 检查字符串是否为null或空字符串
     * Java示例：NullCheck.isEmpty(string)
     */
    @JvmStatic
    fun isEmpty(str: CharSequence?) = str.isNullOrEmpty()

    /**
     * 检查字符串是否为非null且非空
     * Java示例：NullCheck.isNotEmpty(string)
     */
    @JvmStatic
    fun isNotEmpty(str: CharSequence?) = !isEmpty(str)

    /**
     * 检查字符串是否为null或空白字符串
     * Java示例：NullCheck.isBlank(string)
     */
    @JvmStatic
    fun isBlank(str: CharSequence?) = str.isNullOrBlank()

    /**
     * 检查字符串是否为非null且非空白
     * Java示例：NullCheck.isNotBlank(string)
     */
    @JvmStatic
    fun isNotBlank(str: CharSequence?) = !isBlank(str)

    // endregion

    // region 集合检查

    /**
     * 检查集合是否为null或空集合
     * 支持所有Collection类型（List/Set等）
     * Java示例：NullCheck.isEmpty(collection)
     */
    @JvmStatic
    fun isEmpty(collection: Collection<*>?) = collection == null || collection.isEmpty()

    /**
     * 检查Map是否为null或空Map
     * Java示例：NullCheck.isEmpty(map)
     */
    @JvmStatic
    fun isEmpty(map: Map<*, *>?) = map == null || map.isEmpty()

    // endregion

    // region 数组检查

    /**
     * 检查对象数组是否为空
     * Java示例：NullCheck.isEmpty(array)
     */
    @JvmStatic
    fun <T> isEmpty(array: Array<T>?) = array.isNullOrEmpty()

    /**
     * 检查基本类型数组是否为空
     */
    @JvmStatic fun isEmpty(array: IntArray?) = array == null || array.isEmpty()
    @JvmStatic fun isEmpty(array: LongArray?) = array == null || array.isEmpty()
    @JvmStatic fun isEmpty(array: BooleanArray?) = array == null || array.isEmpty()
    @JvmStatic fun isEmpty(array: ByteArray?) = array == null || array.isEmpty()
    @JvmStatic fun isEmpty(array: ShortArray?) = array == null || array.isEmpty()
    @JvmStatic fun isEmpty(array: CharArray?) = array == null || array.isEmpty()
    @JvmStatic fun isEmpty(array: FloatArray?) = array == null || array.isEmpty()
    @JvmStatic fun isEmpty(array: DoubleArray?) = array == null || array.isEmpty()

    // endregion

    // region 安全操作

    /**
     * 安全获取值（带默认值）
     * Java示例：NullCheck.getOrDefault(obj, defaultValue)
     */
    @JvmStatic
    fun <T> getOrDefault(value: T?, defaultValue: T): T = value ?: defaultValue

    /**
     * 安全获取值（带Supplier）
     * Java示例：NullCheck.getOrSupply(obj, () -> "default")
     */
    @JvmStatic
    fun <T> getOrSupply(value: T?, supplier: Supplier<T>): T = value ?: supplier.get()

    /**
     * 非空校验并抛出指定异常
     * Java示例：NullCheck.requireNonNull(obj, "错误信息")
     */
    @JvmStatic
    fun <T> requireNonNull(obj: T?, message: String): T {
        if (obj == null) throw IllegalArgumentException(message)
        return obj
    }

    // endregion

    // region 高阶函数（Java 8+）

    /**
     * 非空时执行操作
     * Java示例：NullCheck.ifNotNull(obj, v -> System.out.println(v))
     */
    @JvmStatic
    fun <T> ifNotNull(obj: T?, consumer: Consumer<T>) {
        obj?.let { consumer.accept(it) }
    }

    /**
     * 为空时执行操作
     * Java示例：NullCheck.ifNull(obj, () -> System.out.println("null"))
     */
    @JvmStatic
    fun ifNull(obj: Any?, runnable: Runnable) {
        if (obj == null) runnable.run()
    }

    // endregion
}