package io.core.engine.shape.span

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.widget.LinearLayout
import android.widget.TextView
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
 * 2025/1/21 11:09
 * @description
 * @author Yuan
 */
class LinearGradientFontSpan(textViewAttribute: ITextViewAttribute) :
    CommonFontSpan(textViewAttribute) {

    companion object {
        /** 水平渐变方向  */
        const val GRADIENT_ORIENTATION_HORIZONTAL: Int = LinearLayout.HORIZONTAL

        /** 垂直渐变方向  */
        const val GRADIENT_ORIENTATION_VERTICAL: Int = LinearLayout.VERTICAL

        /**
         * 构建一个文字渐变色�?Spannable 对象
         */
        fun buildLinearGradientFontSpannable(
            textView: TextView,
            text: CharSequence?,
            colors: IntArray?,
            positions: FloatArray?,
            orientation: Int
        ): SpannableStringBuilder {
            return buildLinearGradientFontSpannable(
                TextViewAttribute(textView),
                text,
                colors,
                positions,
                orientation
            )
        }

        fun buildLinearGradientFontSpannable(
            textViewAttribute: ITextViewAttribute?,
            text: CharSequence?,
            colors: IntArray?,
            positions: FloatArray?,
            orientation: Int
        ): SpannableStringBuilder {
            val builder = SpannableStringBuilder(text)
            val span: LinearGradientFontSpan = LinearGradientFontSpan(
                textViewAttribute!!
            )
                .setTextGradientColor(colors)
                .setTextGradientOrientation(orientation)
                .setTextGradientPositions(positions)
            builder.setSpan(span, 0, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return builder
        }
    }


    /** 文字渐变方向  */
    private var mTextGradientOrientation = 0

    /** 文字渐变颜色�? */
    private var mTextGradientColor: IntArray? = null

    /** 文字渐变位置�? */
    private var mTextGradientPositions: FloatArray? = null

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
        val linearGradient: LinearGradient =
            if (mTextGradientOrientation == GRADIENT_ORIENTATION_VERTICAL) {
                LinearGradient(
                    0f, 0f, 0f, paint.descent() - paint.ascent(),
                    mTextGradientColor!!, mTextGradientPositions, Shader.TileMode.REPEAT
                )
            } else {
                LinearGradient(
                    x, 0f, x + textWidth, 0f,
                    mTextGradientColor!!, mTextGradientPositions, Shader.TileMode.REPEAT
                )
            }
        paint.setShader(linearGradient)
        canvas.drawText(text.toString(), start, end, x, y.toFloat(), paint)
    }

    fun setTextGradientOrientation(orientation: Int): LinearGradientFontSpan {
        mTextGradientOrientation = orientation
        return this
    }

    fun setTextGradientColor(colors: IntArray?): LinearGradientFontSpan {
        mTextGradientColor = colors
        return this
    }

    fun setTextGradientPositions(positions: FloatArray?): LinearGradientFontSpan {
        mTextGradientPositions = positions
        return this
    }
}