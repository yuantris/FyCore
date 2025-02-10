package com.core.fy.android.ui

import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.annotation.StringRes
import com.core.fy.android.R
import io.core.common.base.action.AnimAction
import io.core.common.base.component.dialog.BaseDialog


class WaitDialog {

    class Builder(context: Context) : BaseDialog.Builder<Builder>(context) {

        private val messageView: TextView? by lazy { findViewById(R.id.tv_wait_message) }

        init {
            setContentView(R.layout.wait_dialog)
            setAnimStyle(AnimAction.ANIM_DEFAULT)
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

        override fun show() {
            animateRootViewAppearing()
            super.show()
        }

        private fun animateRootViewAppearing() {
            val rootView = getContentView() ?: return

            // 设置初始状态：缩小到 0
            rootView.scaleX = 0.8f
            rootView.scaleY = 0.8f
            rootView.alpha = 0f

            // 开始动画：从小变大，且逐渐显现
            rootView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(200) // 动画时长
                .setInterpolator(LinearInterpolator()) // 弹性插值器，视觉更自然
                .start()
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