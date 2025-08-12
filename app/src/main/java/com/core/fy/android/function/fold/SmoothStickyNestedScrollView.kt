package com.core.fy.android.function.fold

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.widget.NestedScrollView
import com.core.fy.android.R

class SmoothStickyNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    private var stickyView: View? = null
    private var stickyViewOriginalTop = 0
    private var stickyViewHeight = 0
    private var isSticky = false
    
    // 用于显示吸顶效果的View
    private var floatingView: View? = null
    private var floatingContainer: FrameLayout? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        setupFloatingContainer()
        findStickyView()
    }
    
    private fun setupFloatingContainer() {
        // 创建一个浮动容器用于显示吸顶View
        floatingContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
        }
        
        // 将浮动容器添加到父容器中
        post {
            val parent = parent as? ViewGroup
            parent?.addView(floatingContainer)
        }
    }
    
    private fun findStickyView() {
        val child = getChildAt(0)
        if (child is ViewGroup) {
            findStickyViewRecursive(child)
        }
    }
    
    private fun findStickyViewRecursive(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child.tag == "sticky") {
                stickyView = child
                stickyViewHeight = child.height
                
                // 创建浮动View
                createFloatingView()
                break
            }
            if (child is ViewGroup) {
                findStickyViewRecursive(child)
            }
        }
    }
    
    private fun createFloatingView() {
        stickyView?.let { original ->
            // 克隆原始View的样式和内容
            floatingView = cloneView(original)
            floatingContainer?.addView(floatingView)
        }
    }
    
    private fun cloneView(original: View): View {
        // 这里简化处理，实际项目中可能需要更复杂的克隆逻辑
        val clone = LayoutInflater.from(context).inflate(
            getLayoutResourceId(original), 
            floatingContainer, 
            false
        )
        
        // 复制样式和内容
        copyViewProperties(original, clone)
        return clone
    }
    
    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        
        stickyView?.let { sticky ->
            // 计算吸顶View相对于ScrollView的位置
            val stickyTop = sticky.top - scrollY
            
            when {
                stickyTop <= 0 && !isSticky -> {
                    // 开始吸顶
                    isSticky = true
                    floatingContainer?.visibility = View.VISIBLE
                    sticky.visibility = View.INVISIBLE
                }
                stickyTop > 0 && isSticky -> {
                    // 取消吸顶
                    isSticky = false
                    floatingContainer?.visibility = View.GONE
                    sticky.visibility = View.VISIBLE
                }
            }
        }
    }
    
    // 辅助方法
    private fun getLayoutResourceId(view: View): Int {
        // 根据View类型返回对应的布局资源ID
        // 这里需要根据实际情况实现
        // return R.layout.sticky_item_layout
        return 0
    }
    
    private fun copyViewProperties(source: View, target: View) {
        // 复制View的属性，如文本、背景等
        // 这里需要根据实际的View类型来实现
    }
}
