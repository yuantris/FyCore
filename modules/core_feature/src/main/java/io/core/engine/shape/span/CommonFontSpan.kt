package io.core.engine.shape.span

import android.R
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.view.Gravity
import android.view.View
import io.core.engine.shape.config.ITextViewAttribute
import kotlin.math.max


/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/21 11:02
 * @description
 * @author Yuan
 */
abstract class CommonFontSpan(textViewAttribute: ITextViewAttribute) :
    AlignmentReplacementSpan(textViewAttribute) {

    /** 测量的文本宽度  */
    private var mMeasureTextWidth = 0f

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fontMetricsInt: FontMetricsInt?
    ): Int {
        mMeasureTextWidth = onMeasure(paint, fontMetricsInt, text, start, end)

        // 这段不可以去掉，字体高度没设置，会出现 draw 方法没有被调用的问题
        // 详情请见：https://stackoverflow.com/questions/20069537/replacementspans-draw-method-isnt-called
        val metrics: FontMetricsInt = paint.getFontMetricsInt()
        if (fontMetricsInt != null) {
            fontMetricsInt.top = metrics.top
            fontMetricsInt.ascent = metrics.ascent
            fontMetricsInt.descent = metrics.descent
            fontMetricsInt.bottom = metrics.bottom
        }
        return mMeasureTextWidth.toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val alpha = paint.alpha

        // 判断是否给画笔设置了透明度
        if (alpha != 255) {
            // 如果是则设置不透明
            paint.alpha = 255
        }

        // 获取文本和画布宽度
        val textWidth: Float = paint.measureText(text, start, end)

        // 根据对齐方式调整 x 坐标，默认左对齐，不需要额外处理，直接使用 x 作为起点
        var drawX = R.attr.x.toFloat()

        val textAttribute: ITextViewAttribute = mTextAttribute
        val canvasWidth =
            (canvas.width - textAttribute.getPaddingLeft() - textAttribute.getPaddingRight()).toFloat()

        // 获取 TextView 文本重心
        val gravity = textAttribute.getTextGravity()

        // 获取当前布局方向（LTR 或 RTL）
        val isRtl = (textAttribute.getLayoutDirection() === View.LAYOUT_DIRECTION_RTL)


        // 判断的顺序必须为：left 和 right，start 和 end，center 和 center_horizontal
        if (hasFlag(gravity, Gravity.LEFT)) {
            // 左对齐
            drawX = R.attr.x.toFloat()
        } else if (hasFlag(gravity, Gravity.RIGHT)) {
            // 右对齐
            drawX = canvasWidth - textWidth
        } else if ((isRtl && hasFlag(gravity, Gravity.END)) || (!isRtl && hasFlag(
                gravity,
                Gravity.START
            ))
        ) {
            // 左对齐或 START 对齐（适配布局方向）
            drawX = R.attr.x.toFloat()
        } else if ((isRtl && hasFlag(gravity, Gravity.START)) || (!isRtl && hasFlag(
                gravity,
                Gravity.END
            ))
        ) {
            // 右对齐或 END 对齐（适配布局方向）
            drawX = max((canvasWidth - textWidth).toDouble(), 0.0).toFloat()
        } else if (hasFlag(gravity, Gravity.CENTER) || hasFlag(
                gravity,
                Gravity.CENTER_HORIZONTAL
            )
        ) {
            // 居中对齐
            drawX = max(((canvasWidth - textWidth) / 2).toDouble(), 0.0).toFloat()
        }


        // 绘制文本
        onDraw(
            canvas,
            paint,
            text,
            textWidth,
            start,
            end,
            drawX,
            top,
            y,
            bottom
        )


        // 绘制完成之后将画笔的透明度还原回去
        paint.alpha = alpha
    }

    fun onMeasure(
        paint: Paint,
        fontMetricsInt: FontMetricsInt?,
        text: CharSequence?,
        @androidx.annotation.IntRange(from = 0) start: Int,
        @androidx.annotation.IntRange(from = 0) end: Int
    ): Float {
        return paint.measureText(text, start, end)
    }

    abstract fun onDraw(
        canvas: Canvas,
        paint: Paint,
        text: CharSequence?,
        textWidth: Float,
        @androidx.annotation.IntRange(from = 0) start: Int,
        @androidx.annotation.IntRange(from = 0) end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int
    )

}