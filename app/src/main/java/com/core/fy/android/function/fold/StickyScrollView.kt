package com.core.fy.android.function.fold

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView

class StickyScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    private var mStickyView: View? = null
    private var mStickyViewTopOffset = 0
    private var mStickyViewLeftOffset = 0
    private var mCloneView: View? = null
    
    // 标记是否已经吸顶
    private var isSticky = false
    
    // 吸顶View的原始位置
    private var originalPosition = 0

    override fun onFinishInflate() {
        super.onFinishInflate()
        // 查找带有特定tag的View作为吸顶View
        findStickyView(getChildAt(0))
    }

    private fun findStickyView(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child.tag != null && "sticky".equals(child.tag)) {
                    mStickyView = child
                    return
                }
                if (child is ViewGroup) {
                    findStickyView(child)
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (mStickyView != null) {
            // 记录原始位置
            val location = IntArray(2)
            mStickyView!!.getLocationOnScreen(location)
            originalPosition = location[1]
            
            // 创建一个克隆View用于吸顶显示
            if (mCloneView == null) {
                mCloneView = mStickyView!!.inflate(context)
                mCloneView!!.visibility = View.INVISIBLE
                addView(mCloneView!!)
            }
            
            // 设置克隆View的布局参数
            val params = mCloneView!!.layoutParams as MarginLayoutParams
            params.width = mStickyView!!.width
            params.height = mStickyView!!.height
            params.leftMargin = mStickyViewLeftOffset
            params.topMargin = 0
            mCloneView!!.layoutParams = params
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (mStickyView != null) {
            // 计算吸顶View的位置
            val stickyViewTop = mStickyView!!.top - scrollY
            
            // 当吸顶View滚动到顶部时
            if (stickyViewTop <= mStickyViewTopOffset) {
                if (!isSticky) {
                    isSticky = true
                    mCloneView!!.visibility = View.VISIBLE
                }
            } else {
                if (isSticky) {
                    isSticky = false
                    mCloneView!!.visibility = View.INVISIBLE
                }
            }
        }
    }

    // 辅助方法：克隆View
    private fun View.inflate(context: Context): View {
        val constructor = this.javaClass.getConstructor(Context::class.java)
        val newInstance = constructor.newInstance(context)
        if (this is ViewGroup && newInstance is ViewGroup) {
            for (i in 0 until this.childCount) {
                val child = this.getChildAt(i).inflate(context)
                newInstance.addView(child)
            }
        }
        return newInstance
    }
}
