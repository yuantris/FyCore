package io.core.ui.base.component.dialog

import android.content.Context
import androidx.annotation.StyleRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import io.core.R

sealed class BasePopup<B : BasePopup.Builder<B>>(
    protected val context: Context,
    @StyleRes protected val themeResId: Int = 0
) {
    // 通用配置参数
    protected var isCancelable = true
    protected var dimAmount = 0.5f
    protected var anim = PopupAnimation.FADE
    protected var onDismissListener: (() -> Unit)? = null
    protected var onShowListener: (() -> Unit)? = null

    abstract fun show()
    abstract fun dismiss()

    enum class PopupAnimation {
        SLIDE_BOTTOM, SLIDE_TOP, FADE, SCALE, NONE
    }

    protected fun getAnimationResources(animation: PopupAnimation): Int {
        return when (animation) {
            PopupAnimation.SLIDE_BOTTOM -> R.style.BottomAnimStyle
            PopupAnimation.SLIDE_TOP -> R.style.TopAnimStyle
            PopupAnimation.FADE -> R.style.IOSAnimStyle
            PopupAnimation.SCALE -> R.style.ScaleAnimStyle
            PopupAnimation.NONE -> 0
        }
    }

    inline fun <reified VM : ViewModel> BasePopup<*>.getViewModel(
        owner: ViewModelStoreOwner,
        factory: ViewModelProvider.Factory? = null
    ): VM {
        return if (factory == null) {
            ViewModelProvider(owner)[VM::class.java]
        } else {
            ViewModelProvider(owner, factory)[VM::class.java]
        }
    }

    abstract class Builder<B : Builder<B>> {
        protected var isCancelable = true
        protected var dimAmount = 0.5f
        protected var onDismissListener: (() -> Unit)? = null
        protected var onShowListener: (() -> Unit)? = null
        protected var popupAnimation = PopupAnimation.NONE

        fun setCancelable(cancelable: Boolean): B {
            this.isCancelable = cancelable
            return self()
        }

        fun setDimAmount(amount: Float): B {
            this.dimAmount = amount
            return self()
        }

        fun setAnimation(animation: PopupAnimation): B {
            this.popupAnimation = animation
            return self()
        }

        fun setOnDismissListener(listener: () -> Unit): B {
            this.onDismissListener = listener
            return self()
        }

        fun setOnShowListener(listener: () -> Unit): B {
            this.onShowListener = listener
            return self()
        }

        @Suppress("UNCHECKED_CAST")
        protected fun self(): B = this as B
    }
}
