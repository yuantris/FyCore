package io.core.widget.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import io.core.widget.R
import io.core.engine.shape.builder.ShapeDrawableBuilder
import io.core.engine.shape.builder.TextColorBuilder
import io.core.engine.shape.config.IGetShapeDrawableBuilder
import io.core.engine.shape.config.IGetTextColorBuilder
import io.core.engine.shape.styleable.ShapeEditTextStyleable


/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/21 14:39
 * @description
 * @author Yuan
 */
class ShapeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatEditText(context, attrs, defStyleAttr), IGetShapeDrawableBuilder,
    IGetTextColorBuilder {


    private val STYLEABLE = ShapeEditTextStyleable()

    private var mShapeDrawableBuilder: ShapeDrawableBuilder? = null
    private var mTextColorBuilder: TextColorBuilder? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShapeEditText)
        mShapeDrawableBuilder = ShapeDrawableBuilder(this, typedArray, STYLEABLE)
        mTextColorBuilder = TextColorBuilder(this, typedArray, STYLEABLE)
        typedArray.recycle()

        mShapeDrawableBuilder?.intoBackground()

        mTextColorBuilder?.let {
            if (it.isTextGradientColorsEnable() || it.isTextStrokeColorEnable()) {
                text = text
            } else {
                it.intoTextColor()
            }
        }
    }

    override fun setTextColor(color: Int) {
        super.setTextColor(color)
        mTextColorBuilder?.setTextColor(color)
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

    override fun getShapeDrawableBuilder(): ShapeDrawableBuilder? {
        return mShapeDrawableBuilder
    }

    override fun getTextColorBuilder(): TextColorBuilder? {
        return mTextColorBuilder
    }
}