package com.core.libraries.widget.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MarqueeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        // 启用焦点
        isFocusable = true
        isFocusableInTouchMode = true
        isSingleLine = true
        ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
        marqueeRepeatLimit = -1 // 无限循环
        isSelected = true // 必须设置为选中状态才能实现跑马灯效果
    }

    override fun isFocused(): Boolean {
        // 始终返回true，确保跑马灯效果不会因焦点丢失而停止
        return true
    }
}
