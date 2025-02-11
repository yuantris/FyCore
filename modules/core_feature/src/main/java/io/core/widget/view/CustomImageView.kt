package io.core.widget.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.annotation.Px
import io.core.R

class CustomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var backgroundDrawable: Drawable? = null
    private var activeDrawable: Drawable? = null
    private var currentDrawable: Drawable? = null
    private var isActive = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textItems = mutableListOf<TextItem>()

    init {
        isClickable = true

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomImageView)

            // 设置背景图
            backgroundDrawable = typedArray.getDrawable(R.styleable.CustomImageView_backgroundImage)
            currentDrawable = backgroundDrawable

            // 添加单个文字属性
            val text = typedArray.getString(R.styleable.CustomImageView_text) ?: ""
            val textSize = typedArray.getDimension(
                R.styleable.CustomImageView_textSize,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, resources.displayMetrics)
            )
            val textColor = typedArray.getColor(R.styleable.CustomImageView_textColor, Color.BLACK)
            val marginTop =
                typedArray.getDimensionPixelSize(R.styleable.CustomImageView_layout_marginTop, 0)
            val marginBottom =
                typedArray.getDimensionPixelSize(R.styleable.CustomImageView_layout_marginBottom, 0)
            val marginStart =
                typedArray.getDimensionPixelSize(R.styleable.CustomImageView_layout_marginStart, 0)
            val marginEnd =
                typedArray.getDimensionPixelSize(R.styleable.CustomImageView_layout_marginEnd, 0)

            val topToTop = typedArray.getBoolean(R.styleable.CustomImageView_layout_topToTop, false)
            val bottomToBottom =
                typedArray.getBoolean(R.styleable.CustomImageView_layout_bottomToBottom, false)
            val startToStart =
                typedArray.getBoolean(R.styleable.CustomImageView_layout_startToStart, false)
            val endToEnd = typedArray.getBoolean(R.styleable.CustomImageView_layout_endToEnd, false)

            // 如果设置了 text 属性，则添加到 TextItems 列表
            if (text.isNotEmpty()) {
                addTextItem(
                    TextItem(
                        text = text,
                        textSize = textSize,
                        color = textColor,
                        topToTop = topToTop,
                        bottomToBottom = bottomToBottom,
                        startToStart = startToStart,
                        endToEnd = endToEnd,
                        marginTop = marginTop,
                        marginBottom = marginBottom,
                        marginStart = marginStart,
                        marginEnd = marginEnd
                    )
                )
            }

            typedArray.recycle()
        }
    }

    fun setBackgroundImage(drawable: Drawable) {
        backgroundDrawable = drawable
        currentDrawable = drawable
        invalidate()
    }

    fun setActiveBackground(drawable: Drawable) {
        activeDrawable = drawable
    }

    fun addTextItem(textItem: TextItem) {
        textItems.add(textItem)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 绘制背景图片
        currentDrawable?.let {
            it.setBounds(0, 0, width, height)
            it.draw(canvas)
        }

        // 绘制文字
        for (item in textItems) {
            paint.color = item.color
            paint.textSize = item.textSize
            val x = calculateX(item)
            val y = calculateY(item)
            canvas.drawText(item.text, x, y, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            isActive = !isActive
            currentDrawable =
                if (isActive) activeDrawable ?: backgroundDrawable else backgroundDrawable
            invalidate()
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun calculateX(item: TextItem): Float {
        val width = paint.measureText(item.text)
        return when {
            item.startToStart -> item.marginStart.toFloat()
            item.endToEnd -> width - item.marginEnd.toFloat()
            else -> (width / 2) + item.marginStart
        }
    }

    private fun calculateY(item: TextItem): Float {
        return when {
            item.topToTop -> item.marginTop.toFloat()
            item.bottomToBottom -> height - item.marginBottom.toFloat()
            else -> (height / 2f) - (paint.descent() + paint.ascent()) / 2
        }
    }

    data class TextItem(
        val text: String,
        val textSize: Float = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 16f, Resources.getSystem().displayMetrics
        ),
        val color: Int = Color.BLACK,
        var topToTop: Boolean = false,
        var bottomToBottom: Boolean = false,
        var startToStart: Boolean = false,
        var endToEnd: Boolean = false,
        @Px var marginTop: Int = 0,
        @Px var marginBottom: Int = 0,
        @Px var marginStart: Int = 0,
        @Px var marginEnd: Int = 0
    )
}
