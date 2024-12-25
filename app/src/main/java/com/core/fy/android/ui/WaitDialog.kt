package com.core.fy.android.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import com.core.fy.android.R
import com.core.libraries.base.action.AnimAction
import com.core.libraries.base.dialog.BaseDialog


class WaitDialog {

    class Builder(context: Context) : BaseDialog.Builder<Builder>(context) {

        private val messageView: TextView? by lazy { findViewById(R.id.tv_wait_message) }

        init {
            setContentView(R.layout.wait_dialog)
            setAnimStyle(AnimAction.ANIM_TOAST)
            setBackgroundDimEnabled(false)
            setCancelable(false)
        }

        fun setMessage(@StringRes id: Int): Builder = apply {
            setMessage(getString(id))
        }

        fun setMessage(text: CharSequence?): Builder = apply {
            messageView?.text = text
            messageView?.visibility = if (text == null) View.GONE else View.VISIBLE
        }
    }
}