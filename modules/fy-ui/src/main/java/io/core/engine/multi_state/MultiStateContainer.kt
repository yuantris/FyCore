package io.core.engine.multi_state

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import io.core.engine.multi_state.state.SuccessState

@Suppress("UNCHECKED_CAST")
open class MultiStateContainer : FrameLayout {

    private var originTargetView: View? = null

    private var lastState: MultiState? = null

    var currentState: MultiState? = null

    private var statePool: MutableMap<Class<out MultiState>, MultiState> = mutableMapOf()

    private var animator = ValueAnimator.ofFloat(0.0f, 1.0f).apply {
        duration = MultiStatePage.config.alphaDuration
    }

    constructor(
        context: Context,
        originTargetView: View,
    ) : this(context, null) {
        this.originTargetView = originTargetView
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
    ) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (originTargetView == null && childCount == 1) {
            originTargetView = getChildAt(0)
        }
    }

    fun initialization() {
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        addView(originTargetView, 0, layoutParams)
    }

    inline fun <reified T : MultiState> show(enableAnimator: Boolean = true, noinline notify: (T) -> Unit = {}) {
        show(T::class.java, enableAnimator, notify)
    }

    @JvmOverloads
    fun <T : MultiState> show(
        multiState: T,
        enableAnimator: Boolean = true,
        onNotifyListener: OnNotifyListener<T>? = null,
    ) {
        if (childCount == 0) {
            initialization()
        }

        if (multiState is SuccessState) {
            if (childCount > 1) {
                removeViewAt(1)
            }
            if (lastState !is SuccessState) {
                originTargetView?.visibility = View.VISIBLE
                lastState?.onHiddenChanged(true)
                if (enableAnimator) {
                    originTargetView?.executeAnimator()
                }
            }
        } else {
            originTargetView?.visibility = View.INVISIBLE
            if (lastState != multiState) {
                if (childCount > 1) {
                    removeViewAt(1)
                }
                val currentStateView = multiState.onCreateView(context, LayoutInflater.from(context), this)
                multiState.onViewCreated(currentStateView)
                addView(currentStateView)
                lastState?.onHiddenChanged(true)
                multiState.onHiddenChanged(false)
                if (enableAnimator) {
                    currentStateView.executeAnimator()
                }
            }
            onNotifyListener?.onNotify(multiState)
        }
        currentState = multiState
        lastState = multiState
    }

    @JvmOverloads
    fun <T : MultiState> show(
        clazz: Class<T>,
        enableAnimator: Boolean = true,
        onNotifyListener: OnNotifyListener<T>? = null,
    ) {
        findState(clazz)?.let { multiState -> show(multiState as T, enableAnimator, onNotifyListener) }
    }

    private fun <T : MultiState> findState(clazz: Class<T>): MultiState? {
        return if (statePool.containsKey(clazz)) {
            statePool[clazz]
        } else {
            val state = clazz.newInstance()
            statePool[clazz] = state
            state
        }
    }

    private fun View.executeAnimator() {
        this.clearAnimation()
        animator.addUpdateListener {
            this.alpha = it.animatedValue as Float
        }
        animator.start()
    }
}