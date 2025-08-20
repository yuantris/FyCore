package io.core.widget.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import io.core.R

/**
 * StatefulImageViewV2 - 支持状态接收的图片视图
 * 
 * 增强版StatefulImageView，支持异步状态确认，多状态管理和动画效果
 */
class StatefulImageViewV2 @JvmOverloads constructor(
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
     * 点击事件回调接口
     */
    interface OnViewClickListener {
        /**
         * 视图被点击
         * @param currentState 当前状态
         * @param view 被点击的视图
         */
        fun onViewClicked(currentState: ViewState, view: StatefulImageViewV2)
    }

    // 状态对应的Drawable资源
    private val stateDrawables = mutableMapOf<ViewState, Drawable?>()
    
    // 当前状态
    private var _currentState = ViewState.DEFAULT
    val currentState: ViewState get() = _currentState
    
    // 动画控制
    private var animationEnabled = true
    private var animationDuration = DEFAULT_ANIMATION_DURATION
    
    // Handler for main thread operations
    private val handler = Handler(Looper.getMainLooper())
    
    // 回调
    var onViewClickListener: OnViewClickListener? = null
    var onStateChanged: ((oldState: ViewState, newState: ViewState) -> Unit)? = null

    init {
        // 确保可点击
        isClickable = true
        isFocusable = true
        
        // 禁用系统的状态响应，避免与自定义状态冲突
        isDuplicateParentStateEnabled = false

        // 解析自定义属性
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.StatefulImageViewV2)
            try {
                // 加载各状态图片
                stateDrawables[ViewState.DEFAULT] = typedArray.getDrawable(
                    R.styleable.StatefulImageViewV2_defaultImage
                ) ?: drawable
                
                stateDrawables[ViewState.LOADING] = typedArray.getDrawable(
                    R.styleable.StatefulImageViewV2_loadingImage
                )
                
                stateDrawables[ViewState.SELECTED] = typedArray.getDrawable(
                    R.styleable.StatefulImageViewV2_selectedImage
                )
                
                stateDrawables[ViewState.ERROR] = typedArray.getDrawable(
                    R.styleable.StatefulImageViewV2_errorImage
                )
                
                // 初始状态
                val initialStateValue = typedArray.getInt(
                    R.styleable.StatefulImageViewV2_initialViewState, 0
                )
                _currentState = ViewState.values()[initialStateValue.coerceIn(0, ViewState.values().size - 1)]
                
                // 动画设置
                animationEnabled = typedArray.getBoolean(
                    R.styleable.StatefulImageViewV2_animationEnabled, true
                )
                
                animationDuration = typedArray.getInt(
                    R.styleable.StatefulImageViewV2_animationDuration, DEFAULT_ANIMATION_DURATION.toInt()
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
            _currentState = ViewState.LOADING
            updateDrawableState(animationEnabled)
            
            // 如果是加载动画Drawable，启动它
            (stateDrawables[ViewState.LOADING] as? AnimationDrawable)?.start()
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
            
            // 回到默认状态或错误状态
            val targetState = if (success) ViewState.DEFAULT else ViewState.ERROR
            syncState(targetState)
        }
    }

    /**
     * 设置选中状态
     * @param selected 是否选中
     * @param animated 是否使用动画
     */
    fun setSelected(selected: Boolean, animated: Boolean = true) {
        val targetState = if (selected) ViewState.SELECTED else ViewState.DEFAULT
        syncState(targetState, animated)
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
     * 处理点击事件，通知外部处理
     */
    override fun performClick(): Boolean {
        // 通知外部处理点击事件，不再自动切换状态
        onViewClickListener?.onViewClicked(_currentState, this)
        return super.performClick()
    }

    /**
     * 更新图片显示状态
     * @param animated 是否使用动画
     */
    private fun updateDrawableState(animated: Boolean = false) {
        // 直接获取当前状态对应的Drawable
        val targetDrawable = stateDrawables[_currentState] ?: stateDrawables[ViewState.DEFAULT]

        // 如果是加载状态且是AnimationDrawable，启动动画
        if (_currentState == ViewState.LOADING && targetDrawable is AnimationDrawable) {
            targetDrawable.start()
        }

        // 停止其他状态的动画
        if (_currentState != ViewState.LOADING) {
            (stateDrawables[ViewState.LOADING] as? AnimationDrawable)?.stop()
        }

        // 直接设置对应状态的图片，不使用StateListDrawable
        if (animated) {
            animateDrawableChange(targetDrawable)
        } else {
            super.setImageDrawable(targetDrawable)
        }
    }


    /**
     * 动画切换Drawable
     * @param newDrawable 新的Drawable
     */
    private fun animateDrawableChange(newDrawable: Drawable?) {
        if (animationEnabled && newDrawable != null) {
            clearAnimation()
            // 使用简单的透明度动画，避免TransitionDrawable的双图片问题
            animate()
                .alpha(0f)
                .setDuration(animationDuration / 2)
                .withEndAction {
                    super.setImageDrawable(newDrawable)
                    animate()
                        .alpha(1f)
                        .setDuration(animationDuration / 2)
                        .start()
                }
                .start()
        } else {
            // 无动画时直接设置
            super.setImageDrawable(newDrawable)
        }
    }



    /**
     * 错误动画（左右抖动）
     */
    fun playErrorAnimation() {
        // 清除当前动画
        clearAnimation()

        val animator = ObjectAnimator.ofFloat(
            this, View.TRANSLATION_X,
            0f, -10f, 10f, -10f, 10f, -5f, 5f, 0f
        )
        animator.duration = ERROR_ANIMATION_DURATION
        animator.start()
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        
        // 停止加载动画
        (stateDrawables[ViewState.LOADING] as? AnimationDrawable)?.stop()
    }

    /**
     * 获取状态描述（用于调试）
     */
    fun getStateDescriptionDebug(): String {
        return "Current: $_currentState"
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

    companion object {
        private const val DEFAULT_ANIMATION_DURATION = 300L // 默认动画时长：300毫秒
        private const val ERROR_ANIMATION_DURATION = 500L // 错误动画时长：500毫秒
    }
}