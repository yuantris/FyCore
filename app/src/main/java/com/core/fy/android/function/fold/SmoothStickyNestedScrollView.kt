package com.core.fy.android.function.fold

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import com.core.fy.android.R

class SmoothStickyNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    // 吸顶View相关
    private var stickyView: View? = null
    private var stickyViewOriginalTop = 0
    private var stickyViewHeight = 0
    private var isSticky = false

    // 吸顶View的两个状态View
    private var expandedView: View? = null
    private var collapsedView: View? = null

    // 内容View
    private var contentView: View? = null

    // 滚动相关
    private var lastScrollY = 0

    override fun onFinishInflate() {
        super.onFinishInflate()

        // 查找吸顶View和内容View
        post {
            findViews()
        }
    }

    private fun findViews() {
        val childCount = childCount
        if (childCount > 0) {
            val mainChild = getChildAt(0) as? ViewGroup
            mainChild?.let { container ->
                // 查找吸顶View（通过id查找）
                for (i in 0 until container.childCount) {
                    val child = container.getChildAt(i)
                    if (child.id == R.id.sticky_header_container) {
                        stickyView = child
                        expandedView = child.findViewById(R.id.expanded_view)
                        collapsedView = child.findViewById(R.id.collapsed_view)

                        // 初始化状态
                        expandedView?.alpha = 1f
                        collapsedView?.alpha = 0f

                        // 获取吸顶View的原始位置和高度
                        child.post {
                            stickyViewOriginalTop = child.top
                            stickyViewHeight = child.height
                        }
                        break
                    }
                }

                // 查找内容View（吸顶View后面的View）
                stickyView?.let { sticky ->
                    val stickyIndex = container.indexOfChild(sticky)
                    if (stickyIndex >= 0 && stickyIndex + 1 < container.childCount) {
                        contentView = container.getChildAt(stickyIndex + 1)
                    }
                }
            }
        }
    }

    override fun onScrollChanged(scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        super.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY)

        stickyView?.let { sticky ->
            handleStickyScroll(scrollY)
        }

        lastScrollY = scrollY
    }

    private fun handleStickyScroll(scrollY: Int) {
        val sticky = stickyView ?: return

        // 计算吸顶View相对于容器顶部的位置
        val stickyTop = stickyViewOriginalTop - scrollY

        when {
            stickyTop <= 0 -> {
                // 需要吸顶
                if (!isSticky) {
                    isSticky = true
                    // 吸顶时隐藏原始View，避免重复显示
                    sticky.visibility = View.INVISIBLE
                }

                // 完全变换到折叠状态
                updateStickyTransition(1f)
            }

            stickyTop > 0 -> {
                // 不需要吸顶
                if (isSticky) {
                    isSticky = false
                    // 恢复原始View的可见性
                    sticky.visibility = View.VISIBLE
                }

                // 根据滚动位置计算变换进度
                val progress = if (stickyViewOriginalTop > 0) {
                    val scrollProgress = scrollY.toFloat() / stickyViewOriginalTop.toFloat()
                    scrollProgress.coerceIn(0f, 1f)
                } else {
                    0f
                }

                updateStickyTransition(progress)
            }
        }
    }

    private fun updateStickyTransition(progress: Float) {
        val clampedProgress = progress.coerceIn(0f, 1f)

        // 透明度变化
        expandedView?.alpha = 1f - clampedProgress
        collapsedView?.alpha = clampedProgress

        // 缩放效果
        expandedView?.scaleY = 1f - clampedProgress * 0.2f
        collapsedView?.scaleY = 0.8f + clampedProgress * 0.2f
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // 确保触摸事件能正确处理
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // 处理触摸事件
        return super.onTouchEvent(ev)
    }

    // 重写fling方法以确保平滑滚动
    override fun fling(velocityY: Int) {
        super.fling(velocityY)
    }

    // 重写滚动方法以确保平滑体验
    override fun scrollTo(x: Int, y: Int) {
        super.scrollTo(x, y)
        // 滚动时立即更新吸顶状态
        post {
            stickyView?.let { handleStickyScroll(scrollY) }
        }
    }

    // 重写测量方法确保布局正确
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // 确保在测量完成后更新View引用
        post {
            if (stickyView == null) {
                findViews()
            }
        }
    }

    // 重写dispatchDraw确保吸顶View绘制在最上层
    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        super.dispatchDraw(canvas)

        // 如果处于吸顶状态，在NestedScrollView顶部绘制吸顶View
        if (isSticky) {
            stickyView?.let { sticky ->
                canvas.save()

                // 设置裁剪区域，确保只在NestedScrollView的可视区域内绘制
                canvas.clipRect(0, scrollY, width, scrollY + height)

                // 将画布移动到NestedScrollView的顶部位置
                canvas.translate(0f, scrollY.toFloat())

                // 绘制吸顶View
                sticky.draw(canvas)

                canvas.restore()
            }
        }
    }

    // 添加公共方法供外部调用
    fun getStickyView(): View? = stickyView

    fun isInStickyMode(): Boolean = isSticky

    fun scrollToSticky() {
        if (stickyViewOriginalTop > 0) {
            // 使用smoothScrollBy代替smoothScrollTo
            val currentY = scrollY
            val targetY = stickyViewOriginalTop
            smoothScrollBy(0, targetY - currentY)
        }
    }
}