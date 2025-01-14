package com.core.fy.android.interfaces

import android.os.SystemClock
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.core.fy.android.R
import io.core.engine.state.StateChangedHandler
import io.core.engine.state.StateLayout
import io.core.engine.state.Status

/**
 * 适用于骨骼图动画, 能保证动画至少完整执行一次动画或者显示最短时间, 避免屏幕闪烁
 *
 * @param leastDuration 至少显示动画多长时间, 如果为null则至少显示动画完整播放一次的时间
 */
open class LeastAnimationStateChangedHandler(private var leastDuration: Long? = null) :
    StateChangedHandler {

    /** 加载状态开始时间 */
    private var loadingStartTime = 0L
    private var next: View? = null
    private var animationPlaying = false

    override fun onRemove(container: StateLayout, state: View, status: Status, tag: Any?) {
        if (status == Status.LOADING) {
            val animation = state.findViewById<LottieAnimationView>(R.id.lottie)
            animation?.addAnimatorUpdateListener {
                val duration = SystemClock.elapsedRealtime() - loadingStartTime
                if (duration >= (leastDuration ?: it.duration)) {
                    animationPlaying = false
                    animation.cancelAnimation()
                    animation.removeAllUpdateListeners()
                    for (i in 0 until container.childCount) {
                        container.getChildAt(i).visibility = View.GONE
                    }
                    next?.let { state ->
                        StateChangedHandler.onAdd(container, state, status, tag)
                    }
                }
            }
        } else {
            super.onRemove(container, state, status, tag)
        }
    }

    override fun onAdd(container: StateLayout, state: View, status: Status, tag: Any?) {
        next = state
        if (animationPlaying) return
        StateChangedHandler.onAdd(container, state, status, tag)
        if (status == Status.LOADING) {
            loadingStartTime = SystemClock.elapsedRealtime()
            state.findViewById<LottieAnimationView>(R.id.lottie)?.let {
                animationPlaying = true
                it.repeatCount = LottieDrawable.INFINITE
                it.playAnimation()
            }
        }
    }
}