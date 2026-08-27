package io.core.engine.highlight_guide.painter

import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Region
import androidx.core.content.ContextCompat
import io.core.ui.R
import io.core.common.util.extensions.cool.dpToPx

/**
 * Desc: 通用绘制Painter
 * 高亮区红色描边
 * 相邻高亮区吸附显示
 * 拐角处圆角显示
 *
 * Date: 2025/1/22 10:55
 */
class CommonPainter : IPainter {
    companion object {
        //高亮区吸附距离阈值
        private const val CLOSE_DISTANCE = 100
        private val CORNER_SIZE = 12.dpToPx()
        private val STROKE_WIDTH = 2.dpToPx()
        private val BACKGROUND_COLOR = R.color.cardview_shadow_start_color
        private val STROKE_COLOR = R.color.md_red_500
    }

    override fun getBackgroundColor() = BACKGROUND_COLOR
    override fun onDraw(context: Context, absRectList: List<Rect>, canvas: Canvas, paint: Paint) {
        //canvas.drawRect(rect, paint)
        unionRect(absRectList)
        val path = getUnionPath(absRectList)

        val pathEffect = CornerPathEffect(CORNER_SIZE.toFloat())
        paint.pathEffect = pathEffect
        canvas.drawPath(path, paint)

        val strokePaint = Paint()
        strokePaint.color = ContextCompat.getColor(context, STROKE_COLOR)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.isAntiAlias = true
        strokePaint.strokeWidth = STROKE_WIDTH.toFloat()
        strokePaint.pathEffect = pathEffect
        canvas.drawPath(path, strokePaint)
    }

    private fun unionRect(absRectList: List<Rect>) {
        for (i in absRectList.indices) {
            for (j in absRectList.indices) {
                val rectI = absRectList[i]
                val rectJ = absRectList[j]
                val topDiff = rectI.top - rectJ.bottom
                val leftDiff = rectI.left - rectJ.right
                val bottomDiff = rectJ.top - rectI.bottom
                val rightDiff = rectJ.left - rectI.right

                when {
                    topDiff in 1..CLOSE_DISTANCE && rectI.left >= rectJ.left && rectI.right <= rectJ.right -> {
                        // 上边靠近且包含
                        rectI.top = rectJ.top
                    }

                    leftDiff in 1..CLOSE_DISTANCE && rectI.top >= rectJ.top && rectI.bottom <= rectJ.bottom -> {
                        // 左边靠近且包含
                        rectI.left = rectJ.left
                    }

                    bottomDiff in 1..CLOSE_DISTANCE && rectI.left >= rectJ.left && rectI.right <= rectJ.right -> {
                        // 下边靠近且包含
                        rectI.bottom = rectJ.bottom
                    }

                    rightDiff in 1..CLOSE_DISTANCE && rectI.top >= rectJ.top && rectI.bottom <= rectJ.bottom -> {
                        // 右边靠近且包含
                        rectI.right = rectJ.right
                    }

                    else -> continue
                }
            }
        }
    }


    private fun getUnionPath(absRectList: List<Rect>): Path {
        if (absRectList.isEmpty()) return Path()

        val region = Region()
        for (rect in absRectList) {
            region.union(rect)
        }

        val path = Path()
        region.getBoundaryPath(path)
        return path
    }
}
