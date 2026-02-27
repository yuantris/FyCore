package io.core.nav.other

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.core.utils.tools.buildMainHandler

class DoubleClickProcessor(
    private val doubleClickAction: () -> Unit,
    private val singleClickHint: () -> Unit = {},
    lifecycle: Lifecycle? = null,
    private val delayMillis: Long = 300
) : DefaultLifecycleObserver {
    // 线程安全的时间记�?
    @Volatile
    private var lastClickTime = 0L

    // 使用主线程Handler确保UI操作安全
    private val handler = buildMainHandler()
    private val pendingSingleClick = mutableListOf<Runnable>()

    init {
        lifecycle?.addObserver(this)
    }

    fun handleClick() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastClickTime < delayMillis) {
            // 符合双击条件
            handler.removeCallbacksAndMessages(null)
            pendingSingleClick.clear()
            doubleClickAction()
        } else {
            // 首次单击或超时后单击
            val runnable = Runnable {
                singleClickHint()
                pendingSingleClick.clear()
            }
            pendingSingleClick.add(runnable)
            handler.postDelayed(runnable, delayMillis)
        }

        lastClickTime = currentTime
    }

    override fun onDestroy(owner: LifecycleOwner) {
        handler.removeCallbacksAndMessages(null)
        pendingSingleClick.clear()
    }
}