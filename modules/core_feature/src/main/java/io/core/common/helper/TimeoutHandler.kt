package io.core.common.helper

import io.core.common.util.extensions.cool.MainThreadHandler
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * 超时处理帮助类，用于处理超时操作。
 * @param timeoutMillis 超时时间
 * @param timeoutCallback 超时回调
 */
class TimeoutHandler(
    private val timeoutMillis: Long,
    private val timeoutCallback: TimeoutCallback
) {
    private val lock = Any()
    private val handler = MainThreadHandler.handler
    private val timeoutRunnable = RunnablePool.obtain {
        synchronized(lock) {
            if (state.getAndSet(State.TRIGGERED) == State.ACTIVE) {
                timeoutCallback.onTimeout()
            }
        }
    }

    private enum class State { ACTIVE, CANCELED, TRIGGERED }
    private val state = AtomicReference(State.ACTIVE)

    fun onProgress() {
        synchronized(lock) {
            if (state.get() != State.ACTIVE) return
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed(timeoutRunnable, timeoutMillis)
        }
    }

    fun cancel() {
        synchronized(lock) {
            if (state.getAndSet(State.CANCELED) == State.ACTIVE) {
                handler.removeCallbacks(timeoutRunnable)
                RunnablePool.recycle(timeoutRunnable)
            }
        }
    }

    fun reset() {
        synchronized(lock) {
            if (state.get() != State.TRIGGERED) {
                handler.removeCallbacks(timeoutRunnable)
            }
            state.set(State.ACTIVE)
        }
    }
}

interface TimeoutCallback {
    fun onTimeout()
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
        override fun run() { block() }
    }
}


