package io.core.ui.widget.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import io.core.R
import androidx.core.graphics.toColorInt

class PressEffectImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    // 效果类型枚举
    enum class EffectType {
        OVERLAY, // 原有的覆盖效�?
        SCALE,   // 新增的缩放效�?
        NONE     // 无效�?
    }

    private val overlayPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#40B0B0B0".toColorInt() // 半透明灰色
        style = Paint.Style.FILL
    }

    private val clipPath: Path = Path()
    private var cornerRadius = 16f // 圆角半径
    private var isPressed = false // 是否处于按下状�?

    // 缩放相关属�?
    private var currentScale = 1.0f
    private var scaleAnimator: ValueAnimator? = null
    private var pressedScale = 0.98f // 按下时缩小到98%
    private val animDuration = 150L // 动画持续时间(毫秒)

    // 当前效果类型
    private var effectType = EffectType.OVERLAY

    init {
        // 读取自定义属�?
        val a =
            context.obtainStyledAttributes(attrs, R.styleable.PressEffectImageView, defStyleAttr, 0)
        val defaultPadding =
            a.getDimensionPixelSize(R.styleable.PressEffectImageView_defaultPadding, 10)

        // 读取效果类型（如果在attrs中定义了的话�?
        val effectTypeOrdinal = a.getInt(R.styleable.PressEffectImageView_effectType, 0)
        effectType = EffectType.values()[effectTypeOrdinal]

        a.recycle()

        setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding) // 设置默认内边�?
        updateClipPath() // 初始化时更新路径
    }

    override fun onDraw(canvas: Canvas) {
        if (effectType == EffectType.SCALE && isPressed && isEnabled) {
            // 缩放效果时，通过缩放画布实现（仅在启用状态下�?
            canvas.save()
            canvas.scale(currentScale, currentScale, width / 2f, height / 2f)
        }

        canvas.withClipPath(clipPath) {
            super.onDraw(canvas)
            // 仅当按下并且�?OnClickListener 且效果类型为OVERLAY且启用时显示按压效果
            if (isPressed && hasOnClickListeners() && effectType == EffectType.OVERLAY && isEnabled) {
                drawOverlay(canvas)
            }
        }

        if (effectType == EffectType.SCALE && isPressed && isEnabled) {
            canvas.restore()
        }
    }


    private inline fun Canvas.withClipPath(path: Path, block: Canvas.() -> Unit) {
        clipPath(path) // 直接使用预设路径进行裁剪
        block()
    }

    private fun drawOverlay(canvas: Canvas) {
        canvas.drawRoundRect(
            0f, 0f, width.toFloat(), height.toFloat(), cornerRadius,
            cornerRadius, overlayPaint
        )
    }

    // 更新裁剪路径
    private fun updateClipPath() {
        clipPath.reset()
        clipPath.addRoundRect(
            0f, 0f, width.toFloat(), height.toFloat(),
            cornerRadius, cornerRadius, Path.Direction.CW
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 添加 enabled 状态检�?
        if (!hasOnClickListeners() || effectType == EffectType.NONE || !isEnabled) {
            // 没有设置 OnClickListener、无效果类型或控件被禁用，则直接交由父类处理
            return super.onTouchEvent(event)
        }

        val newPressedState = when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (effectType == EffectType.SCALE) {
                    animateScale(pressedScale)
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (effectType == EffectType.SCALE) {
                    animateScale(1.0f)
                }
                performClick() // 触发点击事件
                false
            }
            else -> isPressed
        }

        // 只有在按压状态变化时才重�?
        if (newPressedState != isPressed) {
            isPressed = newPressedState
            invalidate()
        }

        return true // 自定义触摸逻辑已经处理完成
    }


    // 缩放动画
    private fun animateScale(targetScale: Float) {
        scaleAnimator?.cancel()

        scaleAnimator = ValueAnimator.ofFloat(currentScale, targetScale).apply {
            duration = animDuration
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                currentScale = animator.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    override fun performClick(): Boolean {
        // 确保调用父类的实�?
        super.performClick()
        return true
    }

    // 设置圆角半径
    fun setCornerRadius(radius: Float) {
        if (cornerRadius != radius) {
            cornerRadius = radius
            updateClipPath() // 更新路径
            invalidate() // 重绘
        }
    }

    // 设置效果类型
    fun setEffectType(type: EffectType) {
        if (effectType != type) {
            effectType = type
            invalidate()
        }
    }

    // 获取当前效果类型
    fun getEffectType(): EffectType = effectType

    // 设置按下时的缩放比例
    fun setPressedScale(scale: Float) {
        if (scale in 0.5f..1.0f && pressedScale != scale) {
            pressedScale = scale
        }
    }

    // 支持尺寸变化时更新路�?
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateClipPath() // 尺寸变化时更新裁剪路�?
    }
}