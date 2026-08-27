package io.core.widget.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import io.core.widget.R
import io.core.common.util.extensions.cool.dpToPx

class StatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var size: Int
    private var paintColor: Int
    private var statusType: Int // 0=成功 1=失败 2=警示

    // 新增动画相关属性
    private var animateProgress = 1f
    private var animator: ValueAnimator? = null
    private var currentStatus = STATUS_WARNING

    private val paint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    init {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.StatusView, defStyleAttr, 0)
        size =
            typedArray.getDimensionPixelSize(R.styleable.StatusView_core_status_size, 48.dpToPx())
        paintColor = typedArray.getColor(R.styleable.StatusView_core_status_color, Color.BLACK)
        statusType = typedArray.getInt(R.styleable.StatusView_core_status_type, STATUS_WARNING)
        typedArray.recycle()
        paint.strokeWidth = size / 20f
        paint.color = paintColor
    }

    fun setStatus(type: Int) {
        if (statusType != type) {
            currentStatus = type
            startAnimation()
        }
        statusType = type
    }

    // 新增动画控制逻辑
    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                animateProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (statusType) {
            0 -> drawSuccess(canvas, animateProgress)
            1 -> drawFailure(canvas, animateProgress)
            2 -> drawWarning(canvas, animateProgress)
        }
    }

    private fun drawSuccess(canvas: Canvas, progress: Float) {
        val path = Path().apply {
            // 三点坐标定义（起点、转折点、终点）
            val startX = size * 0.2f
            val startY = size * 0.5f
            val midX = size * 0.4f  // 转折点右下方
            val midY = size * 0.65f
            val endX = size * 0.8f
            val endY = size * 0.3f

            // 分阶段绘制（前60%画第一段，后40%画第二段）
            val phase = 0.6f
            if (progress <= phase) {
                // 第一段直线进度
                val ratio = progress / phase
                moveTo(startX, startY)
                lineTo(
                    startX + (midX - startX) * ratio,
                    startY + (midY - startY) * ratio
                )
            } else {
                // 第二段直线进度
                val ratio = (progress - phase) / (1 - phase)
                moveTo(startX, startY)
                lineTo(midX, midY)  // 完整第一段
                lineTo(
                    midX + (endX - midX) * ratio,
                    midY + (endY - midY) * ratio
                )
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawFailure(canvas: Canvas, progress: Float) {
        val center = size / 2f
        val offset = size * 0.3f * progress

        // 绘制第一根线（左上到右下）
        canvas.drawLine(
            center - offset, center - offset,
            center + offset, center + offset,
            paint
        )

        // 绘制第二根线（右上到左下）
        canvas.drawLine(
            center + offset, center - offset,
            center - offset, center + offset,
            paint
        )
    }

    private fun drawWarning(canvas: Canvas, progress: Float) {
        // 旋转动画（0-15度）
        canvas.save()
        canvas.rotate(15f * (1 - progress), size / 2f, size / 2f)

        // 三角形感叹号（缩放动画）
        val scale = 0.8f + 0.2f * progress
        canvas.scale(scale, scale, size / 2f, size * 0.7f)

        val path = Path().apply {
            moveTo(size / 2f, size * 0.1f)
            lineTo(size * 0.9f, size * 0.8f)
            lineTo(size * 0.1f, size * 0.8f)
            close()
        }
        canvas.drawPath(path, paint)

        // 感叹号竖线（渐现动画）
        paint.alpha = (255 * progress).toInt()
        canvas.drawLine(size / 2f, size * 0.5f, size / 2f, size * 0.65f, paint)
        paint.alpha = 255

        canvas.restore()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            startAnimation()
        } else {
            animator?.cancel()
        }
    }

    companion object {
        const val STATUS_SUCCESS = 0
        const val STATUS_FAILURE = 1
        const val STATUS_WARNING = 2
    }
}