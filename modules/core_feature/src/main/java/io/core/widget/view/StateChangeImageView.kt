package io.core.widget.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import io.core.R
import androidx.core.content.withStyledAttributes

class StateChangeImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    // 默认状态图片
    private var defaultImage: Drawable? = null
    
    // 点击状态图片
    private var pressedImage: Drawable? = null
    
    // 是否处于按下状态
    private var isPressedState = false

    init {
        // 从XML属性中获取图片资源
        context.withStyledAttributes(attrs, R.styleable.StateChangeImageView) {
            defaultImage = getDrawable(R.styleable.StateChangeImageView_defaultImage)
            pressedImage = getDrawable(R.styleable.StateChangeImageView_pressedImage)
        }

        // 设置默认图片
        if (defaultImage != null) {
            setImageDrawable(defaultImage)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 手指按下时切换到点击图片
                isPressedState = true
                updateImage()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 手指抬起或取消时恢复默认图片
                isPressedState = false
                updateImage()
                performClick() // 确保点击事件被触发
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // 因为手动调用了performClick，需要重写此方法以避免警告
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    // 更新显示的图片
    private fun updateImage() {
        if (isPressedState && pressedImage != null) {
            setImageDrawable(pressedImage)
        } else if (defaultImage != null) {
            setImageDrawable(defaultImage)
        }
    }

    // 设置默认图片
    fun setDefaultImage(drawable: Drawable) {
        this.defaultImage = drawable
        if (!isPressedState) {
            setImageDrawable(defaultImage)
        }
    }

    // 设置点击图片
    fun setPressedImage(drawable: Drawable) {
        this.pressedImage = drawable
        if (isPressedState) {
            setImageDrawable(pressedImage)
        }
    }

    // 同时设置两张图片
    fun setStateImages(defaultImage: Drawable, pressedImage: Drawable) {
        setDefaultImage(defaultImage)
        setPressedImage(pressedImage)
    }
}