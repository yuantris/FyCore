package io.core.widget.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import io.core.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.content.withStyledAttributes

class OrbitLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Configuration ratios (relative to view size)
    private val bigCircleSizeRatio = 0.9f      // Big circle diameter as % of view size
    private val orbitWidthRatio = 0.1f        // Big circle stroke width as % of view size
    private val smallCircleSizeRatio = 0.16f   // Small circle diameter as % of view size
    private val orbitRadiusRatio = 0.55f       // Small circle orbit radius as % of big circle radius

    // Colors
    private var bigCircleColor = Color.DKGRAY
    private var smallCircleColor = Color.DKGRAY

    // Animation
    private val animationDuration = 1500L      // ms
    private var animator: ValueAnimator? = null
    private var animatedAngle = 0f
    private val angleRadians get() = animatedAngle * PI.toFloat() / 180f

    // Dynamic dimensions (calculated in onSizeChanged)
    private var centerX = 0f
    private var centerY = 0f
    private var bigCircleRadius = 0f
    private var bigCircleStrokeWidth = 0f
    private var smallCircleRadius = 0f
    private var orbitRadius = 0f

    // Paints (initialized in init block to use dynamic dimensions)
    private lateinit var bigCirclePaint: Paint
    private lateinit var smallCirclePaint: Paint

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.OrbitLoadingView) {
                bigCircleColor = getColor(
                    R.styleable.OrbitLoadingView_bigCircleColor,
                    Color.GRAY
                )
                smallCircleColor = getColor(
                    R.styleable.OrbitLoadingView_smallCircleColor,
                    Color.DKGRAY
                )
            }
        }
        initializePaints()
    }

    private fun initializePaints() {
        bigCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = bigCircleColor
        }

        smallCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = smallCircleColor
        }
    }

    /**
     * 设置大圆颜色
     * @param color 颜色值，如Color.RED或0xFF0000.toInt()
     */
    private fun setBigCircleColor(color: Int) {
        bigCircleColor = color
        bigCirclePaint.color = color
        invalidate()
    }

    /**
     * 设置小圆颜色
     * @param color 颜色值，如Color.BLUE或0x0000FF.toInt()
     */
    private fun setSmallCircleColor(color: Int) {
        smallCircleColor = color
        smallCirclePaint.color = color
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val minDimension = minOf(w, h)

        // Calculate all dimensions based on view size
        bigCircleRadius = minDimension * bigCircleSizeRatio / 2f
        bigCircleStrokeWidth = minDimension * orbitWidthRatio
        smallCircleRadius = minDimension * smallCircleSizeRatio / 2f
        orbitRadius = bigCircleRadius * orbitRadiusRatio

        centerX = w / 2f
        centerY = h / 2f

        // Update paints with new dimensions
        bigCirclePaint.strokeWidth = bigCircleStrokeWidth

    }

    override fun onDraw(canvas: Canvas) {
        // Draw big circle (orbit)
        canvas.drawCircle(centerX, centerY, bigCircleRadius, bigCirclePaint)

        // Calculate and draw small circle position
        val smallCircleX = centerX + orbitRadius * cos(angleRadians)
        val smallCircleY = centerY + orbitRadius * sin(angleRadians)
        canvas.drawCircle(smallCircleX, smallCircleY, smallCircleRadius, smallCirclePaint)
    }

    fun startAnimation() {
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = animationDuration
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    animatedAngle = it.animatedValue as Float
                    invalidate()
                }
            }
        }
        if (!animator!!.isStarted) {
            animator!!.start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
    }

    /**
     * 同时设置大小圆颜色
     * @param color 颜色
     */
    fun setColor(color: Int) {
        setBigCircleColor(color)
        setSmallCircleColor(color)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        animator?.removeAllUpdateListeners()
        super.onDetachedFromWindow()
    }

}