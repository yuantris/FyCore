package io.core.ui.base.vm

import android.os.NetworkOnMainThreadException
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import io.core.common.helper.coroutine.Coroutine
import io.core.utils.extensions.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.coroutines.CoroutineContext

typealias Block<T> = suspend CoroutineScope.() -> T
typealias Error = suspend (e: Exception) -> Unit
typealias Cancel = suspend (e: Exception) -> Unit
typealias EmitBlock<T> = suspend LiveDataScope<T>.() -> T

/**
 * 基础 ViewModel 类，提供统一的协程管理和状态处�?
 *
 * 主要功能�?
 * - 统一的协程启动和错误处理
 * - 状态管理（Loading/Success/Error/Idle�?
 * - Flow 操作封装
 * - LiveData 便捷方法
 */
open class BaseViewModel : ViewModel() {

    private val _status = MutableLiveData<ViewStatus>(ViewStatus.IDLE)
    val viewState: LiveData<ViewStatus> = _status

    /**
     * 通用协程启动�?
     *
     * @param error 错误回调
     * @param cancel 取消回调
     * @param handleError 是否自动处理错误
     * @param context 协程上下�?
     * @param block 执行�?
     */
    protected fun launch(
        error: Error? = null,
        cancel: Cancel? = null,
        handleError: Boolean = true,
        context: CoroutineContext = Dispatchers.Default,
        block: Block<Unit>
    ): Job = viewModelScope.launch(context) {
        showLoading()
        runCatching {
            block(this)
        }.onSuccess {
            showSuccess()
        }.onFailure { e ->
            handleException(e, handleError, error, cancel)
        }
    }

    /**
     * 带作用域的协程启动器，支持成功回�?
     *
     * @param block 执行�?
     * @param success 成功回调
     * @param error 错误回调
     * @param cancel 取消回调
     * @param handleError 是否自动处理错误
     * @param context 协程上下�?
     */
    protected fun <T> launchScopedCoroutine(
        block: Block<T>,
        success: (CoroutineScope, T) -> Unit,
        error: Error? = null,
        cancel: Cancel? = null,
        handleError: Boolean = true,
        context: CoroutineContext = Dispatchers.Default
    ): Job = viewModelScope.launch(context) {
        showLoading()
        runCatching {
            block(this)
        }.onSuccess { result ->
            withContext(Dispatchers.Main) {
                success(this, result)
            }
            showSuccess()
        }.onFailure { e ->
            handleException(e, handleError, error, cancel)
        }
    }

    /**
     * LiveData 封装方法
     *
     * @param error 错误回调
     * @param cancel 取消回调
     * @param handleError 是否自动处理错误
     * @param block 执行�?
     */
    fun <T> emit(
        error: Error? = null,
        cancel: Cancel? = null,
        handleError: Boolean = true,
        block: EmitBlock<T>
    ): LiveData<T> = liveData {
        runCatching {
            emit(block())
        }.onSuccess {
            showSuccess()
        }.onFailure { e ->
            handleException(e, handleError, error, cancel)
        }
    }

    /**
     * Flow 启动器配置类
     */
    data class FlowConfig<T>(
        val onSuccess: (T) -> Unit,
        val onError: Error? = null,
        val onComplete: (() -> Unit)? = null,
        val timeoutMillis: Long? = null,
        val flowOnDispatcher: CoroutineDispatcher? = null
    )

    /**
     * 封装 Flow 的启动逻辑
     *
     * @param flowBlock 创建 Flow 的逻辑
     * @param config Flow 配置
     */
    protected fun <T> flowLaunch(
        flowBlock: suspend () -> Flow<T>,
        config: FlowConfig<T>
    ): Job = viewModelScope.launch {
        showLoading()
        try {
            val flow = flowBlock()
                .apply {
                    config.flowOnDispatcher?.let { flowOn(it) }
                }
                .onCompletion { config.onComplete?.invoke() }
                .catch { exception ->
                    val ex = exception as? Exception ?: Exception("Unknown error", exception)
                    config.onError?.invoke(ex)
                    showError(ex)
                }

            if (config.timeoutMillis != null) {
                withTimeout(config.timeoutMillis) {
                    flow.collect { result -> config.onSuccess(result) }
                }
            } else {
                flow.collect { result -> config.onSuccess(result) }
            }

            showSuccess()
        } catch (e: TimeoutCancellationException) {
            showError(e)
            config.onError?.invoke(e)
        } catch (e: Exception) {
            showError(e)
            config.onError?.invoke(e)
        }
    }

