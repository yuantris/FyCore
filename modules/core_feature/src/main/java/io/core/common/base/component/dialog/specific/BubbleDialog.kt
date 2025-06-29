package io.core.common.base.component.dialog.specific

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import io.core.R
import io.core.common.helper.track.AppTrackV2
import io.core.common.util.extensions.cool.runDelayedMain
import io.core.common.util.extensions.cool.runMain
import io.core.common.util.extensions.ui.invisible
import io.core.widget.view.LoadingView
import io.core.widget.view.StatusView
import java.lang.ref.WeakReference

/**
 * 加载状态对话框
 * @param title 加载对话框的标题
 */
class BubbleDialog @JvmOverloads constructor(
    context: Context,
    private var title: String = context.getString(R.string.common_loading),
) : Dialog(context, R.style.BubbleDialog) {
    private lateinit var tvTitle: TextView
    private lateinit var ivStatus: StatusView
    private lateinit var ivLoading: LoadingView

    companion object {
        private const val DISMISS_DELAY = 1500L

        private var weakInstance: WeakReference<BubbleDialog>? = null

        /**
         * 创建并显示加载对话框
         * @param context 上下文对象
         * @return 创建的对话框实例
         */
        @JvmStatic
        @Synchronized
        fun show(text: String? = null): BubbleDialog? {
            return runCatching {
                val activity = AppTrackV2.getTopActivity() ?: return null
                weakInstance?.get()?.dismiss()
                BubbleDialog(activity).also {
                    weakInstance = WeakReference(it)
                    it.show()
                    text?.let { dialogText -> it.updateTitle(dialogText) }
                }
            }.getOrNull()
        }

        @JvmStatic
        @Synchronized
        fun updateMessage(text: String) {
            weakInstance?.get()?.updateTitle(text)
        }

        @JvmStatic
        @Synchronized
        fun success(text: String) {
            weakInstance?.get()?.showSuccess(text)
        }

        @JvmStatic
        @Synchronized
        fun error(text: String) {
            weakInstance?.get()?.showError(text)
        }

        @JvmStatic
        @Synchronized
        fun warning(text: String) {
            weakInstance?.get()?.showWarning(text)
        }

        @JvmStatic
        @Synchronized
        fun close() {
            weakInstance?.get()?.dismiss()
            weakInstance = null
        }
    }


    override fun onStart() {
        super.onStart()
        setCancelable(false)
        // 设置对话框居中显示
        window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.gravity = Gravity.CENTER
            window.attributes = layoutParams
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_bubble_dialog)
        tvTitle = findViewById(R.id.tv_title)
        ivStatus = findViewById(R.id.iv_status)
        ivLoading = findViewById(R.id.iv_loading)

        tvTitle.text = title
        ivStatus.invisible()
    }


    override fun show() = runMain {
        super.show()
    }


    // 提取状态处理公共方法
    private fun handleStatus(status: Int, text: String) {
        if (!isShowing) show()
        runMain {
            ivStatus.apply {
                setStatus(status)
                updateTitle(text, false)
                runDelayedMain(DISMISS_DELAY) { dismiss() }
            }
        }
    }

    // 状态显示方法
    fun showSuccess(text: String) = handleStatus(StatusView.STATUS_SUCCESS, text)
    fun showError(text: String) = handleStatus(StatusView.STATUS_FAILURE, text)
    fun showWarning(text: String) = handleStatus(StatusView.STATUS_WARNING, text)

    // 优化后的标题更新方法
    @JvmOverloads
    fun updateTitle(text: String, isLoading: Boolean = true) = runMain {
        if (isShowing.not()) {
            title = text
            show()
        }

        ivLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        ivStatus.visibility = if (isLoading) View.GONE else View.VISIBLE
        tvTitle.text = text
    }

}