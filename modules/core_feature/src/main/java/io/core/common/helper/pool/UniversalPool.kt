package io.core.common.helper.pool

import androidx.core.util.Pools
import java.util.concurrent.atomic.AtomicInteger

/**
 * 基于AndroidX Pools的通用对象池
 * @param T 对象类型
 */
class UniversalPool<T : Any> private constructor(
    private val config: Config<T>
) {
    // 内部使用AndroidX的SynchronizedPool
    private val innerPool: Pools.Pool<T> = Pools.SynchronizedPool(config.maxSize)

    // 统计计数器
    private val createdCount = AtomicInteger(0)
    private val destroyedCount = AtomicInteger(0)
    private val activeCount = AtomicInteger(0)

    /**
     * 总创建对象数
     */
    val totalCreated: Int get() = createdCount.get()

    /**
     * 总销毁对象数
     */
    val totalDestroyed: Int get() = destroyedCount.get()

    /**
     * 当前活跃对象数
     */
    val currentActive: Int get() = activeCount.get()

    // 统计信息
    val availableCount: Int get() = config.maxSize - activeCount.get()

    /**
     * 从池中获取对象
     */
    fun borrow(): T {
        val obj = innerPool.acquire()?.takeIf { config.validator(it) } ?: createNewObject()
        activeCount.incrementAndGet()
        return obj
    }

    /**
     * 归还对象到池中
     * @return 是否成功回收
     */
    fun release(obj: T): Boolean {
        return if (config.validator(obj)) {
            config.resetter(obj)
            if (innerPool.release(obj)) {
                activeCount.decrementAndGet()
                true
            } else {
                destroyObject(obj)
                false
            }
        } else {
            destroyObject(obj)
            false
        }
    }

    /**
     * 强制销毁对象
     */
    fun destroy(obj: T) {
        destroyObject(obj)
    }

    /**
     * 清空池内所有对象
     */
    fun clear() {
        var obj: T?
        while (innerPool.acquire().also { obj = it } != null) {
            destroyObject(obj as T)
        }
    }

    private fun createNewObject(): T {
        if (activeCount.get() >= config.maxSize) {
            throw PoolExhaustedException("Pool exhausted (max=${config.maxSize})")
        }
        return config.creator().also {
            createdCount.incrementAndGet()
        }
    }

    private fun destroyObject(obj: T) {
        config.destroyer(obj)
        destroyedCount.incrementAndGet()
        activeCount.decrementAndGet()
    }

    /**
     * 配置构建器
     */
    class Builder<T : Any> {
        var maxSize = 10
        lateinit var creator: () -> T
        var resetter: (T) -> Unit = { _ -> }
        var validator: (T) -> Boolean = { true }
        var destroyer: (T) -> Unit = { _ -> }

        fun build(): UniversalPool<T> {
            require(::creator.isInitialized) { "Creator must be initialized" }
            return UniversalPool(Config(
                maxSize = maxSize,
                creator = creator,
                resetter = resetter,
                validator = validator,
                destroyer = destroyer
            ))
        }
    }

    private data class Config<T>(
        val maxSize: Int,
        val creator: () -> T,
        val resetter: (T) -> Unit,
        val validator: (T) -> Boolean,
        val destroyer: (T) -> Unit
    )
}

class PoolExhaustedException(message: String) : RuntimeException(message)