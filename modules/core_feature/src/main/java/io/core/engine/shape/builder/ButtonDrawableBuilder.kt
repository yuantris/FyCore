package io.core.engine.shape.builder

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.widget.CompoundButton
import androidx.core.widget.CompoundButtonCompat
import io.core.R
import io.core.engine.shape.config.ICompoundButtonStyleable

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/21 13:44
 * @description
 * @author Yuan
 */
class ButtonDrawableBuilder(
    private val mCompoundButton: CompoundButton,
    typedArray: TypedArray,
    styleable: ICompoundButtonStyleable
) {

    private var mButtonDrawable: Drawable? = null
    private var mButtonPressedDrawable: Drawable? = null
    private var mButtonCheckedDrawable: Drawable? = null
    private var mButtonDisabledDrawable: Drawable? = null
    private var mButtonFocusedDrawable: Drawable? = null
    private var mButtonSelectedDrawable: Drawable? = null

    init {
        if (typedArray.hasValue(styleable.getButtonDrawableStyleable())) {
            mButtonDrawable = if (typedArray.getResourceId(
                    styleable.getButtonDrawableStyleable(),
                    0
                ) != R.drawable.shape_view_placeholder
            ) {
                typedArray.getDrawable(styleable.getButtonDisabledDrawableStyleable())
            } else {
                CompoundButtonCompat.getButtonDrawable(mCompoundButton)
            }
        } else {
            mButtonDrawable = null
            mCompoundButton.setButtonDrawable(null)
        }

        if (typedArray.hasValue(styleable.getButtonPressedDrawableStyleable())) {
            mButtonPressedDrawable =
                typedArray.getDrawable(styleable.getButtonPressedDrawableStyleable())
        }

        if (typedArray.hasValue(styleable.getButtonCheckedDrawableStyleable())) {
            mButtonCheckedDrawable =
                typedArray.getDrawable(styleable.getButtonCheckedDrawableStyleable())
        }

        if (typedArray.hasValue(styleable.getButtonDisabledDrawableStyleable())) {
            mButtonDisabledDrawable =
                typedArray.getDrawable(styleable.getButtonDisabledDrawableStyleable())
        }

        if (typedArray.hasValue(styleable.getButtonFocusedDrawableStyleable())) {
            mButtonFocusedDrawable =
                typedArray.getDrawable(styleable.getButtonFocusedDrawableStyleable())
        }

        if (typedArray.hasValue(styleable.getButtonSelectedDrawableStyleable())) {
            mButtonSelectedDrawable =
                typedArray.getDrawable(styleable.getButtonSelectedDrawableStyleable())
        }
    }

    fun setButtonDrawable(drawable: Drawable?): ButtonDrawableBuilder {
        if (mButtonPressedDrawable == mButtonDrawable) {
            mButtonPressedDrawable = drawable
        }
        if (mButtonCheckedDrawable == mButtonDrawable) {
            mButtonCheckedDrawable = drawable
        }
        if (mButtonDisabledDrawable == mButtonDrawable) {
            mButtonDisabledDrawable = drawable
        }
        if (mButtonFocusedDrawable == mButtonDrawable) {
            mButtonFocusedDrawable = drawable
        }
        if (mButtonSelectedDrawable == mButtonDrawable) {
            mButtonSelectedDrawable = drawable
        }
        mButtonDrawable = drawable
        return this
    }

    fun getButtonDrawable(): Drawable? = mButtonDrawable

    fun setButtonPressedDrawable(drawable: Drawable?): ButtonDrawableBuilder {
        mButtonPressedDrawable = drawable
        return this
    }

    fun getButtonPressedDrawable(): Drawable? = mButtonPressedDrawable

    fun setButtonCheckedDrawable(drawable: Drawable?): ButtonDrawableBuilder {
        mButtonCheckedDrawable = drawable
        return this
    }

    fun getButtonCheckedDrawable(): Drawable? = mButtonCheckedDrawable

    fun setButtonDisabledDrawable(drawable: Drawable?): ButtonDrawableBuilder {
        mButtonDisabledDrawable = drawable
        return this
    }

    fun getButtonDisabledDrawable(): Drawable? = mButtonDisabledDrawable

    fun setButtonFocusedDrawable(drawable: Drawable?): ButtonDrawableBuilder {
        mButtonFocusedDrawable = drawable
        return this
    }

    fun getButtonFocusedDrawable(): Drawable? = mButtonFocusedDrawable

    fun setButtonSelectedDrawable(drawable: Drawable?): ButtonDrawableBuilder {
        mButtonSelectedDrawable = drawable
        return this
    }

    fun getButtonSelectedDrawable(): Drawable? = mButtonSelectedDrawable

    fun intoButtonDrawable() {
        if (mButtonDrawable == null) {
            return
        }

        if (mButtonPressedDrawable == null &&
            mButtonCheckedDrawable == null &&
            mButtonDisabledDrawable == null &&
            mButtonFocusedDrawable == null &&
            mButtonSelectedDrawable == null
        ) {
            mCompoundButton.buttonDrawable = mButtonDrawable
            return
        }

        val drawable = StateListDrawable()

        mButtonPressedDrawable?.let {
            drawable.addState(intArrayOf(android.R.attr.state_pressed), it)
        }

        mButtonCheckedDrawable?.let {
            drawable.addState(intArrayOf(android.R.attr.state_checked), it)
        }

        mButtonDisabledDrawable?.let {
            drawable.addState(intArrayOf(-android.R.attr.state_enabled), it)
        }

        mButtonFocusedDrawable?.let {
            drawable.addState(intArrayOf(android.R.attr.state_focused), it)
        }

        mButtonSelectedDrawable?.let {
            drawable.addState(intArrayOf(android.R.attr.state_selected), it)
        }

        drawable.addState(intArrayOf(), mButtonDrawable)
        mCompoundButton.buttonDrawable = drawable
    }
}
