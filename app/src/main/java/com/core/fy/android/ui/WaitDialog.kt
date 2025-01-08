package com.core.fy.android.ui

import android.animation.ValueAnimator
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
            animateRootViewSize()
        }

        private fun animateRootViewSize() {
            val rootView = getContentView() ?: return
            val currentWidth = rootView.width
            val currentHeight = rootView.height

            // 测量根布局的新宽高
            rootView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val targetWidth = rootView.measuredWidth
            val targetHeight = rootView.measuredHeight

            if (currentWidth == targetWidth && currentHeight == targetHeight) {
                return // 宽高未改变，无需动画
            }

            // 使用 ValueAnimator 动画
            val widthAnimator = ValueAnimator.ofInt(currentWidth, targetWidth)
            val heightAnimator = ValueAnimator.ofInt(currentHeight, targetHeight)

            widthAnimator.addUpdateListener { animation ->
                val newWidth = animation.animatedValue as Int
                val params = rootView.layoutParams
                params.width = newWidth
                rootView.layoutParams = params
            }

            heightAnimator.addUpdateListener { animation ->
                val newHeight = animation.animatedValue as Int
                val params = rootView.layoutParams
                params.height = newHeight
                rootView.layoutParams = params
            }

            // 同时启动宽高动画
            widthAnimator.duration = 300 // 动画时长
            heightAnimator.duration = 300
            widthAnimator.start()
            heightAnimator.start()
        }
    }
}