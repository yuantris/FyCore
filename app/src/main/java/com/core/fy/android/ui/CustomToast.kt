package com.core.fy.android.ui

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import io.core.R
import io.core.common.util.extensions.cool.dpToPx
import io.core.common.util.extensions.windowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface ToastConfig {
    data class Params(
        val context: Context,
        var message: String = "",
        var duration: Int = 1800,
        var gravity: Int = Gravity.BOTTOM,
        var offsetX: Int = 0,
        var offsetY: Int = 120.dpToPx()
    ) : ToastConfig

    data class ViewParam(
        var layoutId: Int = R.layout.layout_blut_toast,
        var textViewId: Int = R.id.tv_message
    ) : ToastConfig
}

class CustomToast private constructor(
    private val config: ToastConfig.Params,
    private val viewConfig: ToastConfig.ViewParam
) {
    private var toastView: View? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()

    init {
        require(config.context is Activity) { "Context must be an Activity instance" }
    }

    fun show() {
        val context = config.context as Activity
        if (context.isFinishing || context.isDestroyed) return

        coroutineScope.launch {
            mutex.withLock {
                cancelPendingAnimations()
                showToastInternal(context, viewConfig)
                delay(config.duration.toLong())
                dismissWithAnimation()
            }
        }
    }

    fun dismiss() {
        coroutineScope.cancel("Toast dismissed manually")
        dismissWithAnimation()
    }

    private suspend fun showToastInternal(context: Activity, viewConfig: ToastConfig.ViewParam) {
        withContext(Dispatchers.Main) {
            toastView?.let { return@withContext }

            toastView = createToastView(context, viewConfig).apply {
                setupInitialState()
                addToWindowManager(context)
                playShowAnimation()
            }
        }
    }

    private fun createToastView(context: Context, viewConfig: ToastConfig.ViewParam): View {
        return LayoutInflater.from(context)
            .inflate(viewConfig.layoutId, null).apply {
                findViewById<TextView>(viewConfig.textViewId).text = config.message
            }
    }

    private fun View.setupInitialState() {
        alpha = 0f
        scaleX = 0.95f
        scaleY = 0.95f
    }

    private fun View.addToWindowManager(context: Context) {
        context.windowManager.addView(this, createLayoutParams())
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION, // 根据实际需求选择类型
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = config.gravity
            x = config.offsetX
            y = config.offsetY.takeIf { config.gravity != Gravity.CENTER } ?: 0
        }
    }

    private fun dismissWithAnimation() {
        toastView?.apply {
            playDismissAnimation {
                removeFromWindowSafely()
                toastView = null
            }
        }
    }

    private fun cancelPendingAnimations() {
        toastView?.apply {
            animate().cancel()
            removeFromWindowSafely()
        }
        toastView = null
    }

    private fun removeFromWindowSafely() {
        runCatching {
            (config.context as? Activity)?.windowManager?.removeViewImmediate(toastView)
        }
    }

    class Builder(context: Context) {
        private val config = ToastConfig.Params(context)
        private val viewConfig = ToastConfig.ViewParam()

        fun setMessage(message: String) = apply { config.message = message }
        fun setDuration(duration: Int) = apply { config.duration = duration.coerceAtLeast(500) }
        fun setGravity(gravity: Int) = apply { config.gravity = gravity }
        fun setOffset(x: Int, y: Int) = apply {
            config.offsetX = x
            config.offsetY = y
        }

        fun setLayout(layoutId: Int, textViewId: Int) = apply {
            viewConfig.layoutId = layoutId
            viewConfig.textViewId = textViewId
        }

        fun build(): CustomToast {
            return CustomToast(config, viewConfig)
        }
    }
}

// 动画扩展函数
private fun View.playShowAnimation(duration: Long = 300L) {
    animate().alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(duration)
        .start()
}

private fun View.playDismissAnimation(duration: Long = 300L, endAction: () -> Unit) {
    animate().alpha(0f)
        .scaleX(0.95f)
        .scaleY(0.95f)
        .setDuration(duration)
        .withEndAction(endAction)
        .start()
}