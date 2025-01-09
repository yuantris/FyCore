package com.core.libraries.widget.layout

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.core.libraries.engine.effect.BaseEffectInterface
import com.core.libraries.engine.effect.ClickEffectType

class EffectFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val clickEffect: ClickEffectType = ClickEffectType.SCALE
) : FrameLayout(context, attrs, defStyleAttr), BaseEffectInterface {

    override val targetView: View
        get() = this // 目标控件是自身

    override val effectType: ClickEffectType
        get() = clickEffect // 获取当前点击效果类型

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!this.hasOnClickListeners() && !this.hasOnLongClickListeners()) return super.onTouchEvent(
            event
        )
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (effectType == ClickEffectType.SCALE) startScaleEffect()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (effectType == ClickEffectType.SCALE) resetScaleEffect()
                performClick() // 确保点击事件被触发
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        // 确保调用父类的实现
        super.performClick()
        return true

    }

}

