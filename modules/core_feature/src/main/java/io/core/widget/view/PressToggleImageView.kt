package io.core.widget.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes
import io.core.R

/**
 * PressToggleImageView - 按下状态反馈图片视图
 * 
 * 使用 StateListDrawable 实现按下时的图片切换效果
 * 按下时显示 pressedImage，抬手时恢复 defaultImage
 */
class PressToggleImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    // 默认状态图片
    private var defaultImage: Drawable? = null
    
    // 按下状态图片
    private var pressedImage: Drawable? = null

    init {
        // 从XML属性中获取图片资源
        context.withStyledAttributes(attrs, R.styleable.PressToggleImageView) {
            defaultImage = getDrawable(R.styleable.PressToggleImageView_defaultImage)
            pressedImage = getDrawable(R.styleable.PressToggleImageView_pressedImage)
        }

        // 创建状态选择器
        setupStateListDrawable()
    }

    /**
     * 创建并设置 StateListDrawable
     */
    private fun setupStateListDrawable() {
        val defaultImg = defaultImage ?: drawable
        val pressedImg = pressedImage ?: defaultImg
        
        if (defaultImg != null) {
            val stateListDrawable = StateListDrawable().apply {
                // 按下状态 - 必须在默认状态之前添加
                addState(intArrayOf(android.R.attr.state_pressed), pressedImg)
                // 默认状态 - 必须放在最后
                addState(intArrayOf(), defaultImg)
            }
            
            setImageDrawable(stateListDrawable)
        }
    }

    /**
     * 设置默认状态图片
     * @param drawable 默认图片
     */
    fun setDefaultImage(drawable: Drawable?) {
        this.defaultImage = drawable
        setupStateListDrawable()
    }

    /**
     * 设置按下状态图片
     * @param drawable 按下时的图片
     */
    fun setPressedImage(drawable: Drawable?) {
        this.pressedImage = drawable
        setupStateListDrawable()
    }

    /**
     * 同时设置两张图片
     * @param defaultImage 默认图片
     * @param pressedImage 按下时的图片
     */
    fun setStateImages(defaultImage: Drawable?, pressedImage: Drawable?) {
        this.defaultImage = defaultImage
        this.pressedImage = pressedImage
        setupStateListDrawable()
    }

    /**
     * 设置默认状态图片资源ID
     * @param resId 图片资源ID
     */
    fun setDefaultImageResource(resId: Int) {
        setDefaultImage(AppCompatResources.getDrawable(context, resId))
    }

    /**
     * 设置按下状态图片资源ID
     * @param resId 图片资源ID
     */
    fun setPressedImageResource(resId: Int) {
        setPressedImage(AppCompatResources.getDrawable(context, resId))
    }

    /**
     * 同时设置两张图片的资源ID
     * @param defaultResId 默认图片资源ID
     * @param pressedResId 按下时的图片资源ID
     */
    fun setStateImageResources(defaultResId: Int, pressedResId: Int) {
        setStateImages(
            AppCompatResources.getDrawable(context, defaultResId),
            AppCompatResources.getDrawable(context, pressedResId)
        )
    }
}