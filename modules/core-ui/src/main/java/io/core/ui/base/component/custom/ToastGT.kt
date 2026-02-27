package io.core.ui.base.component.custom

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.core.ui.base.component.custom.strategy.ToastAnimationStrategy
import io.core.ui.base.component.custom.strategy.ToastAppearanceStrategy
import io.core.ui.base.component.custom.strategy.ToastPositionStrategy
import io.core.utils.extensions.windowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 自定义吐司类
 * 使用策略模式实现外观、动画、位置的完全解�?
 */
class ToastGT private constructor(
    private val context: Context,
    private val message: String,
    private val duration: Int,
    private val appearance: ToastAppearanceStrategy,
    private val animationStrategy: ToastAnimationStrategy,
    private val position: ToastPositionStrategy
) : LifecycleObserver {
    private var toastView: View? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        coroutineScope.cancel("Activity destroyed")
        toastView?.let { removeFromWindowSafely() }
    }

    init {
        require(context is Activity) { "Context must be an Activity instance" }
        if (context is LifecycleOwner) {
            (context as LifecycleOwner).lifecycle.addObserver(this)
        }
    }

    /**
     * 显示吐司
     */
    fun show() {
        val activity = context as Activity
        if (activity.isFinishing || activity.isDestroyed) return

        coroutineScope.launch {
            mutex.withLock {
                cancelPendingAnimations()
                showToastInternal()
                delay(duration.toLong())
                dismissWithAnimation()
            }
        }
    }

    /**
     * 同步显示Toast并等待完�?
     * @return 返回一个CompletableDeferred，当Toast显示完成时完�?
     */
    suspend fun showAndWait(): Unit = withContext(Dispatchers.Main) {
        val activity = context as Activity
        if (activity.isFinishing || activity.isDestroyed) return@withContext

        mutex.withLock {
            cancelPendingAnimations()
            showToastInternal()
            delay(duration.toLong())
            dismissWithAnimation()
            // 等待动画完成
            delay(300L) // 等待消失动画完成
        }
    }

    /**
     * 立即关闭吐司
     */
    fun dismiss() {
        coroutineScope.cancel("Toast dismissed manually")
        dismissWithAnimation()
    }

    private suspend fun showToastInternal() {
        withContext(Dispatchers.Main) {
            toastView?.let { return@withContext }

            toastView = appearance.createToastView(context, message).apply {
                addToWindowManager()
                playShowAnimation()
            }
        }
    }

    private fun View.addToWindowManager() {
        context.windowManager.addView(this, position.createLayoutParams(context))
    }

    private fun View.playShowAnimation(duration: Long = 300L) {
        animationStrategy.playShowAnimation(this, duration)
    }

    private fun dismissWithAnimation() {
        toastView?.apply {
            animationStrategy.playDismissAnimation(this, 300L) {
                removeFromWindowSafely()
                toastView = null
            }
        }
    }

    private fun cancelPendingAnimations() {
        toastView?.apply {
            removeFromWindowSafely()
        }
        toastView = null
    }

    private fun removeFromWindowSafely() {
        toastView?.let { view ->
            runCatching {
                (context as? Activity)?.windowManager?.removeViewImmediate(view)
            }.onFailure { e ->
                if (e !is IllegalArgumentException) {
                    // 记录非预期的异常
                    Log.w("ToastGT", "Failed to remove toast view", e)
                }
            }
        }
    }

    /**
     * 建造者类
     */
    class Builder(private val context: Context) {
        private var message: String = ""
        private var duration: Int = 2000
        private var appearance: ToastAppearanceStrategy = ToastConfigManager.getDefaultAppearance()
        private var animation: ToastAnimationStrategy = ToastConfigManager.getDefaultAnimation()
        private var position: ToastPositionStrategy = ToastConfigManager.getDefaultPosition()

        fun setMessage(message: String) = apply { this.message = message }
        fun setDuration(duration: Int) = apply { this.duration = duration.coerceAtLeast(500) }

        fun setAppearance(appearance: ToastAppearanceStrategy) =
            apply { this.appearance = appearance }

        fun setAnimation(animation: ToastAnimationStrategy) = apply { this.animation = animation }
        fun setPosition(position: ToastPositionStrategy) = apply { this.position = position }

        fun build(): ToastGT {
            return ToastGT(context, message, duration, appearance, animation, position)
        }

        fun show() {
            build().show()
        }
    }

    companion object {

        /**
         * 快捷显示方法，使用全局默认配置
         */
        @JvmStatic
        @JvmOverloads
        fun show(context: Context, message: String, duration: Int = 2000) {
            Builder(context)
                .setMessage(message)
                .setDuration(duration)
                .show()
        }

        /**
         * 使用队列显示Toast
         */
        @JvmStatic
        @JvmOverloads
        fun showWithQueue(
            context: Context,
            message: String,
            duration: Int = 2000,
            appearance: ToastAppearanceStrategy? = null,
            animation: ToastAnimationStrategy? = null,
            position: ToastPositionStrategy? = null
        ) {
            ToastQueueManager.addToQueue(
                context, message, duration, appearance, animation, position
            )
        }

        /**
         * 清空Toast队列
         */
        @JvmStatic
        fun clearQueue() {
            ToastQueueManager.clearQueue()
        }

        /**
         * 获取队列大小
         */
        @JvmStatic
        fun getQueueSize(): Int = ToastQueueManager.getQueueSize()
    }
}