package io.core.common.helper.pool

import androidx.core.util.Pools
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * 通用对象池 (基于AndroidX实现)
 * @param T 对象类型
 */
class UniversalPool<T : Any> private constructor(
    private val config: Config<T>
) {
    // 内部使用AndroidX的SynchronizedPool
    private val innerPool: Pools.Pool<WeakReference<T>> = Pools.SynchronizedPool(config.maxSize)

    // 统计计数器
    private val createdCount = AtomicInteger(0)
    private val destroyedCount = AtomicInteger(0)
    private val activeCount = AtomicInteger(0)
    private val lastAccessTime = AtomicLong(System.currentTimeMillis())

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

    /**
     * 池中可用对象数
     */
    val availableCount: Int get() = config.maxSize - activeCount.get()

    /**
     * 从池中获取对象
     */
    fun borrow(): T {
        lastAccessTime.set(System.currentTimeMillis())

        // 清理过期对象
        if (config.expirationTime > 0) {
            cleanExpiredObjects()
        }

        val obj = findValidObject() ?: createNewObject()
        activeCount.incrementAndGet()
        return obj
    }

    /**
     * 归还对象到池中
     * @return 是否成功回收
     */
    fun release(obj: T): Boolean {
        lastAccessTime.set(System.currentTimeMillis())

        return if (config.validator(obj)) {
            config.resetter(obj)
            if (innerPool.release(WeakReference(obj))) {
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
     * 安全使用对象(自动获取和释放)
     */
    inline fun <R> use(block: (T) -> R): R {
        val obj = borrow()
        try {
            return block(obj)
        } finally {
            release(obj)
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
        var ref: WeakReference<T>?
        while (innerPool.acquire().also { ref = it } != null) {
            ref?.get()?.let { destroyObject(it) }
        }
    }

    /**
     * 获取最后访问时间(ms)
     */
    fun getLastAccessTime(): Long = lastAccessTime.get()

    /**
     * 自动释放资源扩展函数(类似try-with-resources)
     */
    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    inline fun <T : Any, R> UniversalPool<T>.use(block: (T) -> R): R {
        val obj = this.borrow()
        try {
            return block(obj)
        } finally {
            this.release(obj)
        }
    }

    private fun findValidObject(): T? {
        var ref: WeakReference<T>?
        while (innerPool.acquire().also { ref = it } != null) {
            val obj = ref?.get()
            if (obj != null && config.validator(obj)) {
                return obj
            } else {
                obj?.let { destroyObject(it) }
            }
        }
        return null
    }

    private fun createNewObject(): T {
        if (activeCount.get() >= config.maxSize) {
            throw RuntimeException("Pool exhausted (max=${config.maxSize})")
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

    private fun cleanExpiredObjects() {
        val now = System.currentTimeMillis()
        var ref: WeakReference<T>?
        while (innerPool.acquire().also { ref = it } != null) {
            val obj = ref?.get()
            if (obj == null || (config.expirationTime > 0 && now - lastAccessTime.get() > config.expirationTime)) {
                obj?.let { destroyObject(it) }
            } else {
                ref?.let { innerPool.release(it) }
                break
            }
        }
    }

    /**
     * 配置构建器
     */
    class Builder<T : Any> {
        private var maxSize = 10
        private var expirationTime: Long = 0 // 默认永不过期
        private lateinit var creator: () -> T
        private var resetter: (T) -> Unit = { _ -> }
        private var validator: (T) -> Boolean = { true }
        private var destroyer: (T) -> Unit = { _ -> }

        fun maxSize(size: Int): Builder<T> {
            this.maxSize = max(size, 1)
            return this
        }

        fun expirationTime(timeMs: Long): Builder<T> {
            this.expirationTime = max(timeMs, 0)
            return this
        }

        fun create(block: () -> T): Builder<T> {
            this.creator = block
            return this
        }

        fun reset(block: (T) -> Unit): Builder<T> {
            this.resetter = block
            return this
        }

        fun validator(block: (T) -> Boolean): Builder<T> {
            this.validator = block
            return this
        }

        fun destroy(block: (T) -> Unit): Builder<T> {
            this.destroyer = block
            return this
        }

        fun build(): UniversalPool<T> {
            require(::creator.isInitialized) { "Creator must be initialized" }
            return UniversalPool(Config(
                maxSize = maxSize,
                expirationTime = expirationTime,
                creator = creator,
                resetter = resetter,
                validator = validator,
                destroyer = destroyer
            ))
        }
    }

    private data class Config<T>(
        val maxSize: Int,
        val expirationTime: Long,
        val creator: () -> T,
        val resetter: (T) -> Unit,
        val validator: (T) -> Boolean,
        val destroyer: (T) -> Unit
    )
}

