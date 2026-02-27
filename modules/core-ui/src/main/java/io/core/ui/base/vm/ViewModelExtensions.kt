package io.core.ui.base.vm

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * BaseViewModelV2 扩展方法
 * 
 * 提供便捷的扩展方法来简化ViewModel的使�?
 */

/**
 * 观察视图状态的扩展方法
 * 
 * @param lifecycleOwner 生命周期拥有�?
 * @param onIdle 空闲状态回�?
 * @param onLoading 加载状态回�?
 * @param onSuccess 成功状态回�?
 * @param onError 错误状态回�?
 * @param onEmpty 空数据状态回�?
 */
fun <T> BaseViewModelV2<T>.observeViewState(
    lifecycleOwner: LifecycleOwner,
    onIdle: (() -> Unit)? = null,
    onLoading: (() -> Unit)? = null,
    onSuccess: ((T) -> Unit)? = null,
    onError: ((String, Int?) -> Unit)? = null,
    onEmpty: (() -> Unit)? = null
) {
    lifecycleOwner.lifecycleScope.launch {
        viewState.collectLatest { state ->
            when (state) {
                is ViewState.Idle -> onIdle?.invoke()
                is ViewState.Loading -> onLoading?.invoke()
                is ViewState.Success -> onSuccess?.invoke(state.data)
                is ViewState.Error -> onError?.invoke(state.message, state.code)
                is ViewState.Empty -> onEmpty?.invoke()
            }
        }
    }
}

/**
 * 观察UI事件的扩展方�?
 * 
 * @param lifecycleOwner 生命周期拥有�?
 * @param onShowToast Toast事件回调
 * @param onShowSnackBar SnackBar事件回调
 * @param onShowDialog 对话框事件回�?
 * @param onHideKeyboard 隐藏键盘事件回调
 * @param onShowLoading 显示加载事件回调
 * @param onHideLoading 隐藏加载事件回调
 * @param onNavigateTo 导航事件回调
 * @param onNavigateBack 返回事件回调
 */
fun <T> BaseViewModelV2<T>.observeUiEvents(
    lifecycleOwner: LifecycleOwner,
    onShowToast: ((String) -> Unit)? = null,
    onShowSnackBar: ((String, String?, (() -> Unit)?) -> Unit)? = null,
    onShowDialog: ((String, String, (() -> Unit)?) -> Unit)? = null,
    onHideKeyboard: (() -> Unit)? = null,
    onShowLoading: (() -> Unit)? = null,
    onHideLoading: (() -> Unit)? = null,
    onNavigateTo: ((String) -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null
) {
    lifecycleOwner.lifecycleScope.launch {
        uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowToast -> onShowToast?.invoke(event.message)
                is UiEvent.ShowSnackBar -> onShowSnackBar?.invoke(
                    event.message, 
                    event.actionText, 
                    event.action
                )
                is UiEvent.ShowDialog -> onShowDialog?.invoke(
                    event.title, 
                    event.message, 
                    event.onConfirm
                )
                is UiEvent.HideKeyboard -> onHideKeyboard?.invoke()
                is UiEvent.ShowLoading -> onShowLoading?.invoke()
                is UiEvent.HideLoading -> onHideLoading?.invoke()
                is UiEvent.NavigateTo -> onNavigateTo?.invoke(event.route)
                is UiEvent.NavigateBack -> onNavigateBack?.invoke()
            }
        }
    }
}

/**
 * 简化的状态观察方�?
 * 只关注成功和错误状�?
 */
fun <T> BaseViewModelV2<T>.observeResult(
    lifecycleOwner: LifecycleOwner,
    onSuccess: (T) -> Unit,
    onError: ((String) -> Unit)? = null,
    onLoading: (() -> Unit)? = null
) {
    observeViewState(
        lifecycleOwner = lifecycleOwner,
        onLoading = onLoading,
        onSuccess = onSuccess,
        onError = { message, _ -> onError?.invoke(message) }
    )
}

/**
 * ViewState 扩展属�?
 */

/**
 * 检查是否为成功状�?
 */
val <T> ViewState<T>.isSuccess: Boolean
    get() = this is ViewState.Success

/**
 * 检查是否为错误状�?
 */
val <T> ViewState<T>.isError: Boolean
    get() = this is ViewState.Error

/**
 * 检查是否为加载状�?
 */
val <T> ViewState<T>.isLoading: Boolean
    get() = this is ViewState.Loading

/**
 * 检查是否为空状�?
 */
val <T> ViewState<T>.isEmpty: Boolean
    get() = this is ViewState.Empty

/**
 * 检查是否为空闲状�?
 */
val <T> ViewState<T>.isIdle: Boolean
    get() = this is ViewState.Idle

/**
 * 获取成功状态的数据，如果不是成功状态则返回null
 */
val <T> ViewState<T>.dataOrNull: T?
    get() = if (this is ViewState.Success) this.data else null

/**
 * 获取错误状态的消息，如果不是错误状态则返回null
 */
val <T> ViewState<T>.errorMessageOrNull: String?
    get() = if (this is ViewState.Error) this.message else null

/**
 * 便捷的状态创建方�?
 */
object ViewStateFactory {
    
    /**
     * 创建成功状�?
     */
    fun <T> success(data: T): ViewState<T> = ViewState.Success(data)
    
    /**
     * 创建错误状�?
     */
    fun <T> error(message: String, code: Int? = null): ViewState<T> = ViewState.Error(message, code)
    
    /**
     * 创建加载状�?
     */
    fun <T> loading(): ViewState<T> = ViewState.Loading
    
    /**
     * 创建空状�?
     */
    fun <T> empty(): ViewState<T> = ViewState.Empty
    
    /**
     * 创建空闲状�?
     */
    fun <T> idle(): ViewState<T> = ViewState.Idle
}

/**
 * UiEvent 便捷创建方法
 */
object UiEventFactory {
    
    /**
     * 创建Toast事件
     */
    fun toast(message: String): UiEvent = UiEvent.ShowToast(message)
    
    /**
     * 创建SnackBar事件
     */
    fun snackBar(
        message: String, 
        actionText: String? = null, 
        action: (() -> Unit)? = null
    ): UiEvent = UiEvent.ShowSnackBar(message, actionText, action)
    
    /**
     * 创建对话框事�?
     */
    fun dialog(
        title: String, 
        message: String, 
        onConfirm: (() -> Unit)? = null
    ): UiEvent = UiEvent.ShowDialog(title, message, onConfirm)
    
    /**
     * 创建导航事件
     */
    fun navigate(route: String): UiEvent = UiEvent.NavigateTo(route)
    
    /**
     * 创建返回事件
     */
    fun back(): UiEvent = UiEvent.NavigateBack
    
    /**
     * 创建隐藏键盘事件
     */
    fun hideKeyboard(): UiEvent = UiEvent.HideKeyboard
}

/**
 * 类型别名，简化使�?
 */
typealias VState<T> = ViewState<T>
typealias UEvent = UiEvent
typealias BaseVM<T> = BaseViewModelV2<T>