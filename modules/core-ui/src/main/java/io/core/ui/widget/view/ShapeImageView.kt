package io.core.ui.widget.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import io.core.R
import io.core.engine.shape.builder.ShapeDrawableBuilder
import io.core.engine.shape.config.IGetShapeDrawableBuilder
import io.core.engine.shape.styleable.ShapeImageViewStyleable


/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/1/21 14:37
 * @description
 * @author Yuan
 */
class ShapeImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), IGetShapeDrawableBuilder {
    private val STYLEABLE = ShapeImageViewStyleable()

    private var mShapeDrawableBuilder: ShapeDrawableBuilder? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShapeImageView)
        mShapeDrawableBuilder = ShapeDrawableBuilder(this, typedArray, STYLEABLE)
        typedArray.recycle()

        mShapeDrawableBuilder?.intoBackground()
    }

    override fun getShapeDrawableBuilder(): ShapeDrawableBuilder? {
        return mShapeDrawableBuilder
    }
}