package com.core.fy.android.function

import android.view.MotionEvent
import com.core.fy.android.databinding.ActivityTestPageBinding
import com.core.fy.android.function.read.page.ContentTextView
import com.core.fy.android.function.read.page.delegate.PageDelegate
import com.core.fy.android.function.read.page.provider.TextPageFactory
import com.core.libraries.common.base.activity.ReflectBindingActivity

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/13 16:43
 * @description
 * @author Yuan
 */
class TestPageActivity: ReflectBindingActivity<ActivityTestPageBinding>(),ContentTextView.CallBack {
    override val headerHeight: Int
        get() = 10
    override val pageFactory: TextPageFactory
        get() = TODO("Not yet implemented")
    override val pageDelegate: PageDelegate?
        get() = TODO("Not yet implemented")
    override val isScroll: Boolean
        get() = TODO("Not yet implemented")
    override var isSelectingSearchResult: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun upSelectedStart(x: Float, y: Float, top: Float) {
        TODO("Not yet implemented")
    }

    override fun upSelectedEnd(x: Float, y: Float) {
        TODO("Not yet implemented")
    }

    override fun onImageLongPress(x: Float, y: Float, src: String) {
        TODO("Not yet implemented")
    }

    override fun onCancelSelect() {
        TODO("Not yet implemented")
    }

    override fun onLongScreenshotTouchEvent(event: MotionEvent): Boolean {
        TODO("Not yet implemented")
    }
}