package com.core.libraries.base.vm

import android.os.NetworkOnMainThreadException
import androidx.lifecycle.*
import com.core.libraries.base.ext.logE
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.coroutines.CoroutineContext

typealias Block<T> = suspend CoroutineScope.() -> T
typealias Error = suspend (e: Exception) -> Unit
typealias Cancel = suspend (e: Exception) -> Unit
typealias EmitBlock<T> = suspend LiveDataScope<T>.() -> T

open class BaseViewModel : ViewModel() {

    val status: MutableLiveData<ViewStatus> = MutableLiveData()

    /**
     * 通用协程启动器
     */
    protected fun <T> launch(
        block: Block<T>,
        onError: Error? = null,
        onSuccess: ((T) -> Unit)? = null,
        onCancel: Cancel? = null,
        handleError: Boolean = true,
        context: CoroutineContext = Dispatchers.Default
    ): Job = viewModelScope.launch(context) {
        runCatching { block() }
            .onSuccess { result ->
                status.postValue(ViewStatus.SUCCESS)
                onSuccess?.invoke(result)
            }
            .onFailure { handleException(it, handleError, onError, onCancel) }
    }

    /**
     * LiveData 封装
     */
    fun <T> emit(
        block: EmitBlock<T>,
        onError: Error? = null,
        onCancel: Cancel? = null,
        handleError: Boolean = true
    ): LiveData<T> = liveData {
        runCatching { emit(block()) }
            .onSuccess { status.postValue(ViewStatus.SUCCESS) }
            .onFailure { handleException(it, handleError, onError, onCancel) }
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
                            status.postValue(ViewStatus.ERROR)
                        } ?: run {
                            // 如果 exception 不是 Exception 类型，处理其他错误情况
                            onError?.invoke(Exception("Unknown error"))
                            status.postValue(ViewStatus.ERROR)
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

                status.postValue(ViewStatus.SUCCESS)
            } catch (e: TimeoutCancellationException) {
                onError?.invoke(e)
                status.postValue(ViewStatus.ERROR)
            } catch (e: Exception) {
                onError?.invoke(e)
                status.postValue(ViewStatus.ERROR)
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
     * 取消协程任务
     */
    protected fun cancelJob(job: Job?) {
        job?.takeIf { it.isActive }?.cancel()
    }

    /**
     * 错误处理逻辑
     */
    private suspend fun handleException(
        throwable: Throwable,
        handleError: Boolean,
        onError: Error?,
        onCancel: Cancel?
    ) {
        when (throwable) {
            is CancellationException -> onCancel?.invoke(throwable)
            else -> {
                val exception = throwable as? Exception ?: RuntimeException("Unknown error", throwable)
                if (handleError) handleCommonError(exception)
                onError?.invoke(exception)
            }
        }
    }

    /**
     * 通用错误处理
     */
    private fun handleCommonError(e: Exception) {
        when (e) {
            is ConnectException -> "网络连接失败".logE()
            is SocketTimeoutException -> "网络请求超时".logE()
            is NetworkOnMainThreadException -> "线程异常".logE()
            else -> e.message?.logE()
        }
        status.postValue(ViewStatus.ERROR)
    }
}

enum class ViewStatus {
    SUCCESS,
    ERROR
}
