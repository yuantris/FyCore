package io.core.other

import io.core.common.util.tools.buildMainHandler

class DoubleClickProcessor(
    private val doubleClickAction: () -> Unit,
    private val singleClickHint: () -> Unit = {},
    private val delayMillis: Long = 300
) {
    // 线程安全的时间记录
    @Volatile
    private var lastClickTime = 0L

    // 使用主线程Handler确保UI操作安全
    private val handler = buildMainHandler()
    private val pendingSingleClick = mutableListOf<Runnable>()

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

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        pendingSingleClick.clear()
    }
}