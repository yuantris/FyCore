package com.core.libraries.widget.view

import android.content.Context
import android.graphics.Rect
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.method.Touch
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.core.libraries.base.ext.logD
import java.util.regex.Pattern

class ClickableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var onLetterClickListener: ((CharSequence, Int) -> Unit)? = null
    private var clickedSpans :MutableList<BackgroundColorSpan>? = null

    init {
        clickedSpans = mutableListOf() // 确保初始化
        movementMethod = EnlargedLinkMovementMethod()
        highlightColor = ContextCompat.getColor(context, android.R.color.transparent)
    }

    /**
     * 格式化文本，在标点符号后加入换行符，并去掉其他位置的换行符
     */
    private fun formatTextWithNewLines(text: String?): String {
        // 去除所有换行符
        val noNewLines = text?.replace("\\n".toRegex(), "")

        // 在标点符号后加入换行符
        val punctuationPattern = Pattern.compile("[.,!?;:。！？，；：]")
        val matcher = punctuationPattern.matcher(noNewLines.toString())
        val formattedText = matcher.replaceAll("$0\n")

        return formattedText
    }

    fun setOnLetterClickListener(listener: (CharSequence, Int) -> Unit) {
        this.onLetterClickListener = listener
        updateTextSpans()
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(formatTextWithNewLines(text.toString()), type)
        updateTextSpans()
    }

    private fun updateTextSpans() {
        val textContent = text ?: return
        val spannableBuilder = SpannableStringBuilder(textContent)
        clickedSpans?.clear() // 清除之前的点击背景样式

        textContent.forEachIndexed { index, char ->
            if (char.isLetterOrDigit()) { // 仅为字母和数字设置点击事件
                spannableBuilder.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        removeHighlight() // 移除之前高亮
                        val backgroundColorSpan = BackgroundColorSpan(
                            ContextCompat.getColor(context, android.R.color.transparent)
                        )
                        clickedSpans?.add(backgroundColorSpan)
                        spannableBuilder.setSpan(
                            backgroundColorSpan,
                            index,
                            index + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        text = spannableBuilder // 更新背景色
                        onLetterClickListener?.invoke(char.toString(), index)
                    }

                    override fun updateDrawState(ds: android.text.TextPaint) {
                        // 移除默认下划线和文字颜色更改
                        ds.isUnderlineText = false
                    }
                }, index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        super.setText(spannableBuilder, BufferType.SPANNABLE)
    }

    /**
     * 移除所有点击背景色
     */
    fun removeHighlight() {
        "开始remove".logD()
        val textContent = text as? SpannableStringBuilder ?: SpannableStringBuilder(text)
        "remove中".logD()

        // 获取所有类型的Span并移除
        val spans = textContent.getSpans(0, textContent.length, Any::class.java)
        spans.forEach { span ->
            textContent.removeSpan(span)
        }

        clickedSpans?.clear()
        text = textContent // 更新文字
        "结束remove".logD()
    }


    private class EnlargedLinkMovementMethod : LinkMovementMethod() {
        private var lastClickableSpan: ClickableSpan? = null

        override fun onTouchEvent(
            widget: TextView?,
            buffer: Spannable?,
            event: MotionEvent?
        ): Boolean {
            val action = event?.action
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                val x = event.x.toInt()
                val y = event.y.toInt()

                widget?.let {
                    val widgetRect = Rect()
                    widget.getGlobalVisibleRect(widgetRect)
                    val widgetX = x - widget.paddingLeft + widget.scrollX
                    val widgetY = y - widget.paddingTop + widget.scrollY

                    val layout = widget.layout
                    val line = layout.getLineForVertical(widgetY)
                    val offset = layout.getOffsetForHorizontal(line, widgetX.toFloat())

                    val clickableSpans = buffer?.getSpans(
                        offset, offset, ClickableSpan::class.java
                    )
                    if (clickableSpans?.isNotEmpty() == true) {
                        val clickableSpan = clickableSpans[0]
                        if (action == MotionEvent.ACTION_UP) {
                            clickableSpan.onClick(widget)
                        }
                        lastClickableSpan = clickableSpan
                        return true
                    } else if (lastClickableSpan != null) {
                        lastClickableSpan = null
                        return false
                    }
                }


            }
            return Touch.onTouchEvent(widget, buffer, event)
        }

    }
}
