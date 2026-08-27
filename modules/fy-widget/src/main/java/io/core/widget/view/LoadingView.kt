package io.core.widget.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import io.core.widget.R
import io.core.common.util.extensions.cool.dpToPx


/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/6 13:38
 * @description
 * @author Yuan
 */
class LoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var size: Int
    private var paintColor: Int
    private var animateValue = 0
    private var animator: ValueAnimator? = null

    // 这里直接使用非空 Paint，并通过 apply 初始化属性
    private val paint: Paint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    init {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.LoadingView, defStyleAttr, 0)
        size = typedArray.getDimensionPixelSize(R.styleable.LoadingView_core_view_size, 32.dpToPx())
        paintColor = typedArray.getColor(R.styleable.LoadingView_core_view_color, Color.BLACK)
        typedArray.recycle()
        paint.color = paintColor
    }

    // 提供一个辅助构造函数
    constructor(context: Context, size: Int, color: Int) : this(context) {
        this.size = size
        this.paintColor = color
        paint.color = color
    }

    fun setColor(color: Int) {
        paintColor = color
        paint.color = color
        invalidate()
    }

    fun setSize(newSize: Int) {
        size = newSize
        requestLayout()
    }

    // 使用 lambda 简化更新监听器的写法
    private val updateListener = ValueAnimator.AnimatorUpdateListener { animation ->
        animateValue = animation.animatedValue as Int
        invalidate()
    }

    private fun startAnimation() {
        if (animator == null) {
            animator = ValueAnimator.ofInt(0, LINE_COUNT - 1).apply {
                addUpdateListener(updateListener)
                duration = 600
                repeatMode = ValueAnimator.RESTART
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        } else if (animator?.isStarted == false) {
            animator?.start()
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    private fun drawLoading(canvas: Canvas, rotateDegrees: Int) {
        val strokeWidth = size / 12f
        val lineHeight = size / 6f
        paint.strokeWidth = strokeWidth

        // 为了避免 canvas 累加平移和旋转，使用 save/restore 分组操作
        canvas.save()
        canvas.rotate(rotateDegrees.toFloat(), size / 2f, size / 2f)
        canvas.translate(size / 2f, size / 2f)
        for (i in 0 until LINE_COUNT) {
            canvas.rotate(DEGREE_PER_LINE.toFloat())
            paint.alpha = (255f * (i + 1) / LINE_COUNT).toInt()
            canvas.save()
            // 将每条线单独绘制后恢复画布状态
            canvas.translate(0f, -size / 2f + strokeWidth / 2f)
            canvas.drawLine(0f, 0f, 0f, lineHeight, paint)
            canvas.restore()
        }
        canvas.restore()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawLoading(canvas, animateValue * DEGREE_PER_LINE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    companion object {
        private const val LINE_COUNT = 12
        private const val DEGREE_PER_LINE = 360 / LINE_COUNT
    }
}
