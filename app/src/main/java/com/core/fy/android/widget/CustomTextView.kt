package com.core.fy.android.widget

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView

class CustomTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val textBuilder = SpannableStringBuilder()

    fun addSingleLine(text: String, styleConfig: SpanConfig.() -> Unit = {}) {
        // 添加换行符（非首行）
        if (textBuilder.isNotEmpty()) {
            textBuilder.append("\n")
        }

        // 创建带样式的文本
        val spannable = SpannableString(text).apply {
            SpanConfig().apply(styleConfig).spans.forEach { span ->
                setSpan(span, 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        textBuilder.append(spannable)
        updateDisplay()
    }

    fun clear() {
        textBuilder.clear()
        updateDisplay()
    }

    private fun updateDisplay() {
        text = textBuilder
    }

    class SpanConfig {
        val spans = mutableListOf<Any>()

        fun bold() = spans.add(StyleSpan(Typeface.BOLD))
        fun italic() = spans.add(StyleSpan(Typeface.ITALIC))
        fun color(@ColorInt color: Int) = spans.add(ForegroundColorSpan(color))
        fun backgroundColor(@ColorInt color: Int) = spans.add(BackgroundColorSpan(color))
    }
}