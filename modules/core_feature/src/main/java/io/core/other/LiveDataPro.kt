package io.core.other

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import io.core.engine.livebus.LiveEventBus

object LiveDataPro {

    // ==================================================
    // 发送事件优化 (Java 中可省略 Class 参数)
    // ==================================================
    @JvmStatic
    fun <T : Any> postEvent(tag: String, event: T) {
        LiveEventBus.get(tag, event.javaClass).post(event)
    }

    @JvmStatic
    fun <T : Any> postEventDelay(tag: String, event: T, delay: Long) {
        LiveEventBus.get(tag, event.javaClass).postDelay(event, delay)
    }

    @JvmStatic
    fun <T : Any> postEventOrderly(tag: String, event: T) {
        LiveEventBus.get(tag, event.javaClass).postOrderly(event)
    }

    // ==================================================
    // 原始带 Class 参数的方法（处理 null 或特殊场景）
    // ==================================================
    @JvmStatic
    fun <T> postEvent(tag: String, event: T?, clazz: Class<T>) {
        LiveEventBus.get(tag, clazz).post(event)
    }

    @JvmStatic
    fun <T> postEventDelay(tag: String, event: T?, delay: Long, clazz: Class<T>) {
        LiveEventBus.get(tag, clazz).postDelay(event, delay)
    }

    @JvmStatic
    fun <T> postEventOrderly(tag: String, event: T?, clazz: Class<T>) {
        LiveEventBus.get(tag, clazz).postOrderly(event)
    }

    // ==================================================
    // 观察事件优化（链式 API）
    // ==================================================
    class ObserverBuilder<T> internal constructor(
        private val tag: String,
        private val clazz: Class<T>
    ) {
        fun with(owner: AppCompatActivity, observer: Observer<T>) {
            LiveEventBus.get(tag, clazz).observe(owner, observer)
        }

        fun with(owner: Fragment, observer: Observer<T>) {
            LiveEventBus.get(tag, clazz).observe(owner, observer)
        }

        fun with(owner: LifecycleService, observer: Observer<T>) {
            LiveEventBus.get(tag, clazz).observe(owner, observer)
        }

        fun withSticky(owner: AppCompatActivity, observer: Observer<T>) {
            LiveEventBus.get(tag, clazz).observeSticky(owner, observer)
        }

        fun withSticky(owner: Fragment, observer: Observer<T>) {
            LiveEventBus.get(tag, clazz).observeSticky(owner, observer)
        }

        fun withSticky(owner: LifecycleService, observer: Observer<T>) {
            LiveEventBus.get(tag, clazz).observeSticky(owner, observer)
        }
    }

    @JvmStatic
    fun <T> on(tag: String, clazz: Class<T>) = ObserverBuilder(tag, clazz)
}