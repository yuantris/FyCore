package io.core.engine.shape.drawable

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.annotation.ColorInt

final class ShapeDrawableUtils {

    companion object {

        /**
         * 保存画布的层。根�?Android 版本判断使用不同的保存方法�?
         */
        @JvmStatic
        fun saveCanvasLayer(
            canvas: Canvas,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            paint: Paint?
        ) {
            canvas.saveLayer(left, top, right, bottom, paint)
        }

        /**
         * 计算线性渐变的坐标�?
         */
        @JvmStatic
        fun computeLinearGradientCoordinate(
            layoutDirection: Int,
            r: RectF,
            level: Float,
            orientation: ShapeGradientOrientation
        ): FloatArray {
            val x0: Float
            val x1: Float
            val y0: Float
            val y1: Float

            when (orientation) {
                ShapeGradientOrientation.START_TO_END -> {
                    return computeLinearGradientCoordinate(
                        layoutDirection,
                        r,
                        level,
                        if (layoutDirection == View.LAYOUT_DIRECTION_RTL)
                            ShapeGradientOrientation.RIGHT_TO_LEFT
                        else
                            ShapeGradientOrientation.LEFT_TO_RIGHT
                    )
                }

                ShapeGradientOrientation.END_TO_START -> {
                    return computeLinearGradientCoordinate(
                        layoutDirection,
                        r,
                        level,
                        if (layoutDirection == View.LAYOUT_DIRECTION_RTL)
                            ShapeGradientOrientation.LEFT_TO_RIGHT
                        else
                            ShapeGradientOrientation.RIGHT_TO_LEFT
                    )
                }

                ShapeGradientOrientation.TOP_START_TO_BOTTOM_END -> {
                    return computeLinearGradientCoordinate(
                        layoutDirection,
                        r,
                        level,
                        if (layoutDirection == View.LAYOUT_DIRECTION_RTL)
                            ShapeGradientOrientation.TOP_RIGHT_TO_BOTTOM_LEFT
                        else
                            ShapeGradientOrientation.TOP_LEFT_TO_BOTTOM_RIGHT
                    )
                }

                ShapeGradientOrientation.TOP_END_TO_BOTTOM_START -> {
                    return computeLinearGradientCoordinate(
                        layoutDirection,
                        r,
                        level,
                        if (layoutDirection == View.LAYOUT_DIRECTION_RTL)
                            ShapeGradientOrientation.TOP_LEFT_TO_BOTTOM_RIGHT
                        else
                            ShapeGradientOrientation.TOP_RIGHT_TO_BOTTOM_LEFT
                    )
                }

                ShapeGradientOrientation.BOTTOM_START_TO_TOP_END -> {
                    return computeLinearGradientCoordinate(
                        layoutDirection,
                        r,
                        level,
                        if (layoutDirection == View.LAYOUT_DIRECTION_RTL)
                            ShapeGradientOrientation.BOTTOM_RIGHT_TO_TOP_LEFT
                        else
                            ShapeGradientOrientation.BOTTOM_LEFT_TO_TOP_RIGHT
                    )
                }

                ShapeGradientOrientation.BOTTOM_END_TO_TOP_START -> {
                    return computeLinearGradientCoordinate(
                        layoutDirection,
                        r,
                        level,
                        if (layoutDirection == View.LAYOUT_DIRECTION_RTL)
                            ShapeGradientOrientation.BOTTOM_LEFT_TO_TOP_RIGHT
                        else
                            ShapeGradientOrientation.BOTTOM_RIGHT_TO_TOP_LEFT
                    )
                }

                ShapeGradientOrientation.TOP_TO_BOTTOM -> {
                    x0 = r.left
                    y0 = r.top
                    x1 = x0
                    y1 = level * r.bottom
                }

                ShapeGradientOrientation.TOP_RIGHT_TO_BOTTOM_LEFT -> {
                    x0 = r.right
                    y0 = r.top
                    x1 = level * r.left
                    y1 = level * r.bottom
                }

                ShapeGradientOrientation.RIGHT_TO_LEFT -> {
                    x0 = r.right
                    y0 = r.top
                    x1 = level * r.left
                    y1 = y0
                }

                ShapeGradientOrientation.BOTTOM_RIGHT_TO_TOP_LEFT -> {
                    x0 = r.right
                    y0 = r.bottom
                    x1 = level * r.left
                    y1 = level * r.top
                }

                ShapeGradientOrientation.BOTTOM_TO_TOP -> {
                    x0 = r.left
                    y0 = r.bottom
                    x1 = x0
                    y1 = level * r.top
                }

                ShapeGradientOrientation.BOTTOM_LEFT_TO_TOP_RIGHT -> {
                    x0 = r.left
                    y0 = r.bottom
                    x1 = level * r.right
                    y1 = level * r.top
                }

                ShapeGradientOrientation.LEFT_TO_RIGHT -> {
                    x0 = r.left
                    y0 = r.top
                    x1 = level * r.right
                    y1 = y0
                }

                ShapeGradientOrientation.TOP_LEFT_TO_BOTTOM_RIGHT -> {
                    x0 = r.left
                    y0 = r.top
                    x1 = level * r.right
                    y1 = level * r.bottom
                }
            }
            return floatArrayOf(x0, y0, x1, y1)
        }

        /**
         * 设置颜色的透明度，参�?Support 包中�?ColorUtils.setAlphaComponent 方法
         */
        @JvmStatic
        @ColorInt
        fun setColorAlphaComponent(
            @ColorInt color: Int,
            alpha: Int
        ): Int {
            require(alpha in 0..255) { "alpha must be between 0 and 255." }
            return (color and 0x00FFFFFF.toInt()) or (alpha shl 24)
        }
    }
}
