package io.core.ui.base.component.dialog

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.annotation.LayoutRes

class CustomPopupWindow private constructor(
    context: Context,
    @LayoutRes private val layoutId: Int,
    private val width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    private val height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    private val initView: (View.(PopupWindow) -> Unit)? = null
) : BasePopup<CustomPopupWindow.Builder>(context) {

    private var popupWindow: PopupWindow? = null

    override fun show() {
        dismiss() // 防止重复显示
        val contentView = LayoutInflater.from(context).inflate(layoutId, null)
        popupWindow?.let {
            initView?.invoke(contentView, it)
        } ?: run {
            PopupWindow(contentView, width, height).apply {
                isFocusable = true
                isOutsideTouchable = isCancelable
                setBackgroundDrawable(ColorDrawable(0))
                anim = PopupAnimation.FADE
                animationStyle = createAnimationStyle()

                setOnDismissListener {
                    onDismissListener?.invoke()
                    popupWindow = null
                }

                popupWindow = this
            }.also {
                onShowListener?.invoke()
                initView?.invoke(contentView, it)
                // playEnterAnimation(it.contentView)
            }
        }
    }

    fun showAtLocation(parent: View, gravity: Int, x: Int = 0, y: Int = 0) {
        popupWindow?.showAtLocation(parent, gravity, x, y)
    }

    fun showAsDropDown(anchor: View, xoff: Int = 0, yoff: Int = 0) {
        popupWindow?.showAsDropDown(anchor, xoff, yoff)
    }

    override fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    private fun createAnimationStyle(): Int {
        return getAnimationResources(anim)
    }


    class Builder(private val context: Context) : BasePopup.Builder<Builder>() {
        private var layoutId = 0
        private var width = ViewGroup.LayoutParams.WRAP_CONTENT
        private var height = ViewGroup.LayoutParams.WRAP_CONTENT
        private var initView: (View.(PopupWindow) -> Unit)? = null

        fun setLayout(@LayoutRes layoutId: Int): Builder {
            this.layoutId = layoutId
            return self()
        }

        fun setSize(width: Int, height: Int): Builder {
            this.width = width
            this.height = height
            return self()
        }

        fun setViewInitializer(block: View.(PopupWindow) -> Unit): Builder {
            this.initView = block
            return self()
        }

        fun build(): CustomPopupWindow {
            return CustomPopupWindow(
                context = context,
                layoutId = layoutId,
                width = width,
                height = height,
                initView = initView
            ).apply {
                isCancelable = this@Builder.isCancelable
                dimAmount = this@Builder.dimAmount
                anim = this@Builder.popupAnimation
                onDismissListener = this@Builder.onDismissListener
                onShowListener = this@Builder.onShowListener
            }
        }
    }
}

// 扩展函数
fun View.showPopupWindow(block: CustomPopupWindow.Builder.() -> Unit): CustomPopupWindow {
    return CustomPopupWindow.Builder(context).apply(block).build().also {
        it.show()
        it.showAsDropDown(this)
    }
}