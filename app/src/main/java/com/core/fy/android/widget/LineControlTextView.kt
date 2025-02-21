package com.core.fy.android.widget

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.max

class LineControlTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    data class LineEntry(
        val text: String,
        val spannable: SpannableString?
    )

    @VisibleForTesting
    internal var lineEntries :MutableList<LineEntry>?=null

    private var _maxLines: Int = super.getMaxLines()
        private set(value) {
            field = value.coerceAtLeast(1)
        }

    // 保持对外API不变
    val maxLines: Int
        @JvmName("getMaxLineNum")
        get() = _maxLines

    override fun getMaxLines() = _maxLines

    init {
        lineEntries = mutableListOf()
        super.setMaxLines(Int.MAX_VALUE) // 使用内置的maxLines管理换行
    }

    // region 行操作核心方法
    fun addSingleLine(text: String) = addLineInternal(text, null)

    fun addStyledLine(text: String, style: (SpannableString) -> Unit) {
        val spannable = createSpannable(text, style)
        addLineInternal(text, spannable)
    }

    fun insertLine(position: Int, text: String) = insertLineInternal(position, text, null)

    fun insertStyledLine(position: Int, text: String, style: (SpannableString) -> Unit) {
        val spannable = createSpannable(text, style)
        insertLineInternal(position, text, spannable)
    }

    fun addAllLines(vararg texts: String) {
        texts.forEach { addLineInternal(it, null) }
        updateDisplayText()
    }

    fun clearLines() {
        lineEntries?.clear()
        updateDisplayText()
    }

    fun clearStyleAtLine(position: Int) {
        val lineEntries = lineEntries ?: return
        if (position in lineEntries.indices) {
            val entry = lineEntries[position]
            lineEntries[position] = entry.copy(spannable = null)
            updateDisplayText()
        }
    }

    fun setMaxCustomLines(max: Int) {
        _maxLines = max
        enforceMaxLines()
    }
    // endregion

    // region 重写TextView方法
    override fun setText(text: CharSequence?, type: BufferType?) {
        lineEntries?.clear()
        text?.split("\n")?.forEach {
            addLineInternal(it.toString(), null)
        }
        enforceMaxLines()
        updateDisplayText()
    }

    @Deprecated("Use setMaxCustomLines() instead", ReplaceWith("setMaxCustomLines(maxLines)"))
    override fun setMaxLines(maxLines: Int) = setMaxCustomLines(maxLines)
    // endregion

    // region 内部实现
    private fun createSpannable(text: String, style: (SpannableString) -> Unit): SpannableString {
        return SpannableString(text).apply(style)
    }

    private fun addLineInternal(text: String, spannable: SpannableString?) {
        lineEntries?.add(LineEntry(text, spannable))
        enforceMaxLines()
        updateDisplayText()
    }

    private fun insertLineInternal(position: Int, text: String, spannable: SpannableString?) {
        val lineEntries = lineEntries ?: return
        if (position in 0..lineEntries.size) {
            lineEntries.add(position, LineEntry(text, spannable))
            enforceMaxLines()
            updateDisplayText()
        }
    }

    private fun enforceMaxLines() {
        val lineEntries = lineEntries ?: return
        if (lineEntries.size > maxLines) {  // 使用属性getter
            lineEntries.subList(maxLines, lineEntries.size).clear()
        }
    }

    private fun updateDisplayText() {
        val builder = SpannableStringBuilder()
        lineEntries?.forEachIndexed { index, entry ->
            builder.append(entry.spannable ?: entry.text)
            if (index != lineEntries?.lastIndex) builder.append("\n")
        }
        super.setText(builder, BufferType.SPANNABLE)
    }
    // endregion
}