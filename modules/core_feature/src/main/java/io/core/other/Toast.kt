package io.core.other

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import io.core.R
import io.core.common.util.log.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference

class Toast private constructor(
    private val context: Context,
    private val message: String,
    private val duration: Int,
    private val gravity: Int,
    private val offsetX: Int,
    private val offsetY: Int
) {

    private val inflater: LayoutInflater by lazy {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    private var easyToastView: WeakReference<View>? = null
    private var mToastJob: Job? = null

    private val mutex = Mutex()

    fun show() {
        if (context !is Activity || context.isFinishing || context.isDestroyed) {
            return
        }

        cancel()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (easyToastView?.get() == null) {
            easyToastView = WeakReference(inflater.inflate(R.layout.layout_blut_toast, null))
        }

        val textView = easyToastView?.get()?.findViewById<TextView>(R.id.tv_message)
        textView?.text = message

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = gravity
        params.x = offsetX
        params.y = offsetY

        easyToastView?.get()?.let { view ->
            CoroutineScope(Dispatchers.Main).launch {
                if (view.parent == null) {
                    try {
                        view.alpha = 0f // 初始透明度
                        view.scaleX = 0.95f // 初始缩放比例
                        view.scaleY = 0.95f
                        windowManager.addView(view, params)

                        // 添加动画：淡入
                        view.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .start()
                    } catch (e: Exception) {
                        e.message?.logE()
                    }
                }
            }
        }

        mToastJob = CoroutineScope(Dispatchers.Main).launch {
            mutex.withLock {
                delay(duration.toLong())
                // 添加动画：淡出并销毁
                easyToastView?.get()?.animate()
                    ?.alpha(0f)
                    ?.scaleX(0.95f)
                    ?.scaleY(0.95f)
                    ?.setDuration(300)
                    ?.withEndAction {
                        destroy()
                    }
                    ?.start()
            }
        }
    }

    fun cancel() {
        mToastJob?.cancel()
        easyToastView?.get()?.animate()
            ?.alpha(0f)
            ?.scaleX(0.95f)
            ?.scaleY(0.95f)
            ?.setDuration(300)
            ?.withEndAction {
                destroy()
            }
            ?.start()
    }

    private fun destroy() {
        CoroutineScope(Dispatchers.Main).launch {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            easyToastView?.get()?.let {
                if (it.parent != null) {
                    windowManager.removeViewImmediate(it)
                }
            }
            easyToastView = null
        }
    }

    class Builder(private val context: Context) {
        private var message: String = ""
        private var duration: Int = 1500
        private var gravity: Int = Gravity.BOTTOM
        private var offsetX: Int = 0
        private var offsetY: Int = 300 // 默认底部偏移

        fun setMessage(message: String) = apply { this.message = message }
        fun setDuration(duration: Int) = apply { this.duration = duration }
        fun setGravity(gravity: Int) = apply { this.gravity = gravity }
        fun setOffset(x: Int, y: Int) = apply {
            this.offsetX = x
            this.offsetY = y
        }

        fun create(): Toast {
            return Toast(context, message, duration, gravity, offsetX, offsetY)
        }
    }
}
