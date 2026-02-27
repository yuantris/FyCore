package io.core.nav.other.single

/**
 * 单例模板（无参初始化，线程安全）
 * @param T 单例类型
 * @param creator 构造器函数
 */
open class SingletonHolder<out T : Any>(private val creator: () -> T) {
    @Volatile
    private var instance: T? = null

    /**
     * 获取单例实例
     */
    fun getInstance(): T = instance ?: synchronized(this) {
        instance ?: creator().also { instance = it }
    }

    /**
     * 清除单例实例（用于测试环境或特殊情况�?
     */
    fun clearInstance() {
        instance = null
    }
}