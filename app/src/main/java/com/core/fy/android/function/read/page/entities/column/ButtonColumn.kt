package com.core.fy.android.function.read.page.entities.column

import android.graphics.Canvas
import androidx.annotation.Keep
import com.core.fy.android.function.read.page.ContentTextView
import com.core.fy.android.function.read.page.entities.TextLine
import com.core.fy.android.function.read.page.entities.TextLine.Companion.emptyTextLine


/**
 * 按钮�?
 */
@Keep
data class ButtonColumn(
    override var start: Float,
    override var end: Float,
) : BaseColumn {
    override var textLine: TextLine = emptyTextLine
    override fun draw(view: ContentTextView, canvas: Canvas) {

    }
}