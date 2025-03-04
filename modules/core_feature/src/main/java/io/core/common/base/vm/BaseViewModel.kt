package io.core.common.base.vm

import android.os.NetworkOnMainThreadException
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import io.core.common.helper.coroutine.Coroutine
import io.core.common.util.log.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.coroutines.CoroutineContext

typealias Block<T> = suspend CoroutineScope.() -> T
typealias Error = suspend (e: Exception) -> Unit
typealias Cancel = suspend (e: Exception) -> Unit
typealias EmitBlock<T> = suspend LiveDataScope<T>.() -> T

open class BaseViewModel : ViewModel() {

    private val _status = MutableLiveData<ViewStatus>()
    val viewState: LiveData<ViewStatus> = _status

    /**
     * 通用协程启动器
     */
    protected fun launch(
        error: Error? = null,
        cancel: Cancel? = null,
        handleError: Boolean = true,
        context: CoroutineContext = Dispatchers.Default,
        block: Block<Unit>
    ): Job = viewModelScope.launch(context) {
        setLoading()
        runCatching {
            block(this)
        }.onSuccess {
            setSuccess()
        }.onFailure { e ->
            when (e) {
                is CancellationException -> {
                    cancel?.invoke(e)
                }

                else -> {
                    val exception = e as? Exception ?: RuntimeException("Unknown error", e)
                    if (handleError) setError(exception)
                    error?.invoke(exception)
                }
            }
        }
    }

    protected fun <T> launchScopedCoroutine(
        block: Block<T>,
        success: (CoroutineScope, T) -> Unit,
        error: Error? = null,
        cancel: Cancel? = null,
        handleError: Boolean = true,
        context: CoroutineContext = Dispatchers.Default
    ): Job = viewModelScope.launch(context) {
        runCatching {
            block(this)
        }.onSuccess { result ->
            withContext(Dispatchers.Main) {
                success(this, result)
            }
            _status.postValue(ViewStatus.SUCCESS)
        }.onFailure { e ->
            when (e) {
                is CancellationException -> {
                    cancel?.invoke(e)
                }

                else -> {
                    val exception = e as? Exception ?: RuntimeException("Unknown error", e)
                    if (handleError) {
                        handleCommonError(exception)
                    }
                    error?.invoke(exception)
                }
            }
        }
    }

    /**
     * LiveData 封装
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
            _status.postValue(ViewStatus.SUCCESS)
        }.onFailure { e ->
            when (e) {
                is CancellationException -> {
                    cancel?.invoke(e)
                }

                else -> {
                    val exception = e as? Exception ?: RuntimeException("Unknown error", e)
                    if (handleError) {
                        handleCommonError(exception)
                    }
                    error?.invoke(exception)
                }
            }
        }
    }


    /**
     * 封装 Flow 的启动逻辑
     *
     * @param flowBlock 创建 Flow 的逻辑
     * @param onSuccess 成功时的回调，接收 Flow 的结果
     * @param onError 失败时的回调
     * @param onComplete Flow 完成时的回调
     * @param timeoutMillis 超时时间（毫秒），默认不限时
     * @param flowOnDispatcher 切换 Flow 的执行线程，默认不切换
     */
    protected fun <T> flowLaunch(
        flowBlock: suspend () -> Flow<T>,
        onSuccess: (T) -> Unit,
        onError: Error? = null,
        onComplete: (() -> Unit)? = null,
        timeoutMillis: Long? = null,
        flowOnDispatcher: CoroutineDispatcher? = null
    ): Job {
        return viewModelScope.launch {
            setLoading()
            try {
                val flow = flowBlock()
                    .apply {
                        if (flowOnDispatcher != null) {
                            flowOn(flowOnDispatcher)
                        }
                    }
                    .onCompletion {
                        onComplete?.invoke()
                    }
                    .catch { exception ->
                        (exception as? Exception)?.let {
                            onError?.invoke(it)
                            _status.postValue(ViewStatus.ERROR)
                        } ?: run {
                            // 如果 exception 不是 Exception 类型，处理其他错误情况
                            onError?.invoke(Exception("Unknown error"))
                            _status.postValue(ViewStatus.ERROR)
                        }
                    }

                if (timeoutMillis != null) {
                    // 带超时的 collect
                    withTimeout(timeoutMillis) {
                        flow.collect { result -> onSuccess(result) }
                    }
                } else {
                    // 普通 collect
                    flow.collect { result -> onSuccess(result) }
                }

                setSuccess()
            } catch (e: TimeoutCancellationException) {
                setError(e)
                onError?.invoke(e)
            } catch (e: Exception) {
                setError(e)
                onError?.invoke(e)
            }
        }
    }


    /**
     * 异步任务
     */
    protected fun <T> async(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        block: Block<T>
    ): T = runBlocking {
        viewModelScope.async(dispatcher) { block() }.await()
    }

    /**
     * 取消协程
     * @param job 协程job
     */
    protected fun cancelJob(job: Job?) {
        if (job != null && job.isActive && !job.isCompleted && !job.isCancelled) {
            job.cancel()
        }
    }

    /**
     * 通用错误处理
     */
    private fun handleCommonError(e: Exception) {
        when (e) {
            is ConnectException -> "网络连接失败".logE()
            is SocketTimeoutException -> "网络请求超时".logE()
            is JsonParseException -> "数据解析错误".logE()
            is NetworkOnMainThreadException -> "线程异常".logE()
            else -> e.message?.logE()
        }
        _status.postValue(ViewStatus.ERROR)
    }

    // 新增快捷状态设置方法
    protected fun setLoading() = _status.postValue(ViewStatus.LOADING)
    protected fun setSuccess() = _status.postValue(ViewStatus.SUCCESS)
    protected fun setError(e: Exception? = null) {
        e?.let { handleCommonError(it) }
        _status.postValue(ViewStatus.ERROR)
    }

    fun <T> execute(
        scope: CoroutineScope = viewModelScope,
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        executeContext: CoroutineContext = Dispatchers.Main,
        block: suspend CoroutineScope.() -> T
    ): Coroutine<T> {
        return Coroutine.async(scope, context, start, executeContext, block)
    }

    fun <T> executeLazy(
        scope: CoroutineScope = viewModelScope,
        context: CoroutineContext = Dispatchers.IO,
        executeContext: CoroutineContext = Dispatchers.Main,
        block: suspend CoroutineScope.() -> T
    ): Coroutine<T> {
        return Coroutine.async(scope, context, CoroutineStart.LAZY, executeContext, block)
    }

    fun <R> submit(
        scope: CoroutineScope = viewModelScope,
        context: CoroutineContext = Dispatchers.IO,
        block: suspend CoroutineScope.() -> Deferred<R>
    ): Coroutine<R> {
        return Coroutine.async(scope, context) { block().await() }
    }
}

enum class ViewStatus {
    LOADING,
    SUCCESS,
    ERROR
}
