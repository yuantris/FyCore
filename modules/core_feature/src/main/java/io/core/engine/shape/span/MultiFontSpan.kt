package io.core.engine.shape.span

import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import android.text.style.ReplacementSpan
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
 * 2025/1/21 11:20
 * @description
 * @author Yuan
 */
class MultiFontSpan(
    textViewAttribute: ITextViewAttribute,
    vararg replacementSpans: ReplacementSpan
) : AlignmentReplacementSpan(textViewAttribute) {

    /** 测量的文本宽度  */
    private var mMeasureTextWidth = 0f

    private val mReplacementSpans: List<ReplacementSpan> = replacementSpans.toList()

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        for (replacementSpan in mReplacementSpans) {
            val size = replacementSpan.getSize(paint, text, start, end, fm)
            mMeasureTextWidth = max(mMeasureTextWidth.toDouble(), size.toDouble()).toFloat()
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
        for (replacementSpan in mReplacementSpans) {
            replacementSpan.draw(
                canvas,
                text,
                start,
                end,
                x,
                top,
                y,
                bottom,
                paint
            )
        }
    }

    override fun updateMeasureState(p: TextPaint) {
        super.updateMeasureState(p)
        for (replacementSpan in mReplacementSpans) {
            replacementSpan.updateMeasureState(p)
        }
    }

    override fun updateDrawState(ds: TextPaint?) {
        super.updateDrawState(ds)
        for (replacementSpan in mReplacementSpans) {
            replacementSpan.updateDrawState(ds)
        }
    }


}