package io.core.widget.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import io.core.R
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AsyncStateImageView - 支持状态接收的图片视图
 * 
 * 增强版StatefulImageView，支持异步状态确认，多状态管理和动画效果
 */
class AsyncStateImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    /**
     * 视图状态枚举
     */
    enum class ViewState {
        DEFAULT,    // 默认状态
        LOADING,    // 加载中状态
        SELECTED,   // 选中状态
        ERROR       // 错误状态
    }

    /**
     * 状态变化回调接口
     */
    interface StateChangeCallback {
        /**
         * 状态变化请求
         * @param current 当前状态
         * @param requested 请求的状态
         * @return 返回true表示处理该请求，false表示不处理（将立即执行状态变化）
         */
        fun onStateChangeRequested(current: ViewState, requested: ViewState): Boolean = true
        
        /**
         * 状态变化完成
         * @param oldState 旧状态
         * @param newState 新状态
         */
        fun onStateChanged(oldState: ViewState, newState: ViewState) {}
        
        /**
         * 状态变化失败
         * @param requestedState 请求的状态
         * @param error 错误信息
         */
        fun onStateChangeFailed(requestedState: ViewState, error: Throwable?) {}
    }

    // 状态对应的Drawable资源
    private val stateDrawables = mutableMapOf<ViewState, Drawable?>()
    
    // 当前状态
    private var _currentState = ViewState.DEFAULT
    val currentState: ViewState get() = _currentState
    
    // 请求的状态（等待确认）
    private var pendingState: ViewState? = null
    
    // 状态变化锁（防止并发状态变化）
    private val stateChangeLock = AtomicBoolean(false)
    
    // 状态变化超时处理
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var stateChangeTimeout = DEFAULT_TIMEOUT
    
    // 动画控制
    private var animationEnabled = true
    private var animationDuration = DEFAULT_ANIMATION_DURATION
    private var currentAnimation: Animator? = null
    
    // 回调
    var stateChangeCallback: StateChangeCallback? = null
    var onStateChanged: ((oldState: ViewState, newState: ViewState) -> Unit)? = null

    init {
        // 确保可点击
        isClickable = true
        isFocusable = true

        // 解析自定义属性
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.AsyncStateImageView)
            try {
                // 加载各状态图片
                stateDrawables[ViewState.DEFAULT] = typedArray.getDrawable(
                    R.styleable.AsyncStateImageView_defaultImage
                ) ?: drawable
                
                stateDrawables[ViewState.LOADING] = typedArray.getDrawable(
                    R.styleable.AsyncStateImageView_loadingImage
                )
                
                stateDrawables[ViewState.SELECTED] = typedArray.getDrawable(
                    R.styleable.AsyncStateImageView_selectedImage
                )
                
                stateDrawables[ViewState.ERROR] = typedArray.getDrawable(
                    R.styleable.AsyncStateImageView_errorImage
                )
                
                // 初始状态
                val initialStateValue = typedArray.getInt(
                    R.styleable.AsyncStateImageView_initialViewState, 0
                )
                _currentState = ViewState.values()[initialStateValue.coerceIn(0, ViewState.values().size - 1)]
                
                // 动画设置
                animationEnabled = typedArray.getBoolean(
                    R.styleable.AsyncStateImageView_animationEnabled, true
                )
                
                animationDuration = typedArray.getInt(
                    R.styleable.AsyncStateImageView_animationDuration, DEFAULT_ANIMATION_DURATION.toInt()
                ).toLong()
                
                stateChangeTimeout = typedArray.getInt(
                    R.styleable.AsyncStateImageView_stateChangeTimeout, DEFAULT_TIMEOUT.toInt()
                ).toLong()
            } finally {
                typedArray.recycle()
            }
        }

        // 确保有默认图片
        if (stateDrawables[ViewState.DEFAULT] == null) {
            stateDrawables[ViewState.DEFAULT] = drawable
        }

        updateDrawableState()
    }

    /**
     * 请求状态变化
     * @param newState 新状态
     * @param timeout 超时时间（毫秒），默认使用全局设置
     */
    fun requestStateChange(newState: ViewState, timeout: Long = stateChangeTimeout) {
        // 确保在主线程执行
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { requestStateChange(newState, timeout) }
            return
        }
        
        // 验证状态变化是否合理
        if (!isValidStateTransition(_currentState, newState)) return
        
        // 如果有待处理的状态变化，取消它
        cancelPendingStateChange()
        
        // 如果状态变化锁被占用，不处理新的状态变化请求
        if (!stateChangeLock.compareAndSet(false, true)) return
        
        val callback = stateChangeCallback
        
        // 如果没有回调或回调不处理此请求，直接变更状态
        if (callback == null || !callback.onStateChangeRequested(_currentState, newState)) {
            applyStateChange(newState)
            stateChangeLock.set(false)
            return
        }
        
        // 设置待处理状态
        pendingState = newState
        
        // 设置超时处理
        timeoutRunnable = Runnable {
            if (pendingState != null) {
                val requestedState = pendingState
                pendingState = null
                stateChangeLock.set(false)
                callback.onStateChangeFailed(requestedState!!, TimeoutException())
            }
        }
        handler.postDelayed(timeoutRunnable!!, timeout)
    }

    /**
     * 确认状态变化
     * @param success 是否成功
     * @param error 错误信息（如果失败）
     */
    fun confirmStateChange(success: Boolean, error: Throwable? = null) {
        val requestedState = pendingState ?: return
        
        // 取消超时处理
        cancelPendingStateChange()
        
        if (success) {
            // 应用状态变化
            applyStateChange(requestedState)
        } else {
            // 通知状态变化失败
            stateChangeCallback?.onStateChangeFailed(requestedState, error)
        }
        
        // 重置状态
        pendingState = null
        stateChangeLock.set(false)
    }

    /**
     * 同步外部状态（直接设置状态，不触发回调）
     * @param state 状态
     * @param animated 是否使用动画
     */
    fun syncState(state: ViewState, animated: Boolean = true) {
        // 确保在主线程执行
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { syncState(state, animated) }
            return
        }
        
        // 取消任何待处理的状态变化
        cancelPendingStateChange()
        
        // 如果状态相同，不做处理
        if (_currentState == state) return
        
        val oldState = _currentState
        _currentState = state
        
        // 更新视图
        updateDrawableState(animated && animationEnabled)
        
        // 只触发onStateChanged回调，不触发StateChangeCallback
        onStateChanged?.invoke(oldState, state)
    }

    /**
     * 显示加载状态
     */
    fun showLoading() {
        if (_currentState != ViewState.LOADING) {
            val oldState = _currentState
            _currentState = ViewState.LOADING
            updateDrawableState(animationEnabled)
            
            // 如果是加载动画Drawable，启动它
            (stateDrawables[ViewState.LOADING] as? AnimationDrawable)?.start()
            
            // 不触发回调，因为这是临时状态
        }
    }

    /**
     * 隐藏加载状态
     * @param success 是否成功
     */
    fun hideLoading(success: Boolean = true) {
        if (_currentState == ViewState.LOADING) {
            // 停止加载动画
            (stateDrawables[ViewState.LOADING] as? AnimationDrawable)?.stop()
            
            // 如果有待处理状态，使用它；否则回到默认状态或错误状态
            val targetState = pendingState ?: if (success) ViewState.DEFAULT else ViewState.ERROR
            
            if (pendingState != null) {
                confirmStateChange(success)
            } else {
                syncState(targetState)
            }
        }
    }

    /**
     * 设置选中状态
     * @param selected 是否选中
     * @param animated 是否使用动画
     */
    fun setSelected(selected: Boolean, animated: Boolean = true) {
        val targetState = if (selected) ViewState.SELECTED else ViewState.DEFAULT
        requestStateChange(targetState)
    }

    /**
     * 设置状态对应的Drawable
     * @param state 状态
     * @param drawable Drawable资源
     */
    fun setStateDrawable(state: ViewState, drawable: Drawable?) {
        stateDrawables[state] = drawable
        if (_currentState == state) {
            updateDrawableState(false)
        }
    }

    /**
     * 设置状态对应的Drawable资源ID
     * @param state 状态
     * @param resId 资源ID
     */
    fun setStateDrawableResource(state: ViewState, @DrawableRes resId: Int) {
        setStateDrawable(state, AppCompatResources.getDrawable(context, resId))
    }

    /**
     * 设置是否启用动画
     * @param enabled 是否启用
     */
    fun setAnimationEnabled(enabled: Boolean) {
        animationEnabled = enabled
    }

    /**
     * 设置状态变化超时时间
     * @param timeout 超时时间（毫秒）
     */
    fun setStateChangeTimeout(timeout: Long) {
        stateChangeTimeout = timeout
    }

    /**
     * 处理点击事件，请求切换状态
     */
    override fun performClick(): Boolean {
        // 如果当前是选中状态，请求切换到默认状态；否则请求切换到选中状态
        val targetState = if (_currentState == ViewState.SELECTED) ViewState.DEFAULT else ViewState.SELECTED
        requestStateChange(targetState)
        return super.performClick()
    }

    /**
     * 应用状态变化
     * @param newState 新状态
     */
    private fun applyStateChange(newState: ViewState) {
        val oldState = _currentState
        _currentState = newState
        
        // 更新视图
        updateDrawableState(animationEnabled)
        
        // 触发回调
        stateChangeCallback?.onStateChanged(oldState, newState)
        onStateChanged?.invoke(oldState, newState)
    }

    /**
     * 取消待处理的状态变化
     */
    private fun cancelPendingStateChange() {
        timeoutRunnable?.let {
            handler.removeCallbacks(it)
            timeoutRunnable = null
        }
    }

    /**
     * 更新图片显示状态
     * @param animated 是否使用动画
     */
    private fun updateDrawableState(animated: Boolean = false) {
        // 获取当前状态对应的Drawable
        val targetDrawable = stateDrawables[_currentState] ?: stateDrawables[ViewState.DEFAULT]
        
        // 如果是加载状态且是AnimationDrawable，启动动画
        if (_currentState == ViewState.LOADING && targetDrawable is AnimationDrawable) {
            targetDrawable.start()
        }
        
        // 创建状态选择器
        createSelectorDrawable()?.let {
            if (animated) {
                animateDrawableChange(it)
            } else {
                super.setImageDrawable(it)
            }
        } ?: run {
            if (animated) {
                animateDrawableChange(targetDrawable)
            } else {
                super.setImageDrawable(targetDrawable)
            }
        }
    }

    /**
     * 创建带状态的Drawable选择器
     */
    private fun createSelectorDrawable(): StateListDrawable? {
        val defaultDrawable = stateDrawables[ViewState.DEFAULT] ?: return null
        val selectedDrawable = stateDrawables[ViewState.SELECTED]
        val errorDrawable = stateDrawables[ViewState.ERROR]

        return StateListDrawable().apply {
            // 错误状态
            if (errorDrawable != null && _currentState == ViewState.ERROR) {
                addState(intArrayOf(), errorDrawable)
                return@apply
            }
            
            // 按下状态优先
            addState(intArrayOf(android.R.attr.state_pressed),
                selectedDrawable ?: defaultDrawable)

            // 选中状态
            if (_currentState == ViewState.SELECTED && selectedDrawable != null) {
                addState(intArrayOf(), selectedDrawable)
            } else {
                // 默认状态
                addState(intArrayOf(), defaultDrawable)
            }
        }
    }

    /**
     * 动画切换Drawable
     * @param newDrawable 新的Drawable
     */
    /**
     * 动画切换Drawable
     * @param newDrawable 新的Drawable
     */
    private fun animateDrawableChange(newDrawable: Drawable?) {
        // 取消当前动画
        currentAnimation?.cancel()

        // 创建淡出动画
        val fadeOutAnimator = ObjectAnimator.ofFloat(this, "alpha", alpha, 0f)
        fadeOutAnimator.duration = animationDuration / 2
        fadeOutAnimator.interpolator = AccelerateDecelerateInterpolator()

        fadeOutAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 切换图片
                super@AsyncStateImageView.setImageDrawable(newDrawable)

                // 创建淡入动画
                val fadeInAnimator = ObjectAnimator.ofFloat(this@AsyncStateImageView, "alpha", 0f, 1f)
                fadeInAnimator.duration = animationDuration / 2
                fadeInAnimator.interpolator = AccelerateDecelerateInterpolator()

                fadeInAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        currentAnimation = null
                    }
                })

                currentAnimation = fadeInAnimator
                fadeInAnimator.start()
            }
        })

        currentAnimation = fadeOutAnimator
        fadeOutAnimator.start()
    }


    /**
     * 错误动画（左右抖动）
     */
    fun playErrorAnimation() {
        // 取消当前动画避免冲突
        currentAnimation?.cancel()
        
        val animator = ObjectAnimator.ofFloat(
            this, View.TRANSLATION_X,
            0f, -10f, 10f, -10f, 10f, -5f, 5f, 0f
        )
        animator.duration = ERROR_ANIMATION_DURATION
        currentAnimation = animator
        animator.start()
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        
        // 取消所有动画
        currentAnimation?.cancel()
        currentAnimation = null
        
        // 清理超时处理
        cancelPendingStateChange()
        
        // 重置状态锁
        stateChangeLock.set(false)
        pendingState = null
        
        // 停止加载动画
        (stateDrawables[ViewState.LOADING] as? AnimationDrawable)?.stop()
    }

    /**
     * 获取当前是否有待处理的状态变化
     */
    fun hasPendingStateChange(): Boolean = pendingState != null

    /**
     * 强制取消待处理的状态变化
     */
    fun cancelPendingStateChangeForce() {
        cancelPendingStateChange()
        pendingState = null
        stateChangeLock.set(false)
    }

    /**
     * 验证状态变化是否合理
     * @param from 源状态
     * @param to 目标状态
     * @return 是否允许此状态变化
     */
    private fun isValidStateTransition(from: ViewState, to: ViewState): Boolean {
        // 相同状态不需要变化
        if (from == to) return false
        
        // 从加载状态只能通过confirmStateChange或hideLoading来切换
        // 这里我们允许所有状态变化，但在实际使用中可以根据业务逻辑限制
        return true
    }

    /**
     * 获取状态描述（用于调试）
     */
    fun getStateDescriptionDebug(): String {
        return "Current: $_currentState, Pending: $pendingState, Locked: ${stateChangeLock.get()}"
    }

    // 禁止直接设置图片，强制使用状态控制
    @Deprecated("Use setStateDrawable or setStateDrawableResource instead",
        ReplaceWith("setStateDrawable(ViewState.DEFAULT, drawable)"))
    override fun setImageDrawable(drawable: Drawable?) {
        setStateDrawable(ViewState.DEFAULT, drawable)
    }

    @Deprecated("Use setStateDrawableResource instead",
        ReplaceWith("setStateDrawableResource(ViewState.DEFAULT, resId)"))
    override fun setImageResource(resId: Int) {
        setStateDrawableResource(ViewState.DEFAULT, resId)
    }

    /**
     * 超时异常
     */
    class TimeoutException : Exception("State change request timed out")

    companion object {
        private const val DEFAULT_TIMEOUT = 5000L // 默认超时时间：5秒
        private const val DEFAULT_ANIMATION_DURATION = 300L // 默认动画时长：300毫秒
        private const val ERROR_ANIMATION_DURATION = 500L // 错误动画时长：500毫秒
    }
}