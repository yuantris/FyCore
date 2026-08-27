package io.core.common.base.component.activity

import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.viewbinding.ViewBinding
import io.core.common.util.extensions.ui.inflateBindingWithGeneric
import io.core.engine.swipeback.SwipeBackHelper

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/6 15:00
 * @description
 * @author Yuan
 */
abstract class BaseSwipeActivity<VB : ViewBinding> : BaseActivity() {

    lateinit var binding: VB
    private var swipeBackHelper: SwipeBackHelper? = null

    /**
     * 关闭侧滑
     */
    var swipeEnable = true
        set(value) {
            field = value
            swipeBackHelper?.setEnable(field)
        }

    override fun contentViewBind(): View? {
        binding = inflateBindingWithGeneric(layoutInflater)
        return binding.root
    }

    override fun initial(savedInstanceState: Bundle?) {
        swipeBackHelper = SwipeBackHelper(this)
        swipeBackHelper?.setBackgroundColor(Color.WHITE)
        super.initial(savedInstanceState)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        swipeBackHelper?.dispatchTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        swipeBackHelper?.onTouchEvent(event)
        return super.onTouchEvent(event)
    }
}