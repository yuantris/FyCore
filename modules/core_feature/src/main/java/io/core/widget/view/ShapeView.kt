package io.core.widget.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.core.R
import io.core.engine.shape.builder.ShapeDrawableBuilder
import io.core.engine.shape.config.IGetShapeDrawableBuilder
import io.core.engine.shape.styleable.ShapeViewStyleable


/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/21 13:48
 * @description
 * @author Yuan
 */
class ShapeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), IGetShapeDrawableBuilder {
    private var mShapeDrawableBuilder: ShapeDrawableBuilder

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShapeView)
        mShapeDrawableBuilder = ShapeDrawableBuilder(this, typedArray, ShapeViewStyleable())
        typedArray.recycle()

        mShapeDrawableBuilder.intoBackground()
    }

    override fun getShapeDrawableBuilder(): ShapeDrawableBuilder {
        return mShapeDrawableBuilder
    }
}