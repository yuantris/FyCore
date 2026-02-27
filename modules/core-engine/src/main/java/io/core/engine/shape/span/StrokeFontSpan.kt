package io.core.engine.shape.span

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.widget.TextView
import androidx.annotation.ColorInt
import io.core.engine.shape.config.ITextViewAttribute
import io.core.engine.shape.other.TextViewAttribute


/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/1/21 11:27
 * @description
 * @author Yuan
 */
class StrokeFontSpan(textViewAttribute: ITextViewAttribute) : CommonFontSpan(textViewAttribute) {
    companion object {
        /**
         * 构建一个文字描边的 Spannable 对象
         */
        fun buildStrokeFontSpannable(
            textView: TextView,
            text: CharSequence?,
            textStrokeColor: Int,
            textStrokeSize: Int
        ): SpannableStringBuilder {
            return buildStrokeFontSpannable(
                TextViewAttribute(textView),
                text,
                textStrokeColor,
                textStrokeSize
            )
        }

        fun buildStrokeFontSpannable(
            textViewAttribute: ITextViewAttribute?,
            text: CharSequence?,
            textStrokeColor: Int,
            textStrokeSize: Int
        ): SpannableStringBuilder {
            val builder = SpannableStringBuilder(text)
            val span: StrokeFontSpan = StrokeFontSpan(
                textViewAttribute!!
            )
                .setTextStrokeColor(textStrokeColor)
                .setTextStrokeSize(textStrokeSize)
            builder.setSpan(span, 0, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return builder
        }

    }

    /** 描边画笔  */
    private val mStrokePaint = Paint()

    private var mTextStrokeColor = 0
    private var mTextStrokeSize = 0

    /** 文本颜色  */
    private var mTextSolidColor = 0

    override fun onDraw(
        canvas: Canvas,
        paint: Paint,
        text: CharSequence?,
        textWidth: Float,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int
    ) {
        mStrokePaint.set(paint);
        // 设置抗锯�?
        mStrokePaint.isAntiAlias = true;
        // 设置防抖�?
        mStrokePaint.isDither = true;
        mStrokePaint.textSize = paint.textSize;
        // 描边宽度
        mStrokePaint.strokeWidth = mTextStrokeSize.toFloat();
        mStrokePaint.style = Paint.Style.STROKE;
        // 设置粗体
        //mStrokePaint.setFakeBoldText(true);
        mStrokePaint.color = mTextStrokeColor;
        // 绘制文本描边
        canvas.drawText(text.toString(), start, end, x, y.toFloat(), mStrokePaint);

        // 绘制文本内容
        if (mTextSolidColor != Color.TRANSPARENT) {
            paint.color = mTextSolidColor;
            canvas.drawText(text.toString(), start, end, x, y.toFloat(), paint);
        }
    }

    fun setTextSolidColor(@ColorInt color: Int): StrokeFontSpan {
        mTextSolidColor = color
        return this
    }

    fun setTextStrokeColor(@ColorInt color: Int): StrokeFontSpan {
        mTextStrokeColor = color
        return this
    }

    fun setTextStrokeSize(size: Int): StrokeFontSpan {
        mTextStrokeSize = size
        return this
    }
}