package io.core.widget.layout

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import io.core.R
import io.core.engine.shape.builder.ShapeDrawableBuilder
import io.core.engine.shape.config.IGetShapeDrawableBuilder
import io.core.engine.shape.styleable.ShapeRelativeLayoutStyleable


/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/21 14:45
 * @description
 * @author Yuan
 */
class ShapeRelativeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), IGetShapeDrawableBuilder {
    private val STYLEABLE = ShapeRelativeLayoutStyleable()
    private var mShapeDrawableBuilder: ShapeDrawableBuilder? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShapeRelativeLayout)
        mShapeDrawableBuilder = ShapeDrawableBuilder(this, typedArray, STYLEABLE)
        typedArray.recycle()

        mShapeDrawableBuilder?.intoBackground()
    }

    override fun getShapeDrawableBuilder(): ShapeDrawableBuilder? {
        return mShapeDrawableBuilder
    }
}