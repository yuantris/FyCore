package io.core.ui.widget.layout

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.core.R
import io.core.engine.shape.builder.ShapeDrawableBuilder
import io.core.engine.shape.config.IGetShapeDrawableBuilder
import io.core.engine.shape.styleable.ShapeConstraintLayoutStyleable


/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/1/21 14:55
 * @description
 * @author Yuan
 */
class ShapeConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), IGetShapeDrawableBuilder {
    private val STYLEABLE = ShapeConstraintLayoutStyleable()

    private var mShapeDrawableBuilder: ShapeDrawableBuilder? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShapeConstraintLayout)
        mShapeDrawableBuilder = ShapeDrawableBuilder(this, typedArray, STYLEABLE)
        typedArray.recycle()

        mShapeDrawableBuilder?.intoBackground()
    }

    override fun getShapeDrawableBuilder(): ShapeDrawableBuilder? {
        return mShapeDrawableBuilder
    }
}