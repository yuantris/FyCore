package com.core.libraries.widget.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.core.libraries.R

class PressEffectImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val overlayPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40B0B0B0") // 半透明灰色
        style = Paint.Style.FILL
    }

    private val clipPath: Path = Path()
    private var cornerRadius = 16f // 圆角半径
    private var isPressed = false // 是否处于按下状态

    init {
        // 读取自定义属性
        val a = context.obtainStyledAttributes(attrs, R.styleable.PressEffectImageView, defStyleAttr, 0)
        val defaultPadding = a.getDimensionPixelSize(R.styleable.PressEffectImageView_defaultPadding, 10)
        a.recycle()

        setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding) // 设置默认内边距
        updateClipPath() // 初始化时更新路径
    }

    override fun onDraw(canvas: Canvas) {
        canvas.withClipPath(clipPath) {
            super.onDraw(canvas)
            // 仅当按下并且有 OnClickListener 时显示按压效果
            if (isPressed && hasOnClickListeners()) {
                drawOverlay(canvas)
            }
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
        if (!hasOnClickListeners()) {
            // 没有设置 OnClickListener，则直接交由父类处理
            return super.onTouchEvent(event)
        }

        val newPressedState = when (event.action) {
            MotionEvent.ACTION_DOWN -> true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                performClick() // 触发点击事件
                false
            }

            else -> isPressed
        }

        // 只有在按压状态变化时才重绘
        if (newPressedState != isPressed) {
            isPressed = newPressedState
            invalidate()
        }

        return true // 自定义触摸逻辑已经处理完成
    }


    // 设置圆角半径
    fun setCornerRadius(radius: Float) {
        if (cornerRadius != radius) {
            cornerRadius = radius
            updateClipPath() // 更新路径
            invalidate() // 重绘
        }
    }

    // 支持尺寸变化时更新路径
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateClipPath() // 尺寸变化时更新裁剪路径
    }
}
