package io.core.engine.highlight_guide.painter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.ColorRes

/**
 * Desc: 高亮区域绘制接口
 *
 * Date: 2025/1/22 10:50
 */
interface IPainter {
    @ColorRes
    fun getBackgroundColor(): Int
    fun onDraw(context: Context, absRectList: List<Rect>, canvas: Canvas, paint: Paint)
}
