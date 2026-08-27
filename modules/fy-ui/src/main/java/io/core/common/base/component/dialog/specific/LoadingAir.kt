package io.core.common.base.component.dialog.specific

import io.core.Android
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.toColorInt
import androidx.core.view.updateLayoutParams
import io.core.ui.R
import io.core.appCtx
import io.core.common.util.extensions.cool.dpToPx
import io.core.common.util.extensions.ui.layout2View
import io.core.widget.view.OrbitLoadingView
import java.lang.ref.WeakReference

/**
 * 基于OrbitLoadingView的加载对话框
 * @param context 上下文对象
 * @param cancelable 是否可取消
 */
class LoadingAir(context: Context, private val cancelable: Boolean = false) :
    Dialog(context, R.style.BubbleDialog) {

    private lateinit var loadingView: OrbitLoadingView
    private lateinit var messageTextView: TextView

    init {
        // 设置无标题栏样式
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        // 设置背景透明
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootView = appCtx.layout2View(R.layout.core_loading_dialog)
        setContentView(rootView)
        loadingView = findViewById(R.id.loading_view)
        messageTextView = findViewById(R.id.message_text)
        loadingView.setColor(globalConfig.loadingColor)
        messageTextView.setTextColor(globalConfig.messageColor)
        messageTextView.text = globalConfig.message
        messageTextView.typeface = globalConfig.typeface

        globalConfig.background?.let {
            findViewById<CardView>(R.id.root_view).apply {
                setCardBackgroundColor(it)
            }
        }

        // 设置对话框属性
        setCancelable(cancelable)
        setCanceledOnTouchOutside(cancelable)
    }


    override fun show() {
        super.show()
        loadingView.startAnimation()
    }

    override fun dismiss() {
        loadingView.stopAnimation()
        super.dismiss()
    }

    /**
     * 更新对话框显示文本
     * @param text 要显示的文本内容
     */
    private fun reMsg(text: String) {
        messageTextView.text = text
        if (text.isNotBlank()) {
            messageTextView.updateLayoutParams<LinearLayout.LayoutParams> {
                marginEnd = 16.dpToPx()
            }
        }
    }

    /**
     * 对话框配置类
     */
    data class Config(
        val cancelable: Boolean = false,
        val background: Int? = null,
        val loadingColor: Int = "#333333".toColorInt(),
        val messageColor: Int = "#333333".toColorInt(),
        val typeface: android.graphics.Typeface? = null,
        val message: String = ""
    ) {
        /**
         * 配置构建器
         */
        class Builder {
            private var cancelable: Boolean = false
            private var background: Int? = null
            private var loadingColor: Int = "#333333".toColorInt()
            private var messageColor: Int = "#333333".toColorInt()
            private var typeface: android.graphics.Typeface? = null
            private var message: String = ""

            fun setCancelable(cancelable: Boolean) = apply { this.cancelable = cancelable }
            fun setBackground(drawableColor: Int) = apply { this.background = drawableColor }
            fun setLoadingColor(color: Int) = apply { this.loadingColor = color }
            fun setMessageColor(color: Int) = apply { this.messageColor = color }
            fun setTypeface(typeface: android.graphics.Typeface?) =
                apply { this.typeface = typeface }

            fun setMessage(message: String) = apply { this.message = message }

            fun build() = Config(
                cancelable,
                background,
                loadingColor,
                messageColor,
                typeface,
                message
            )
        }
    }

    companion object {

        private var weakInstance: WeakReference<LoadingAir>? = null

        /**
         * 全局配置单例
         */
        private var globalConfig: Config = Config()

        /**
         * 获取全局配置
         */
        @JvmStatic
        fun getGlobalConfig(): Config = globalConfig

        /**
         * 更新全局配置
         */
        @JvmStatic
        fun updateGlobalConfig(config: Config) {
            globalConfig = config
        }

        /**
         * 创建并显示加载对话框
         * @param cancelable 是否可取消，默认为false
         * @return 创建的对话框实例
         */
        @JvmStatic
        @JvmOverloads
        @Synchronized
        fun show(
            text: String = globalConfig.message,
            cancelable: Boolean = globalConfig.cancelable
        ): LoadingAir? {
            return runCatching {
                val activity = Android.topActivity ?: return null
                weakInstance?.get()?.dismiss()
                LoadingAir(activity, cancelable).also {
                    weakInstance = WeakReference(it)
                    it.show()
                    it.reMsg(text)
                }
            }.getOrNull()
        }

        @JvmStatic
        @Synchronized
        fun updateMessage(text: String) {
            weakInstance?.get()?.reMsg(text)
        }

        @JvmStatic
        @Synchronized
        fun close() {
            weakInstance?.get()?.dismiss()
            weakInstance = null
        }

        @Synchronized
        fun closeWith(next: () -> Unit) {
            weakInstance?.get()?.dismiss()
            weakInstance = null
            next()
        }
    }
}