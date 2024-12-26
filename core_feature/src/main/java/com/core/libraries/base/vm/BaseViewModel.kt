package com.core.libraries.base.vm

import android.os.NetworkOnMainThreadException
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.core.libraries.base.ext.logE
import com.core.libraries.enums.ViewStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.coroutines.CoroutineContext

//可以在别名类指定类型，例如suspend CoroutineScope.() -> Unit -----但是此时的block不需要调用invoke 了，直接block（）
typealias Block<T> = suspend (CoroutineScope) -> T
typealias Error = suspend (e: Exception) -> Unit
typealias Cancel = suspend (e: Exception) -> Unit
typealias EmitBlock<T> = suspend LiveDataScope<T>.() -> T


open class BaseViewModel : ViewModel() {

    //封装页面状态的LiveData
    val viewStatus: MutableLiveData<Enum<ViewStatus>> = MutableLiveData()


    /**
     * 创建并执行协程
     * @param block 协程中执行
     * @param error 错误时执行
     * @param cancel 错误时执行
     * @param handleError 是否处理异常
     * @return Job
     */
    protected fun launch(
        block: Block<Unit>,
        error: Error? = null,
        cancel: Cancel? = null,
        handleError: Boolean = true,
        context: CoroutineContext = Dispatchers.Default
    ): Job = viewModelScope.launch(context) {
        runCatching {
            block(this)
        }.onSuccess {
            viewStatus.postValue(ViewStatus.SUCCESS)
        }.onFailure { e ->
            when (e) {
                is CancellationException -> {
                    cancel?.invoke(e)
                }
                else -> {
                    val exception = e as? Exception ?: RuntimeException("Unknown error", e)
                    if (handleError) {
                        onError(exception)
                    }
                    error?.invoke(exception)
                }
            }
        }
    }

    protected fun launchScopedCoroutine(
        block: Block<Unit>,
        success: Block<Unit>,
        error: Error? = null,
        cancel: Cancel? = null,
        handleError: Boolean = true,
        context: CoroutineContext = Dispatchers.Default
    ): Job = viewModelScope.launch(context) {
        runCatching {
            block(this)
        }.onSuccess {
            withContext(Dispatchers.Main) {
                success(this)
            }
            viewStatus.postValue(ViewStatus.SUCCESS)
        }.onFailure { e ->
            when (e) {
                is CancellationException -> {
                    cancel?.invoke(e)
                }

                else -> {
                    val exception = e as? Exception ?: RuntimeException("Unknown error", e)
                    if (handleError) {
                        onError(exception)
                    }
                    error?.invoke(exception)
                }
            }
        }
    }


    /**
     * @param dispatcher  设置线程，这里默认主线程是因为默认的方法通过suspend挂起有自身线程封闭机制，所以不需要创建多余的线程，预留字段，是为了给自己Jsoup框架子线程执行-防止崩溃
     * @param block 协程中执行
     * @return Deferred<T>
     */
    protected fun <T> async(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        block: Block<T>
    ): Deferred<T> = viewModelScope.async(dispatcher) { block.invoke(this) }


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
     * 省去每次创建liveData的烦恼，利用liveData的包装创建，直接传入block发送道对应的页面（此时用livedata的协程作用域，不需要用viewModelScope，用的是liveDataScope）
     *
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
            viewStatus.postValue(ViewStatus.SUCCESS)
        }.onFailure { e ->
            when (e) {
                is CancellationException -> {
                    cancel?.invoke(e)
                }

                else -> {
                    val exception = e as? Exception ?: RuntimeException("Unknown error", e)
                    if (handleError) {
                        onError(exception)
                    }
                    error?.invoke(exception)
                }
            }
        }
    }

    /**
     * 统一处理错误
     * @param e 异常
     */
    private fun onError(e: Exception) {
        when (e) {
            is ConnectException -> {
                "网络连接失败".logE()
                viewStatus.value = ViewStatus.ERROR
            }

            is SocketTimeoutException -> {
                "网络请求超时".logE()
                viewStatus.value = ViewStatus.ERROR
            }

            is NetworkOnMainThreadException -> {
                "线程异常".logE()
                viewStatus.value = ViewStatus.ERROR
            }

            else -> {
                e.message?.logE()
                viewStatus.value = ViewStatus.ERROR
            }
        }
    }
}