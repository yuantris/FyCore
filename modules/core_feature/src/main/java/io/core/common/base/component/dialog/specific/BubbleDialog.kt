package io.core.common.base.component.dialog.specific

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import io.core.R
import io.core.common.util.tools.runOnUI

/**
 * 加载对话框
 * @param title 加载对话框的标题
 */
class BubbleDialog @JvmOverloads constructor(
    context: Context,
    private var title: String = context.getString(R.string.common_loading),
) : Dialog(context, R.style.BubbleDialog) {

    private var tvTitle: TextView? = null

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
        tvTitle?.text = title
    }

    override fun show() {
        runOnUI {
            super.show()
        }
    }

    /**
     * 更新标题文本
     */
    fun updateTitle(text: String) {
        if (isShowing) {
            runOnUI {
                tvTitle?.text = text
            }
        } else {
            title = text
        }
    }
}