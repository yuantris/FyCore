package com.core.fy.android.function.fold

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.core.fy.android.R
import io.core.common.util.log.LogPure

class SmoothStickHeaderBehavior(context: Context, attrs: AttributeSet) :
    CoordinatorLayout.Behavior<View>(context, attrs) {

    private var mOriginalTop = -1
    private var mStickyTop = 0
    private var mIsSticky = false

    // 两个不同的View
    private var expandedView: View? = null
    private var collapsedView: View? = null

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: View,
        layoutDirection: Int
    ): Boolean {
        val handled = super.onLayoutChild(parent, child, layoutDirection)

        if (mOriginalTop == -1) {
            // 等待布局完成后再获取位置
            child.post {
                mOriginalTop = child.top

                // 找到两个子View
                expandedView = child.findViewById(R.id.expanded_view)
                collapsedView = child.findViewById(R.id.collapsed_view)

                // 初始状态：显示展开View，隐藏折叠View
                expandedView?.alpha = 1f
                collapsedView?.alpha = 0f
            }
        }

        return handled
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        val currentTop = child.top
        val newTop = currentTop - dy

        when {
            dy > 0 -> { // 向上滑动
                handleUpwardScroll(child, currentTop, newTop, dy, consumed)
            }

            dy < 0 -> { // 向下滑动
                handleDownwardScroll(child, target, currentTop, newTop, dy, consumed)
            }
        }
    }

    private fun handleUpwardScroll(
        child: View,
        currentTop: Int,
        newTop: Int,
        dy: Int,
        consumed: IntArray
    ) {
        LogPure.d { "handleUpwardScroll" }
        when {
            currentTop > mStickyTop -> {
                // 还没到吸顶位置，View跟随滑动
                val moveDistance = if (newTop >= mStickyTop) {
                    dy // 正常移动
                } else {
                    currentTop - mStickyTop // 移动到吸顶位置
                }
                ViewCompat.offsetTopAndBottom(child, -moveDistance)
                consumed[1] = moveDistance

                // 计算变换进度 (0.0 到 1.0)
                val progress = 1f - (child.top.toFloat() / mOriginalTop.toFloat())
                updateViewTransition(progress)

                // 检查是否刚好到达吸顶位置
                if (child.top <= mStickyTop) {
                    mIsSticky = true
                }
            }
            // 如果已经吸顶(currentTop <= mStickyTop)，不消费事件，让内容滑动
        }
    }

    private fun handleDownwardScroll(
        child: View,
        target: View,
        currentTop: Int,
        newTop: Int,
        dy: Int,
        consumed: IntArray
    ) {
        Log.d("StickyBehavior", "=== Down Scroll Debug ===")
        Log.d("StickyBehavior", "currentTop: $currentTop, newTop: $newTop, dy: $dy")
        Log.d("StickyBehavior", "mOriginalTop: $mOriginalTop, mStickyTop: $mStickyTop")
        Log.d("StickyBehavior", "mIsSticky: $mIsSticky")
        Log.d("StickyBehavior", "canScrollVertically(-1): ${target.canScrollVertically(-1)}")

        when {
            mIsSticky -> {
                if (!target.canScrollVertically(-1)) {
                    val moveDistance = if (newTop <= mOriginalTop) {
                        -dy
                    } else {
                        mOriginalTop - currentTop
                    }

                    Log.d("StickyBehavior", "Moving by: $moveDistance")
                    Log.d("StickyBehavior", "Before move - child.top: ${child.top}")

                    ViewCompat.offsetTopAndBottom(child, moveDistance)

                    Log.d("StickyBehavior", "After move - child.top: ${child.top}")

                    // 添加这里：计算向下滑动的进度并更新视图变换
                    val progress = 1f - (child.top.toFloat() / mOriginalTop.toFloat())
                    updateViewTransition(progress)

                    consumed[1] = dy

                    if (child.top >= mOriginalTop) {
                        mIsSticky = false
                        Log.d("StickyBehavior", "Sticky state changed to false")
                        // 确保精确定位
                        val finalAdjust = mOriginalTop - child.top
                        if (finalAdjust != 0) {
                            ViewCompat.offsetTopAndBottom(child, finalAdjust)
                            Log.d(
                                "StickyBehavior",
                                "Final adjust: $finalAdjust, final top: ${child.top}"
                            )
                        }
                        // 确保完全恢复到展开状态
                        updateViewTransition(0f)
                    }
                } else {
                    Log.d("StickyBehavior", "Target can still scroll up, not moving sticky view")
                }
            }

            currentTop < mOriginalTop -> {
                Log.d("StickyBehavior", "Non-sticky state, restoring position")
                // 非吸顶状态但位置不在原始位置，需要恢复
                val moveDistance = if (newTop <= mOriginalTop) {
                    -dy
                } else {
                    mOriginalTop - currentTop
                }
                ViewCompat.offsetTopAndBottom(child, moveDistance)

                // 添加这里：计算恢复过程的进度并更新视图变换
                val progress = 1f - (child.top.toFloat() / mOriginalTop.toFloat())
                updateViewTransition(progress)

                consumed[1] = dy
            }
        }
    }


    private fun updateViewTransition(progress: Float) {
        val clampedProgress = progress.coerceIn(0f, 1f)

        // 透明度变化
        expandedView?.alpha = 1f - clampedProgress
        collapsedView?.alpha = clampedProgress

        // 可以添加更多变换效果
        // 比如缩放、位移等
        expandedView?.scaleY = 1f - clampedProgress * 0.2f
        collapsedView?.scaleY = 0.8f + clampedProgress * 0.2f
    }
}

