package io.core.common.helper.valid

/**
 * 高内聚判空工具类
 * 1. 支持链式调用
 * 2. 支持多参数判空
 * 3. 提供空安全操作符替代方案
 * 4. 支持自定义空回调
 * 5. 完全兼容Kotlin空安全特性
 */
object NullChecker {

    // region 核心判空方法
    
    /**
     * 判断对象是否为空
     * @param obj 待检测对象
     * @return Boolean 是否为空
     */
    fun isNull(obj: Any?) = obj == null

    /**
     * 多参数非空检查（全非空返回true）
     * @param objects 可变参数列表
     * @return Boolean 是否全部非空
     */
    fun checkNotNull(vararg objects: Any?) = objects.all { it != null }

    /**
     * 多参数空值检查（存在空返回true）
     * @param objects 可变参数列表
     * @return Boolean 是否存在空值
     */
    fun hasAnyNull(vararg objects: Any?) = objects.any { it == null }

    // endregion

    // region 空值处理扩展
    
    /**
     * 安全获取对象（支持默认值）
     * @param obj 目标对象
     * @param default 默认值/生成函数
     * @return 非空结果
     */
    @JvmOverloads
    fun <T> safeEmpty(obj: T?, default: () -> T = { throw NullPointerException("Object is null") }) = obj ?: default()

    /**
     * 非空校验（支持异常抛出）
     * @param obj 目标对象
     * @param message 异常信息
     * @return 非空对象
     * @throws NullPointerException 对象为空时抛出
     */
    fun <T : Any> requireNotNull(obj: T?, message: String = "") = obj ?: throw NullPointerException(message)

    // endregion

    // region 高阶函数操作
    
    /**
     * 非空执行代码块
     * @param obj 目标对象
     * @param block 执行代码块
     * @return 原始对象（保持链式调用）
     */
    fun <T> ifNotNull(obj: T?, block: (T) -> Unit): T? {
        obj?.let(block)
        return obj
    }

    /**
     * 空值执行代码块
     * @param obj 目标对象
     * @param block 执行代码块
     * @return 原始对象（保持链式调用）
     */
    fun <T> ifNull(obj: T?, block: () -> Unit): T? {
        if (obj == null) block()
        return obj
    }

    // endregion
}
