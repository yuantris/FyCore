package io.core.utils.extensions.cool

import io.core.utils.tools.ThreadUltra

// DSL 配置�?
class ThreadConfig<T> {
    internal var backgroundAction: (() -> T)? = null
    internal var successAction: (T.() -> Unit)? = null
    internal var errorAction: ((Throwable) -> Unit)? = null
    internal var completeAction: (() -> Unit)? = null

    fun background(block: () -> T) {
        backgroundAction = block
    }

    fun success(block: T.() -> Unit) {
        successAction = block
    }

    fun error(block: (Throwable) -> Unit) {
        errorAction = block
    }

    fun complete(block: () -> Unit) {
        completeAction = block
    }

}

fun <T> coolThread(block: ThreadConfig<T>.() -> Unit){
    val config = ThreadConfig<T>().apply(block)
    ThreadUltra.execute(object : ThreadUltra.Task<T>() {
        override fun doInBackground(): T {
            return config.backgroundAction?.invoke()
                ?: throw IllegalStateException("Background action not defined")
        }

        override fun onSuccess(result: T) {
            config.successAction?.invoke(result)
        }

        override fun onFail(errorType: ThreadUltra.ErrorType, ex: Throwable) {
            config.errorAction?.invoke(ex)
        }

        override fun onComplete() {
            config.completeAction?.invoke()
        }

    })
}