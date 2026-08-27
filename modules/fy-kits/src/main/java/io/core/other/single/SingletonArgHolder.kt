package io.core.other.single

/**
 * 单例模板（支持带参数初始化，线程安全）
 * @param T 单例类型
 * @param A 构造参数类型
 * @param creator 构造器函数
 */
open class SingletonArgHolder<out T : Any, in A>(private val creator: (A) -> T) {
    @Volatile
    private var instance: T? = null

    /**
     * 获取单例实例
     * @param arg 构造参数
     */
    fun getInstance(arg: A): T = instance ?: synchronized(this) {
        instance ?: creator(arg).also { instance = it }
    }

    /**
     * 清除单例实例（用于测试环境或特殊情况）
     */
    fun clearInstance() {
        instance = null
    }
}