package io.core.common.helper

import io.core.common.util.extensions.cool.printOnDebug
import io.core.common.util.log.LogPure
import io.core.common.util.log.TAG
import kotlinx.coroutines.*
import java.util.function.Supplier
import kotlin.coroutines.CoroutineContext

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
inline fun <T> tryCatch(
    tryBlock: () -> T,
    catchBlock: (Throwable) -> Unit = { it.printOnDebug() },
    finallyBlock: () -> Unit = {}
): T? {
    return try {
        tryBlock()
    } catch (e: Exception) {
        catchBlock(e)
        null
    } finally {
        finallyBlock()
    }
}

inline fun <T> tryCatchWithDefault(
    default: T, // 提供默认返回值
    tryBlock: () -> T,
    catchBlock: (Throwable) -> Unit = { it.printStackTrace() },
    finallyBlock: () -> Unit = {}
): T {
    return try {
        tryBlock()
    } catch (e: Exception) {
        catchBlock(e)
        default
    } finally {
        finallyBlock()
    }
}

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
            catch: (Exception) -> Unit
        ) {
            try {
                block()
            } catch (e: Exception) {
                catch(e)
            }
        }

        inline fun <reified E : Throwable> execute(
            block: () -> Unit,
            catch: (E) -> Unit,
            noinline finally: (() -> Unit)? = null
        ) {
            try {
                block()
            } catch (e: Throwable) {
                if (e is E) {
                    catch(e)
                } else {
                    throw e
                }
            } finally {
                finally?.invoke()
            }
        }

        inline fun <T> executeWithReturn(block: () -> T): T? {
            return try {
                block()
            } catch (e: Exception) {
                LogPure.e(TAG, "An error occurred: " + e.message)
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
            LogPure.e(TAG, "An error occurred: " + e.message)
        }

        fun interface CheckedConsumer<T> {
            @Throws(Exception::class)
            fun accept(t: T)
        }
    }
}
