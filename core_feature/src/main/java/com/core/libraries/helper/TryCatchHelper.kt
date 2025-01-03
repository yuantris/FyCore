package com.core.libraries.helper

import com.core.libraries.base.ext.TAG
import com.core.libraries.util.LogUtils
import java.util.function.Supplier
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2024/12/31 9:14
 * @description
 * @author Yuan
 */
class TryCatchHelper {

    companion object {
        fun execute(block: () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                handleException(e)
            }
        }

        fun execute(
            block: () -> Unit,
            catchBlock: (Exception) -> Unit
        ) {
            try {
                block()
            } catch (e: Exception) {
                catchBlock(e)
            }
        }

        inline fun <reified E : Throwable> execute(
            block: () -> Unit,
            catchBlock: (E) -> Unit,
            noinline finallyBlock: (() -> Unit)? = null
        ) {
            try {
                block()
            } catch (e: Throwable) {
                if (e is E) {
                    catchBlock(e)
                } else {
                    throw e
                }
            } finally {
                finallyBlock?.invoke()
            }
        }

        inline fun <T> executeWithReturn(block: () -> T): T? {
            return try {
                block()
            } catch (e: Exception) {
                LogUtils.logE(TAG, "An error occurred: " + e.message)
                null
            }
        }

        fun <T : AutoCloseable?> executeWithResources(
            resourceSupplier: Supplier<T>,
            consumer: CheckedConsumer<T>
        ) {
            try {
                resourceSupplier.get().use { resource ->
                    consumer.accept(resource)
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }

        @OptIn(DelicateCoroutinesApi::class)
        fun executeInCoroutine(
            context: CoroutineContext = Dispatchers.IO,
            block: suspend CoroutineScope.() -> Unit,
            onError: (Exception) -> Unit = { handleException(it) }
        ) {
            GlobalScope.launch(context) {
                try {
                    block()
                } catch (e: Exception) {
                    onError(e)
                }
            }
        }

        @OptIn(DelicateCoroutinesApi::class)
        fun executeWithReturnInCoroutine(
            context: CoroutineContext = Dispatchers.IO,
            block: suspend CoroutineScope.() -> Any?,
            onError: (Exception) -> Unit = { handleException(it) }
        ): Deferred<Any?> {
            return GlobalScope.async(context) {
                try {
                    block()
                } catch (e: Exception) {
                    onError(e)
                    null
                }
            }
        }

        private fun handleException(e: Exception) {
            LogUtils.logE(TAG, "An error occurred: " + e.message)
        }

        fun interface CheckedConsumer<T> {
            @Throws(Exception::class)
            fun accept(t: T)
        }
    }
}
