package io.core.common.util.tools

import android.graphics.Color
import android.graphics.Rect
import androidx.annotation.ColorInt
import com.hjq.shape.drawable.ShapeDrawable
import io.core.common.util.ext.cool.dpToPx

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/17 14:19
 * @description
 * @author Yuan
 */
object DrawableBuilder {
    private var color: Int = Color.TRANSPARENT
    private var radius: Float = 0f
    private var topLeftRadius: Float = 0f
    private var topRightRadius: Float = 0f
    private var bottomLeftRadius: Float = 0f
    private var bottomRightRadius: Float = 0f
    private var padding: Rect = Rect()
    private var paddingLeft: Int = 0
    private var paddingRight: Int = 0
    private var paddingTop: Int = 0
    private var paddingBottom: Int = 0

    fun setSolidColor(@ColorInt colorInt: Int): DrawableBuilder {
        this.color = colorInt
        return this
    }

    fun setRadius(radius: Float): DrawableBuilder {
        this.radius = radius.dpToPx()
        return this
    }

    fun setTopLeftRadius(radius: Float): DrawableBuilder {
        this.topLeftRadius = radius.dpToPx()
        return this
    }

    fun setTopRightRadius(radius: Float): DrawableBuilder {
        this.topRightRadius = radius.dpToPx()
        return this
    }

    fun setBottomLeftRadius(radius: Float): DrawableBuilder {
        this.bottomLeftRadius = radius.dpToPx()
        return this
    }

    fun setBottomRightRadius(radius: Float): DrawableBuilder {
        this.bottomRightRadius = radius.dpToPx()
        return this
    }

    fun setPadding(rect: Rect): DrawableBuilder {
        this.padding = rect
        return this
    }

    fun setPaddingLeft(padding: Int): DrawableBuilder {
        this.paddingLeft = padding.dpToPx()
        return this
    }

    fun setPaddingRight(padding: Int): DrawableBuilder {
        this.paddingRight = padding.dpToPx()
        return this
    }

    fun setPaddingTop(padding: Int): DrawableBuilder {
        this.paddingTop = padding.dpToPx()
        return this
    }

    fun setPaddingBottom(padding: Int): DrawableBuilder {
        this.paddingBottom = padding.dpToPx()
        return this
    }


    fun build(): ShapeDrawable {
        val drawable = ShapeDrawable()
        drawable.setSolidColor(color)
        if (radius > 0) {
            drawable.setRadius(radius)
        } else {
            drawable.setRadius(topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius)
        }
        if (isValidRectWithArea(padding)) {
            drawable.setPadding(padding)
        }else{
            drawable.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        }

        return drawable
    }
}

fun isValidRectWithArea(rect: Rect): Boolean {
    // 检查矩形的宽度和高度是否大于零
    return rect.width() > 0 && rect.height() > 0
}