package io.core.utils

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 *
 * 只执行一次的委托，解�?
 * private var isInit = false
 * if (!isInit) {
 *     isInit = true
 *     // do something
 * }
 * 优化�?
 * val value by Once {
 *     // do something
 * }
 * @author Yuan
 * 2025/3/12 17:05
 */
class Once<T>(private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
    private val initialized = AtomicBoolean(false)
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return if (initialized.compareAndSet(false, true)) {
            value = initializer().also { value = it }
            value!!
        } else {
            value!!
        }
    }
}

class ResettableOnce<T>(private val initializer: () -> T) {
    private val initialized = AtomicBoolean(false)
    private var value: T? = null

    fun get(): T {
        return if (initialized.compareAndSet(false, true)) {
            value = initializer().also { value = it }
            value!!
        } else {
            value!!
        }
    }

    fun reset() {
        initialized.set(false)
        value = null
    }
}
