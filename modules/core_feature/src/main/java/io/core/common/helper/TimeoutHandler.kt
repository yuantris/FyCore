package io.core.common.helper

import io.core.common.util.extensions.cool.HandlerGT
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 超时处理帮助类，用于处理超时操作。
 * @param timeoutMillis 超时时间(毫秒)
 * @param timeoutCallback 超时回调
 * @param logger 日志记录器(可选)
 * @param maxRetries 最大重试次数(可选，默认无限制)
 */
class TimeoutHandler @JvmOverloads constructor(
    private val timeoutMillis: Long,
    private val timeoutCallback: TimeoutCallback,
    private val logger: ((String) -> Unit)? = null,
    private val maxRetries: Int = -1, // -1 表示无限制
) {
    private val lock = Any()
    private val handler = HandlerGT.main
    private val retryCount = AtomicInteger(0)

    private val timeoutRunnable = RunnablePool.obtain {
        synchronized(lock) {
            if (state.getAndSet(State.TRIGGERED) == State.ACTIVE) {
                logger?.invoke("Timeout triggered (reason: ${reason.get()})")
                timeoutCallback.onTimeout(reason.get())
            }
        }
    }

    // 超时原因枚举
    enum class TimeoutReason {
        NORMAL,         // 正常超时
        RETRY_LIMIT,    // 达到重试限制
        MANUAL_TRIGGER  // 手动触发
    }

    private enum class State { ACTIVE, CANCELED, TRIGGERED }

    private val state = AtomicReference(State.ACTIVE)
    private val reason = AtomicReference(TimeoutReason.NORMAL)

    fun resetTimeout() {
        synchronized(lock) {
            if (state.get() != State.ACTIVE) return

            // 检查重试次数限制
            if (maxRetries > 0 && retryCount.incrementAndGet() > maxRetries) {
                reason.set(TimeoutReason.RETRY_LIMIT)
                logger?.invoke("Max retries reached ($maxRetries), triggering timeout")
                handler.post(timeoutRunnable)
                return
            }

            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed(timeoutRunnable, timeoutMillis)
            logger?.invoke("Timeout reset (retry ${retryCount.get()}/$maxRetries)")
        }
    }

    fun cancel() {
        synchronized(lock) {
            if (state.getAndSet(State.CANCELED) == State.ACTIVE) {
                handler.removeCallbacks(timeoutRunnable)
                RunnablePool.recycle(timeoutRunnable)
                logger?.invoke("Timeout canceled")
            }
        }
    }

    @JvmOverloads
    fun triggerNow(reason: TimeoutReason = TimeoutReason.MANUAL_TRIGGER) {
        synchronized(lock) {
            this.reason.set(reason)
            if (state.getAndSet(State.TRIGGERED) == State.ACTIVE) {
                handler.removeCallbacks(timeoutRunnable)
                logger?.invoke("Timeout manually triggered (reason: $reason)")
                timeoutCallback.onTimeout(reason)
            }
        }
    }

    fun restart() {
        synchronized(lock) {
            if (state.get() != State.TRIGGERED) {
                handler.removeCallbacks(timeoutRunnable)
            }
            state.set(State.ACTIVE)
            reason.set(TimeoutReason.NORMAL)
            retryCount.set(0)
            logger?.invoke("Timeout handler restarted")
        }
    }
}

interface TimeoutCallback {
    fun onTimeout(reason: TimeoutHandler.TimeoutReason)
}

private object RunnablePool {
    private val pool = ConcurrentLinkedQueue<Runnable>()

    fun obtain(block: () -> Unit): Runnable {
        return pool.poll()?.also { (it as? WrappedRunnable)?.block = block }
            ?: WrappedRunnable(block)
    }

    fun recycle(runnable: Runnable) {
        if (runnable is WrappedRunnable) {
            pool.offer(runnable)
        }
    }

    private class WrappedRunnable(var block: () -> Unit) : Runnable {
        override fun run() {
            block()
        }
    }
}


