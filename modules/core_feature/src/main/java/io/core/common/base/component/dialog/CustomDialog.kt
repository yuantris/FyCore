package io.core.common.base.component.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import io.core.R
import io.core.common.util.ext.logE

class CustomDialog private constructor(
    context: Context,
    @StyleRes themeResId: Int,
    @LayoutRes private val layoutId: Int,
    private val width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    private val height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    private val gravity: Int = Gravity.CENTER,
    private val initView: (View.(Dialog) -> Unit)? = null,
) : BasePopup<CustomDialog.Builder>(context, themeResId) {

    private var dialog: Dialog? = null

    override fun show() {
        dialog?.let {
            if (!it.isShowing) {
                it.show()
            }
            return
        }

        val dialog = Dialog(context, themeResId).apply {
            setContentView(layoutId)
            window?.apply {
                setLayout(width, height)
                setGravity(gravity)
                setBackgroundDrawable(ColorDrawable(0))
                setDimAmount(dimAmount)
            }
            setCancelable(isCancelable)
            setOnShowListener { onShowListener?.invoke() }
            setOnDismissListener { onDismissListener?.invoke() }
        }

        dialog.window?.decorView?.let {
            initView?.invoke(it.findViewById<View>(android.R.id.content), dialog)
        }

        dialog.window?.setWindowAnimations(getAnimationResources(anim))
        dialog.show()
        this.dialog = dialog
    }

    override fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    class Builder(private var context: Context) : BasePopup.Builder<Builder>() {
        private var layoutId = 0
        private var width = ViewGroup.LayoutParams.WRAP_CONTENT
        private var height = ViewGroup.LayoutParams.WRAP_CONTENT
        private var gravity = Gravity.CENTER
        private var themeResId = 0
        private var initView: (View.(Dialog) -> Unit)? = null

        fun setLayout(@LayoutRes layoutId: Int): Builder {
            this.layoutId = layoutId
            return self()
        }

        fun setSize(width: Int, height: Int): Builder {
            this.width = width
            this.height = height
            return self()
        }

        fun setGravity(gravity: Int): Builder {
            this.gravity = gravity
            return self()
        }

        fun setTheme(@StyleRes themeResId: Int): Builder {
            this.themeResId = themeResId
            return self()
        }

        fun setViewInitializer(block: View.(Dialog) -> Unit): Builder {
            this.initView = block
            return self()
        }

        fun build(): CustomDialog {
            return CustomDialog(
                context = context,
                themeResId = themeResId,
                layoutId = layoutId,
                width = width,
                height = height,
                gravity = gravity,
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
fun Context.showCustomDialog(block: CustomDialog.Builder.() -> Unit): CustomDialog {
    return CustomDialog.Builder(this).apply(block).build().also { it.show() }
}