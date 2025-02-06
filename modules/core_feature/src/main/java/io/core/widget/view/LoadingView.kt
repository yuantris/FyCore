package io.core.widget.view

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.collection.SimpleArrayMap
import io.core.R
import io.core.common.util.ext.cool.dpToPx


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
class LoadingView : View {
    private var mSize = 0
    private var mPaintColor = 0
    private var mAnimateValue = 0
    private var mAnimator: ValueAnimator? = null
    private var mPaint: Paint? = null

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : this(
        context,
        attrs,
        0
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoadingView, defStyleAttr, 0)
        mSize = typedArray.getDimensionPixelSize(R.styleable.LoadingView_core_view_size, 32.dpToPx())
        mPaintColor = typedArray.getColor(R.styleable.LoadingView_core_view_color, Color.BLACK)
        typedArray.recycle()
        initPaint()
    }

    constructor(context: Context?, size: Int, color: Int) : super(context) {
        mSize = size
        mPaintColor = color
        initPaint()
    }

    private fun initPaint() {
        mPaint = Paint()
        mPaint!!.color = mPaintColor
        mPaint!!.isAntiAlias = true
        mPaint!!.strokeCap = Paint.Cap.ROUND
    }

    fun setColor(color: Int) {
        mPaintColor = color
        mPaint!!.color = color
        invalidate()
    }

    fun setSize(size: Int) {
        mSize = size
        requestLayout()
    }

    private val mUpdateListener =
        AnimatorUpdateListener { animation ->
            mAnimateValue = animation.animatedValue as Int
            invalidate()
        }

    fun start() {
        if (mAnimator == null) {
            mAnimator = ValueAnimator.ofInt(0, LINE_COUNT - 1)
            mAnimator?.addUpdateListener(mUpdateListener)
            mAnimator?.setDuration(600)
            mAnimator?.repeatMode = ValueAnimator.RESTART
            mAnimator?.repeatCount = ValueAnimator.INFINITE
            mAnimator?.interpolator = LinearInterpolator()
            mAnimator?.start()
        } else if (!mAnimator!!.isStarted) {
            mAnimator!!.start()
        }
    }

    fun stop() {
        if (mAnimator != null) {
            mAnimator!!.removeUpdateListener(mUpdateListener)
            mAnimator!!.removeAllUpdateListeners()
            mAnimator!!.cancel()
            mAnimator = null
        }
    }

    private fun drawLoading(canvas: Canvas, rotateDegrees: Int) {
        val width = mSize / 12
        val height = mSize / 6
        mPaint!!.strokeWidth = width.toFloat()

        canvas.rotate(rotateDegrees.toFloat(), (mSize / 2).toFloat(), (mSize / 2).toFloat())
        canvas.translate((mSize / 2).toFloat(), (mSize / 2).toFloat())

        for (i in 0 until LINE_COUNT) {
            canvas.rotate(DEGREE_PER_LINE.toFloat())
            mPaint!!.alpha = (255f * (i + 1) / LINE_COUNT).toInt()
            canvas.translate(0f, (-mSize / 2 + width / 2).toFloat())
            canvas.drawLine(0f, 0f, 0f, height.toFloat(), mPaint!!)
            canvas.translate(0f, (mSize / 2 - width / 2).toFloat())
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(mSize, mSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val saveCount =
            canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null, Canvas.ALL_SAVE_FLAG)
        drawLoading(canvas, mAnimateValue * DEGREE_PER_LINE)
        canvas.restoreToCount(saveCount)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            start()
        } else {
            stop()
        }
    }


    companion object {
        private const val LINE_COUNT = 12
        private const val DEGREE_PER_LINE = 360 / LINE_COUNT
    }
}