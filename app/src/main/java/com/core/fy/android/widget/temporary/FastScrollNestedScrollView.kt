package com.core.fy.android.widget.temporary

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import io.core.R

@Suppress("MemberVisibilityCanBePrivate", "unused")
class FastScrollNestedScrollView : NestedScrollView {

    private lateinit var mFastScroller: FastScroller

    constructor(context: Context) : super(context) {
        layout(context, null)
    }

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int = 0
    ) : super(context, attrs, defStyleAttr) {
        layout(context, attrs)
    }

    private fun layout(context: Context, attrs: AttributeSet?) {
        mFastScroller = FastScroller(context, attrs)
        mFastScroller.id = R.id.fast_scroller
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        mFastScroller.visibility = visibility
    }

    fun setFastScrollEnabled(enabled: Boolean) {
        mFastScroller.isEnabled = enabled
    }

    fun setHideScrollbar(hideScrollbar: Boolean) {
        mFastScroller.setFadeScrollbar(hideScrollbar)
    }

    fun setTrackVisible(visible: Boolean) {
        mFastScroller.setTrackVisible(visible)
    }

    fun setTrackColor(color: Int) {
        mFastScroller.setTrackColor(color)
    }

    fun setHandleColor(color: Int) {
        mFastScroller.setHandleColor(color)
    }

    fun setBubbleVisible(visible: Boolean) {
        mFastScroller.setBubbleVisible(visible)
    }

    fun setBubbleColor(color: Int) {
        mFastScroller.setBubbleColor(color)
    }

    fun setBubbleTextColor(color: Int) {
        mFastScroller.setBubbleTextColor(color)
    }

    fun setFastScrollStateChangeListener(fastScrollStateChangeListener: FastScrollStateChangeListener) {
        mFastScroller.setFastScrollStateChangeListener(fastScrollStateChangeListener)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mFastScroller.attachNestedScrollView(this)
        var parent = parent
        while (parent != null) {
            when (parent) {
                is ConstraintLayout, is CoordinatorLayout, is FrameLayout, is RelativeLayout -> break
                else -> parent = (parent as View).parent
            }
        }
        if (parent is ViewGroup && parent.indexOfChild(mFastScroller) == -1) {
            parent.addView(mFastScroller)
            mFastScroller.setLayoutParams(parent)
        }
    }

    override fun onDetachedFromWindow() {
        mFastScroller.detachNestedScrollView()
        super.onDetachedFromWindow()
    }
}