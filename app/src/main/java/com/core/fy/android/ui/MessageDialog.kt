package com.core.fy.android.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import com.core.fy.android.R
import io.core.ui.base.component.dialog.BaseDialog
import io.core.ui.base.component.dialog.specific.CommonDialog


class MessageDialog {

    class Builder(context: Context) : CommonDialog.Builder<Builder>(context) {

        private val messageView: TextView? by lazy { findViewById(R.id.tv_message_message) }

        private var onConfirm: ((BaseDialog?) -> Unit)? = null
        private var onCancel: ((BaseDialog?) -> Unit)? = null

        init {
            setCustomView(R.layout.message_dialog)
        }

        fun setMessage(@StringRes id: Int): Builder = apply {
            setMessage(getString(id))
        }

        fun setMessage(text: CharSequence?): Builder = apply {
            messageView?.text = text
        }

        fun setListener(
            onConfirm: (BaseDialog?) -> Unit = {},
            onCancel: (BaseDialog?) -> Unit = {}
        ): Builder = apply {
            this.onConfirm = onConfirm
            this.onCancel = onCancel
        }

        override fun create(): BaseDialog {
            // 如果内容为空就抛出异�?
            if (("" == messageView?.text.toString())) {
                throw IllegalArgumentException("Dialog message not null")
            }
            return super.create()
        }

        override fun onClick(view: View) {
            when (view.id) {
                R.id.tv_ui_confirm -> {
                    autoDismiss()
                    onConfirm?.invoke(getDialog())
                }
                R.id.tv_ui_cancel -> {
                    autoDismiss()
                    onCancel?.invoke(getDialog())
                }
            }
        }
    }
}