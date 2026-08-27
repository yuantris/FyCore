package io.core.common.base.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 视图状态密封类
 * 
 * @param T 数据类型
 */
sealed class ViewState<out T> {
    /** 初始状态 */
    object Idle : ViewState<Nothing>()
    
    /** 加载中状态 */
    object Loading : ViewState<Nothing>()
    
    /** 成功状态，携带数据 */
    data class Success<T>(val data: T) : ViewState<T>()
    
    /** 错误状态 */
    data class Error(val message: String, val code: Int? = null) : ViewState<Nothing>()
    
    /** 空数据状态 */
    object Empty : ViewState<Nothing>()
}

/**
 * UI事件密封类
 * 用于处理一次性UI事件
 */
sealed class UiEvent {
    /** 显示Toast消息 */
    data class ShowToast(val message: String) : UiEvent()
    
    /** 显示SnackBar，支持操作按钮 */
    data class ShowSnackBar(
        val message: String, 
        val actionText: String? = null, 
        val action: (() -> Unit)? = null
    ) : UiEvent()
    
    /** 显示对话框 */
    data class ShowDialog(
        val title: String, 
        val message: String, 
        val onConfirm: (() -> Unit)? = null
    ) : UiEvent()
    
    /** 隐藏软键盘 */
    object HideKeyboard : UiEvent()
    
    /** 显示加载指示器 */
    object ShowLoading : UiEvent()
    
    /** 隐藏加载指示器 */
    object HideLoading : UiEvent()
    
    /** 导航到指定路由 */
    data class NavigateTo(val route: String) : UiEvent()
    
    /** 返回上一页 */
    object NavigateBack : UiEvent()
}

/**
 * BaseViewModelV2 - 全新设计的ViewModel基类
 * 
 * 主要功能：
 * - 统一的状态管理（ViewState）
 * - 一次性UI事件处理（UiEvent）
 * - 针对API、数据库、文件操作的专门方法
 * - 完善的错误处理机制
 * - 简洁的异步操作封装
 * 
 * @param T 主要数据类型
 */
abstract class BaseViewModelV2<T> : ViewModel() {
    
    // ==================== 状态管理 ====================
    
    /** 视图状态流 */
    private val _viewState = MutableStateFlow<ViewState<T>>(ViewState.Idle)
    val viewState: StateFlow<ViewState<T>> = _viewState.asStateFlow()
    
    /** UI事件流 */
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()
    
    /** 加载状态流 */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // ==================== 核心执行方法 ====================
    
    /**
     * 通用异步执行器
     * 
     * @param showLoading 是否显示加载状态
     * @param onStart 开始执行回调
     * @param onComplete 完成回调（无论成功失败都会调用）
     * @param onError 错误回调
     * @param action 要执行的异步操作
     */
    protected fun execute(
        showLoading: Boolean = true,
        onStart: (() -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null,
        action: suspend () -> T
    ) {
        viewModelScope.launch {
            try {
                onStart?.invoke()
                if (showLoading) {
                    setLoading(true)
                    emitEvent(UiEvent.ShowLoading)
                }
                
                val result = action()
                _viewState.value = ViewState.Success(result)
                
            } catch (e: Exception) {
                handleError(e)
                onError?.invoke(e)
            } finally {
                if (showLoading) {
                    setLoading(false)
                    emitEvent(UiEvent.HideLoading)
                }
                onComplete?.invoke()
            }
        }
    }
    
    /**
     * 带成功回调的异步执行器
     * 
     * @param showLoading 是否显示加载状态
     * @param onStart 开始执行回调
     * @param onSuccess 成功回调
     * @param onComplete 完成回调（无论成功失败都会调用）
     * @param onError 错误回调
     * @param action 要执行的异步操作
     */
    protected fun executeWithSuccess(
        showLoading: Boolean = true,
        onStart: (() -> Unit)? = null,
        onSuccess: ((T) -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null,
        action: suspend () -> T
    ) {
        viewModelScope.launch {
            try {
                onStart?.invoke()
                if (showLoading) {
                    setLoading(true)
                    emitEvent(UiEvent.ShowLoading)
                }
                
                val result = action()
                _viewState.value = ViewState.Success(result)
                onSuccess?.invoke(result)
                
            } catch (e: Exception) {
                handleError(e)
                onError?.invoke(e)
            } finally {
                if (showLoading) {
                    setLoading(false)
                    emitEvent(UiEvent.HideLoading)
                }
                onComplete?.invoke()
            }
        }
    }
    
    /**
     * API请求专用方法
     * 针对网络请求进行了优化，包含专门的网络错误处理
     * 
     * @param api API调用函数
     * @param onSuccess 成功回调
     * @param onError 错误回调
     * @param showToast 是否在出错时显示Toast
     */
    protected fun apiCall(
        api: suspend () -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
        showToast: Boolean = false
    ) {
        executeWithSuccess(
            action = api,
            onSuccess = onSuccess,
            onError = { e ->
                val message = parseApiError(e)
                onError?.invoke(message)
                if (showToast) {
                    showToast(message)
                }
            }
        )
    }
    
    /**
     * 数据库操作专用方法
     * 针对数据库操作进行了优化，通常不显示loading（因为操作很快）
     * 
     * @param operation 数据库操作函数
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    protected fun dbOperation(
        operation: suspend () -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        executeWithSuccess(
            showLoading = false, // 数据库操作通常很快，不显示loading
            action = operation,
            onSuccess = onSuccess,
            onError = { e ->
                val message = "数据库操作失败: ${e.message}"
                onError?.invoke(message)
            }
        )
    }
    
    /**
     * 文件操作专用方法
     * 针对文件读写操作进行了优化，包含专门的文件错误处理
     * 
     * @param operation 文件操作函数
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    protected fun fileOperation(
        operation: suspend () -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        executeWithSuccess(
            action = operation,
            onSuccess = onSuccess,
            onError = { e ->
                val message = parseFileError(e)
                onError?.invoke(message)
            }
        )
    }
    
    // ==================== 状态管理方法 ====================
    
    /**
     * 设置加载状态
     */
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
        if (loading) {
            _viewState.value = ViewState.Loading
        }
    }
    
    /**
     * 设置成功状态
     */
    protected fun setSuccess(data: T) {
        _viewState.value = ViewState.Success(data)
    }
    
    /**
     * 设置错误状态
     */
    protected fun setError(message: String, code: Int? = null) {
        _viewState.value = ViewState.Error(message, code)
    }
    
    /**
     * 设置空数据状态
     */
    protected fun setEmpty() {
        _viewState.value = ViewState.Empty
    }
    
    /**
     * 重置为初始状态
     */
    protected fun setIdle() {
        _viewState.value = ViewState.Idle
    }
    
    /**
     * 获取当前状态
     */
    protected fun getCurrentState(): ViewState<T> = _viewState.value
    
    /**
     * 检查是否为加载状态
     */
    protected fun isCurrentlyLoading(): Boolean = _isLoading.value
    
    // ==================== UI事件处理 ====================
    
    /**
     * 发送UI事件
     */
    protected fun emitEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }
    
