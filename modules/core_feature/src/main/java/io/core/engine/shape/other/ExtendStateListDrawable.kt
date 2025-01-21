package io.core.engine.shape.other

import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable


/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/21 10:52
 * @description
 * @author Yuan
 */
class ExtendStateListDrawable : StateListDrawable() {
    companion object {
        private val STATE_DEFAULT: IntArray = intArrayOf()
        private val STATE_PRESSED: IntArray = intArrayOf(android.R.attr.state_pressed)
        private val STATE_CHECKED: IntArray = intArrayOf(android.R.attr.state_checked)
        private val STATE_DISABLED: IntArray = intArrayOf(-android.R.attr.state_enabled)
        private val STATE_FOCUSED: IntArray = intArrayOf(android.R.attr.state_focused)
        private val STATE_SELECTED: IntArray = intArrayOf(android.R.attr.state_selected)
    }

    private val mDrawableMap = HashMap<IntArray, Drawable>()

    override fun addState(stateSet: IntArray, drawable: Drawable?) {
        super.addState(stateSet, drawable)
        if (drawable == null) {
            return
        }
        mDrawableMap[stateSet] = drawable
    }

    fun setDefaultDrawable(drawable: Drawable?) {
        addState(STATE_DEFAULT, drawable)
    }

    fun getDefaultDrawable(): Drawable? {
        return mDrawableMap[STATE_DEFAULT]
    }

    fun setPressedDrawable(drawable: Drawable?) {
        addState(STATE_PRESSED, drawable)
    }

    fun getPressedDrawable(): Drawable? {
        return mDrawableMap[STATE_PRESSED]
    }

    fun setCheckDrawable(drawable: Drawable?) {
        addState(STATE_CHECKED, drawable)
    }

    fun getCheckDrawable(): Drawable? {
        return mDrawableMap[STATE_CHECKED]
    }

    fun setDisabledDrawable(drawable: Drawable?) {
        addState(STATE_DISABLED, drawable)
    }

    fun getDisabledDrawable(): Drawable? {
        return mDrawableMap[STATE_DISABLED]
    }

    fun setFocusedDrawable(drawable: Drawable?) {
        addState(STATE_FOCUSED, drawable)
    }

    fun getFocusedDrawable(): Drawable? {
        return mDrawableMap[STATE_FOCUSED]
    }

    fun setSelectDrawable(drawable: Drawable?) {
        addState(STATE_SELECTED, drawable)
    }

    fun getSelectDrawable(): Drawable? {
        return mDrawableMap[STATE_SELECTED]
    }
}