package io.core.common.helper.pool

import androidx.annotation.CallSuper
import androidx.core.util.Pools

interface ObjectPool<T> {
    fun obtain(): T
    fun recycle(target: T)
    fun create(): T
}

/**
 * 同步 线程不安全
 */
abstract class BaseObjectPool<T : Any>(size: Int) : ObjectPool<T> {

    open val pool = Pools.SimplePool<T>(size)

    override fun obtain(): T {
        return pool.acquire() ?: create()
    }

    @CallSuper
    override fun recycle(target: T) {
        pool.release(target)
    }

}

/**
 * 线程安全
 */
abstract class BaseSafeObjectPool<T : Any>(size: Int) : BaseObjectPool<T>(size) {

    override val pool = Pools.SynchronizedPool<T>(size)

}

/**
 * 委托实现
 */
class ObjectPoolLocked<T>(private val delegate: ObjectPool<T>) : ObjectPool<T> by delegate {

    @Synchronized
    override fun obtain(): T {
        return delegate.obtain()
    }

    @Synchronized
    override fun recycle(target: T) {
        return delegate.recycle(target)
    }

}

fun <T> ObjectPool<T>.synchronized(): ObjectPool<T> = ObjectPoolLocked(this)
