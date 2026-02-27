package io.core.ui.widget.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import io.core.R
import io.core.engine.shape.builder.ButtonDrawableBuilder
import io.core.engine.shape.builder.ShapeDrawableBuilder
import io.core.engine.shape.builder.TextColorBuilder
import io.core.engine.shape.config.IGetButtonDrawableBuilder
import io.core.engine.shape.config.IGetShapeDrawableBuilder
import io.core.engine.shape.config.IGetTextColorBuilder
import io.core.engine.shape.styleable.ShapeRadioButtonStyleable


/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/1/21 14:31
 * @description
 * @author Yuan
 */
class ShapeRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatRadioButton(context, attrs, defStyleAttr), IGetShapeDrawableBuilder,
    IGetTextColorBuilder, IGetButtonDrawableBuilder {

    private val STYLEABLE = ShapeRadioButtonStyleable()

    private var mShapeDrawableBuilder: ShapeDrawableBuilder? = null
    private var mTextColorBuilder: TextColorBuilder? = null
    private var mButtonDrawableBuilder: ButtonDrawableBuilder? = null

    init {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.ShapeRadioButton,
            0,
            R.style.ShapeRadioButtonStyle
        )
        mShapeDrawableBuilder = ShapeDrawableBuilder(this, typedArray, STYLEABLE)
        mTextColorBuilder = TextColorBuilder(this, typedArray, STYLEABLE)
        mButtonDrawableBuilder = ButtonDrawableBuilder(this, typedArray, STYLEABLE)
        typedArray.recycle()

        mShapeDrawableBuilder?.intoBackground()

        mTextColorBuilder?.let {
            if (it.isTextGradientColorsEnable() || it.isTextStrokeColorEnable()) {
                text = text
            } else {
                it.intoTextColor()
            }
        }
        mButtonDrawableBuilder?.intoButtonDrawable()
    }

    override fun setTextColor(color: Int) {
        super.setTextColor(color)
        mTextColorBuilder?.setTextColor(color);
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        mTextColorBuilder?.let {
            if (it.isTextGradientColorsEnable() || it.isTextStrokeColorEnable()) {
                super.setText(it.buildTextSpannable(text), type)
            } else {
                super.setText(text, type)
            }
        } ?: super.setText(text, type)
    }

    override fun setButtonDrawable(buttonDrawable: Drawable?) {
        super.setButtonDrawable(buttonDrawable)
        mButtonDrawableBuilder?.setButtonDrawable(buttonDrawable)
    }

    override fun getShapeDrawableBuilder(): ShapeDrawableBuilder? {
        return mShapeDrawableBuilder
    }

    override fun getTextColorBuilder(): TextColorBuilder? {
        return mTextColorBuilder
    }

    override fun getButtonDrawableBuilder(): ButtonDrawableBuilder? {
        return mButtonDrawableBuilder
    }
}