package io.core.common.helper.pool

import androidx.annotation.CallSuper
import androidx.core.util.Pools
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

// region 核心接口定义

/**
 * 增强型对象池接口
 * @param T 对象类型
 */
interface EnhancedObjectPool<T> {
    /**
     * 获取对象（协程挂起安全）
     */
    suspend fun acquire(): T

    /**
     * 释放对象（协程挂起安全）
     * @return 是否成功回收
     */
    suspend fun release(obj: T): Boolean

    /**
     * 立即销毁对象（线程安全）
     */
    fun destroy(obj: T)
}

// endregion

// region Android官方池适配实现

/**
 * 兼容Android Pools的基类
 */
abstract class AndroidCompatPool<T : Any>(
    protected val maxSize: Int,
    private val reset: (T) -> Unit,
    private val validate: (T) -> Boolean
) : EnhancedObjectPool<T> {

    protected abstract val internalPool: Pools.Pool<T>

    @CallSuper
    override suspend fun release(obj: T): Boolean {
        return if (validate(obj)) {
            reset(obj)
            synchronized(internalPool) {
                internalPool.release(obj)
            }
            true
        } else {
            destroy(obj)
            false
        }
    }

    override fun destroy(obj: T) {
        // Android默认池不提供销毁逻辑，需子类实现
    }
}

/**
 * 同步安全对象池（基于AndroidX实现）
 */
class AndroidObjectPool<T : Any>(
    maxSize: Int,
    private val create: () -> T,
    reset: (T) -> Unit,
    validate: (T) -> Boolean,
    private val destroy: (T) -> Unit
) : AndroidCompatPool<T>(maxSize, reset, validate) {

    override val internalPool = Pools.SynchronizedPool<T>(maxSize)

    override suspend fun acquire(): T {
        return synchronized(internalPool) {
            internalPool.acquire() ?: create()
        }
    }

    fun preallocate(count: Int) {
        synchronized(internalPool) {
            repeat(count) {
                val obj = create()
                internalPool.release(obj)
            }
        }
    }

    override fun destroy(obj: T) {
        destroy.invoke(obj)
    }
}

// endregion

// region 协程优化实现

/**
 * 协程优化对象池
 */
class CoroutineObjectPool<T : Any>(
    private val maxSize: Int,
    private val create: () -> T,
    private val reset: (T) -> Unit = {},
    private val validate: (T) -> Boolean = { true },
    private val destroy: (T) -> Unit = {}
) : EnhancedObjectPool<T> {

    private val mutex = Mutex()
    private val pool = LinkedList<T>()

    override suspend fun acquire(): T = mutex.withLock {
        findValidObject() ?: createNewObject()
    }

    override suspend fun release(obj: T): Boolean = mutex.withLock {
        when {
            pool.size >= maxSize -> {
                destroy(obj)
                false
            }

            validate(obj) -> {
                reset(obj)
                pool.add(obj)
                true
            }

            else -> {
                destroy(obj)
                false
            }
        }
    }

    override fun destroy(obj: T) {
        destroy.invoke(obj)
    }

    private fun findValidObject(): T? {
        val iterator = pool.iterator()
        while (iterator.hasNext()) {
            val obj = iterator.next()
            if (validate(obj)) {
                iterator.remove()
                return obj
            } else {
                iterator.remove()
                destroy(obj)
            }
        }
        return null
    }

    private fun createNewObject(): T {
        check(pool.size < maxSize) { "Pool exhausted" }
        return create()
    }
}

// endregion

// region 统一构建工具

object ObjectPoolBuilder {

    /**
     * 构建Android风格对象池
     */
    fun <T : Any> android(
        maxSize: Int,
        preallocate: Int = 0, // 新增预分配数量
        create: () -> T,
        config: AndroidConfig<T>.() -> Unit = {}
    ): EnhancedObjectPool<T> {
        val configObj = AndroidConfig<T>().apply(config)
        val pool = AndroidObjectPool(
            maxSize = maxSize,
            create = create,
            reset = configObj.reset,
            validate = configObj.validate,
            destroy = configObj.destroy
                ?: throw IllegalArgumentException("Destroy callback required")
        )
        if (preallocate > 0) {
            require(preallocate <= maxSize) { "预分配数量不能超过最大容量" }
            pool.preallocate(preallocate)
        }
        return pool
    }

    /**
     * 构建协程优化对象池
     */
    fun <T : Any> coroutine(
        maxSize: Int,
        create: () -> T,
        config: CoroutineConfig<T>.() -> Unit = {}
    ): EnhancedObjectPool<T> {
        val configObj = CoroutineConfig<T>().apply(config)
        return CoroutineObjectPool(
            maxSize = maxSize,
            create = create,
            reset = configObj.reset,
            validate = configObj.validate,
            destroy = configObj.destroy
        )
    }

    class AndroidConfig<T> {
        var reset: (T) -> Unit = {}
        var validate: (T) -> Boolean = { true }
        var destroy: ((T) -> Unit)? = null
    }

    class CoroutineConfig<T> {
        var reset: (T) -> Unit = {}
        var validate: (T) -> Boolean = { true }
        var destroy: (T) -> Unit = {}
    }
}

// endregion

// region 扩展工具

/**
 * 自动释放资源（类似try-with-resources）
 */
suspend inline fun <T : Any, R> EnhancedObjectPool<T>.use(block: (T) -> R): R {
    val obj = acquire()
    try {
        return block(obj)
    } finally {
        release(obj)
    }
}

// endregion