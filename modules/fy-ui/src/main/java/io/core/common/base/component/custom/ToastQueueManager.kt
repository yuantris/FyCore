package io.core.common.base.component.custom

import android.content.Context
import io.core.common.base.component.custom.strategy.ToastAnimationStrategy
import io.core.common.base.component.custom.strategy.ToastAppearanceStrategy
import io.core.common.base.component.custom.strategy.ToastPositionStrategy
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Toast队列管理器
 * 支持按顺序队列展示多个Toast消息
 */
object ToastQueueManager {
    private val toastQueue = ConcurrentLinkedQueue<ToastTask>()
    private val mutex = Mutex()
    private var isProcessing = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    data class ToastTask(
        val context: Context,
        val message: String,
        val duration: Int = 1800,
        val appearance: ToastAppearanceStrategy? = null,
        val animation: ToastAnimationStrategy? = null,
        val position: ToastPositionStrategy? = null
    )

    /**
     * 添加到Toast队列
     */
    fun addToQueue(
        context: Context,
        message: String,
        duration: Int = 1800,
        appearance: ToastAppearanceStrategy? = null,
        animation: ToastAnimationStrategy? = null,
        position: ToastPositionStrategy? = null
    ) {
        val task = ToastTask(context, message, duration, appearance, animation, position)
        toastQueue.offer(task)
        processQueue()
    }

    /**
     * 处理队列中的Toast
     */
    private fun processQueue() {
        coroutineScope.launch {
            mutex.withLock {
                if (isProcessing || toastQueue.isEmpty()) return@launch

                isProcessing = true

                while (toastQueue.isNotEmpty()) {
                    val task = toastQueue.poll() ?: break
                    try {
                        val toast = ToastGT.Builder(task.context)
                            .setMessage(task.message)
                            .setDuration(task.duration)
                            .apply {
                                task.appearance?.let { setAppearance(it) }
                                task.animation?.let { setAnimation(it) }
                                task.position?.let { setPosition(it) }
                            }
                            .build()

                        // 同步等待Toast完成
                        toast.showAndWait()

                        // 添加短暂间隔，确保用户体验
                        delay(100L)
                    } catch (e: Exception) {
                        // 处理异常，继续下一个
                        e.printStackTrace()
                    }
                }

                isProcessing = false
            }
        }
    }


    /**
     * 清空队列
     */
    fun clearQueue() {
        toastQueue.clear()
    }

    /**
     * 获取队列大小
     */
    fun getQueueSize(): Int = toastQueue.size
}