    /**
     * 显示Toast消息
     */
    protected fun showToast(message: String) = emitEvent(UiEvent.ShowToast(message))
    
    /**
     * 显示SnackBar消息
     */
    protected fun showSnackBar(
        message: String, 
        actionText: String? = null, 
        action: (() -> Unit)? = null
    ) = emitEvent(UiEvent.ShowSnackBar(message, actionText, action))
    
    /**
     * 显示对话框
     */
    protected fun showDialog(
        title: String, 
        message: String, 
        onConfirm: (() -> Unit)? = null
    ) = emitEvent(UiEvent.ShowDialog(title, message, onConfirm))
    
    /**
     * 隐藏软键盘
     */
    protected fun hideKeyboard() = emitEvent(UiEvent.HideKeyboard)
    
    /**
     * 导航到指定页面
     */
    protected fun navigateTo(route: String) = emitEvent(UiEvent.NavigateTo(route))
    
    /**
     * 返回上一页
     */
    protected fun navigateBack() = emitEvent(UiEvent.NavigateBack)
    
    // ==================== 错误处理 ====================
    
    /**
     * 统一错误处理
     */
    private fun handleError(exception: Exception) {
        val message = when (exception) {
            is UnknownHostException -> "网络连接失败"
            is SocketTimeoutException -> "请求超时"
            is ConnectException -> "服务器连接失败"
            is FileNotFoundException -> "文件不存在"
            is IOException -> "IO操作失败"
            is IllegalArgumentException -> "参数错误"
            is SecurityException -> "权限不足"
            else -> exception.message ?: "未知错误"
        }
        setError(message)
    }
    
    /**
     * 解析API错误
     */
    private fun parseApiError(exception: Exception): String {
        return when (exception) {
            is UnknownHostException -> "网络连接失败，请检查网络设置"
            is SocketTimeoutException -> "请求超时，请稍后重试"
            is ConnectException -> "服务器连接失败，请稍后重试"
            else -> "请求失败: ${exception.message ?: "未知错误"}"
        }
    }
    
    /**
     * 解析文件操作错误
     */
    private fun parseFileError(exception: Exception): String {
        return when (exception) {
            is FileNotFoundException -> "文件不存在或无法访问"
            is IOException -> "文件读写失败"
            is SecurityException -> "没有文件访问权限"
            else -> "文件操作失败: ${exception.message ?: "未知错误"}"
        }
    }
    
    // ==================== 实用工具方法 ====================
    
    /**
     * 防抖动检查
     */
    private var lastActionTime = 0L
    protected fun isDoubleClick(intervalMs: Long = 500): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastActionTime < intervalMs) {
            true
        } else {
            lastActionTime = currentTime
            false
        }
    }
    
    /**
     * 延迟执行
     */
    protected fun delayedAction(delayMs: Long, action: () -> Unit) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(delayMs)
            action()
        }
    }
    
    /**
     * 重试机制
     */
    protected fun retryAction(
        maxRetries: Int = 3,
        delayMs: Long = 1000,
        action: suspend () -> T,
        onSuccess: ((T) -> Unit)? = null,
        onFinalError: ((Exception) -> Unit)? = null
    ) {
        viewModelScope.launch {
            var retryCount = 0
            var lastException: Exception? = null
            
            while (retryCount < maxRetries) {
                try {
                    val result = action()
                    setSuccess(result)
                    onSuccess?.invoke(result)
                    return@launch
                } catch (e: Exception) {
                    lastException = e
                    retryCount++
                    if (retryCount < maxRetries) {
                        kotlinx.coroutines.delay(delayMs)
                    }
                }
            }
            
            // 所有重试都失败了
            lastException?.let { e ->
                handleError(e)
                onFinalError?.invoke(e)
            }
        }
    }
}