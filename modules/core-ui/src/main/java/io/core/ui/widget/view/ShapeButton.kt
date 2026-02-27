package io.core.ui.widget.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import io.core.R
import io.core.engine.shape.builder.ShapeDrawableBuilder
import io.core.engine.shape.builder.TextColorBuilder
import io.core.engine.shape.config.IGetShapeDrawableBuilder
import io.core.engine.shape.config.IGetTextColorBuilder
import io.core.engine.shape.styleable.ShapeButtonStyleable


/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/1/21 14:43
 * @description
 * @author Yuan
 */
class ShapeButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr), IGetShapeDrawableBuilder, IGetTextColorBuilder {
    private val STYLEABLE = ShapeButtonStyleable()

    private var mShapeDrawableBuilder: ShapeDrawableBuilder? = null
    private var mTextColorBuilder: TextColorBuilder? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShapeButton)
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