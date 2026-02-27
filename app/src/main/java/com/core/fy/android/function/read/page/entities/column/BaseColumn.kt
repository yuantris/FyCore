package com.core.fy.android.function.read.page.entities.column

import android.graphics.Canvas
import com.core.fy.android.function.read.page.ContentTextView
import com.core.fy.android.function.read.page.entities.TextLine

/**
 * 列基�?
 */
interface BaseColumn {
    var start: Float
    var end: Float
    var textLine: TextLine

    fun draw(view: ContentTextView, canvas: Canvas)

    fun isTouch(x: Float): Boolean {
        return x > start && x < end
    }

}