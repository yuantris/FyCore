package io.core.common.base.component.dialog.specific

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import io.core.R
import io.core.common.util.extensions.cool.runDelayedMain
import io.core.common.util.extensions.cool.runMain
import io.core.common.util.extensions.ui.invisible
import io.core.widget.view.LoadingView
import io.core.widget.view.StatusView

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