    /**
     * Flow 启动器的便捷方法
     */
    protected fun <T> flowLaunch(
        flowBlock: suspend () -> Flow<T>,
        onSuccess: (T) -> Unit,
        onError: Error? = null,
        onComplete: (() -> Unit)? = null,
        timeoutMillis: Long? = null,
        flowOnDispatcher: CoroutineDispatcher? = null
    ): Job = flowLaunch(
        flowBlock,
        FlowConfig(onSuccess, onError, onComplete, timeoutMillis, flowOnDispatcher)
    )

    /**
     * 异步任务执行器（挂起函数版本�?
     *
     * @param dispatcher 调度�?
     * @param block 执行�?
     */
    protected suspend fun <T> asyncSuspend(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        block: Block<T>
    ): T = withContext(dispatcher) {
        block()
    }

    /**
     * 取消协程任务
     *
     * @param job 协程job
     */
    protected fun cancelJob(job: Job?) {
        job?.takeIf { it.isActive && !it.isCompleted && !it.isCancelled }?.cancel()
    }

    /**
     * 统一的异常处理方�?
     *
     * @param throwable 异常
     * @param handleError 是否自动处理错误
     * @param error 错误回调
     * @param cancel 取消回调
     */
    private suspend fun handleException(
        throwable: Throwable,
        handleError: Boolean,
        error: Error?,
        cancel: Cancel?
    ) {
        when (throwable) {
            is CancellationException -> {
                cancel?.invoke(throwable)
            }
            else -> {
                val exception = throwable as? Exception ?: RuntimeException("Unknown error", throwable)
                if (handleError) {
                    handleCommonError(exception)
                }
                error?.invoke(exception)
            }
        }
    }

    /**
     * 通用错误处理
     *
     * @param e 异常
     */
    private fun handleCommonError(e: Exception) {
        val errorMessage = when (e) {
            is ConnectException -> "网络连接失败"
            is SocketTimeoutException -> "网络请求超时"
            is JsonParseException -> "数据解析错误"
            is NetworkOnMainThreadException -> "线程异常"
            is TimeoutCancellationException -> "请求超时"
            else -> e.message ?: "未知错误"
        }
        errorMessage.logE()
        showError()
    }

    // 状态管理方�?
    /**
     * 显示加载状�?
     */
    fun showLoading() = _status.postValue(ViewStatus.LOADING)

    /**
     * 显示成功状�?
     */
    fun showSuccess() = _status.postValue(ViewStatus.SUCCESS)

    /**
     * 显示错误状�?
     *
     * @param e 异常（可选）
     */
    fun showError(e: Exception? = null) {
        e?.let { handleCommonError(it) }
        _status.postValue(ViewStatus.ERROR)
    }

    /**
     * 重置状态为空闲
     */
    fun resetStatus() = _status.postValue(ViewStatus.IDLE)

    /**
     * 获取当前状�?
     */
    fun getCurrentStatus(): ViewStatus = _status.value ?: ViewStatus.IDLE

    // Coroutine 工具方法
    /**
     * 执行协程任务
     */
    fun <T> execute(
        scope: CoroutineScope = viewModelScope,
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        executeContext: CoroutineContext = Dispatchers.Main,
        block: suspend CoroutineScope.() -> T
    ): Coroutine<T> {
        return Coroutine.async(scope, context, start, executeContext, block)
    }

    /**
     * 执行懒加载协程任�?
     */
    fun <T> executeLazy(
        scope: CoroutineScope = viewModelScope,
        context: CoroutineContext = Dispatchers.IO,
        executeContext: CoroutineContext = Dispatchers.Main,
        block: suspend CoroutineScope.() -> T
    ): Coroutine<T> {
        return Coroutine.async(scope, context, CoroutineStart.LAZY, executeContext, block)
    }

    /**
     * 提交异步任务
     */
    fun <R> submit(
        scope: CoroutineScope = viewModelScope,
        context: CoroutineContext = Dispatchers.IO,
        block: suspend CoroutineScope.() -> Deferred<R>
    ): Coroutine<R> {
        return Coroutine.async(scope, context) { block().await() }
    }
}

/**
 * 视图状态枚�?
 */
enum class ViewStatus {
    /** 空闲状�?*/
    IDLE,
    /** 加载�?*/
    LOADING,
    /** 成功 */
    SUCCESS,
    /** 错误 */
    ERROR
}
