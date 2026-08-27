package io.core.widget.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import io.core.widget.R

class StatefulImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    // 状态图片资源
    private var defaultDrawable: Drawable? = null
    private var selectedDrawable: Drawable? = null

    // 状态变化监听器
    var onStateChanged: ((Boolean) -> Unit)? = null

    // 当前状态
    var isStateChanged: Boolean = false
        private set(value) {
            if (field != value) {
                field = value
                updateDrawableState()
                onStateChanged?.invoke(value)
            }
        }

    init {
        // 确保可点击
        isClickable = true
        isFocusable = true

        // 解析自定义属性
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.StatefulImageView)
            try {
                defaultDrawable = typedArray.getDrawable(R.styleable.StatefulImageView_defaultImage)
                selectedDrawable = typedArray.getDrawable(R.styleable.StatefulImageView_selectedImage)
                isStateChanged = typedArray.getBoolean(R.styleable.StatefulImageView_initialState, false)
            } finally {
                typedArray.recycle()
            }
        }

        // 确保有默认图片
        if (defaultDrawable == null) {
            defaultDrawable = drawable
        }

        updateDrawableState()
    }

    // 设置默认图片资源
    fun setDefaultImageResource(@DrawableRes resId: Int) {
        defaultDrawable = AppCompatResources.getDrawable(context, resId)
        updateDrawableState()
    }

    // 设置选中状态图片资源
    fun setSelectedImageResource(@DrawableRes resId: Int) {
        selectedDrawable = AppCompatResources.getDrawable(context, resId)
        updateDrawableState()
    }

    // 切换状态
    fun toggleState() {
        isStateChanged = !isStateChanged
    }

    // 设置状态
    fun setState(state: Boolean) {
        isStateChanged = state
    }

    // 处理点击事件，自动切换状态
    override fun performClick(): Boolean {
        toggleState()
        return super.performClick()
    }

    // 更新图片显示状态
    private fun updateDrawableState() {
        val currentDrawable = if (isStateChanged && selectedDrawable != null) {
            selectedDrawable
        } else {
            defaultDrawable
        }

        createSelectorDrawable()?.let {
            super.setImageDrawable(it)
        } ?: run {
            super.setImageDrawable(currentDrawable)
        }
    }

    // 创建带状态的Drawable选择器
    private fun createSelectorDrawable(): StateListDrawable? {
        defaultDrawable ?: return null

        return StateListDrawable().apply {
            // 按下状态优先
            addState(intArrayOf(android.R.attr.state_pressed),
                selectedDrawable ?: defaultDrawable!!)

            // 选中状态
            if (isStateChanged && selectedDrawable != null) {
                addState(intArrayOf(), selectedDrawable)
            }

            // 默认状态
            addState(intArrayOf(), defaultDrawable!!)
        }
    }

    // 禁止直接设置图片，强制使用状态控制
    @Deprecated("Use setDefaultImageResource or setSelectedImageResource instead",
        ReplaceWith("setDefaultImageResource(resId)"))
    override fun setImageResource(resId: Int) = setDefaultImageResource(resId)

    @Deprecated("Use setDefaultImageResource or setSelectedImageResource instead",
        ReplaceWith("setDefaultImageResource(resId)"))
    override fun setImageDrawable(drawable: Drawable?) {
        setDefaultImageResource(0)
        defaultDrawable = drawable
        updateDrawableState()
    }
}