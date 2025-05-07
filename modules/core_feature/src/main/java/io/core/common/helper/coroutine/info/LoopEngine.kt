package io.core.common.helper.coroutine.info

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

/**
 * 循环处理器，用于周期性执行任务
 * @property config 循环处理器配置
 * @constructor 私有构造函数，通过Builder构建实例
 */
class LoopEngine private constructor(
    private val config: Config
) {
    private val job: AtomicReference<Job?> = AtomicReference(null)
    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val pauseTime = AtomicReference<Long>(0L)

    // 使用独立的作用域控制生命周期
    private val processorScope = CoroutineScope(
        SupervisorJob() + config.coroutineContext + CoroutineName("LoopProcessor")
    )

    /**
     * 循环处理器配置类
     * @property coroutineContext 协程上下文，默认为Dispatchers.IO
     * @property initialDelayMs 初始延迟时间(毫秒)
     * @property intervalMs 执行间隔时间(毫秒)
     * @property retryIntervalMs 错误重试间隔时间(毫秒)
     * @property onError 错误处理回调，返回Boolean决定是否继续执行
     * @property onStart 开始执行回调
     * @property onStop 停止执行回调
     * @property dispatcherForCallbacks 回调执行的协程上下文，默认为Dispatchers.Main
     */
    class Config {
        var coroutineContext: CoroutineContext = Dispatchers.IO
        var initialDelayMs: Long = 0
        var intervalMs: Long = 1_000
        var retryIntervalMs: Long = 500
        var onError: ((Throwable) -> Boolean)? = null
        var onStart: (() -> Unit)? = null
        var onStop: (() -> Unit)? = null
        var dispatcherForCallbacks: CoroutineContext = Dispatchers.Main
        lateinit var block: suspend () -> Unit
    }

    /**
     * 启动循环处理器
     */
    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            return
        }
        job.set(processorScope.launch {
            try {
                config.onStart?.let {
                    withContext(config.dispatcherForCallbacks) { it() }
                }

                delay(config.initialDelayMs)
                while (isActive) {
                    try {
                        if (isPaused.get()) {
                            delay(100) // 暂停时减少CPU使用
                            continue
                        }
                        config.block()
                        delay(config.intervalMs)
                    } catch (e: CancellationException) {
                        return@launch // 正常退出
                    } catch (e: Exception) {
                        val shouldContinue = config.onError?.let {
                            withContext(config.dispatcherForCallbacks) { it(e) }
                        } ?: true

                        if (!shouldContinue || !isActive) break
                        delay(config.retryIntervalMs)
                    }
                }
            } finally {
                withContext(NonCancellable){
                    withContext(config.dispatcherForCallbacks) {
                        config.onStop?.invoke()
                    }
                    isRunning.set(false)
                    isPaused.set(false)
                }

            }
        })
    }

    /**
     * 暂停循环处理器
     */
    fun pause() {
        if (isRunning.get() && !isPaused.get()) {
            isPaused.set(true)
            pauseTime.set(System.currentTimeMillis())
        }
    }

    /**
     * 继续循环处理器
     */
    fun resume() {
        if (isRunning.get() && isPaused.get()) {
            val pausedDuration = System.currentTimeMillis() - pauseTime.get()
            processorScope.launch {
                delay(pausedDuration.coerceAtLeast(0))
                isPaused.set(false)
            }
        }
    }

    /**
     * 停止循环处理器
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            job.get()?.cancel()
        }
    }

    /**
     * 获取当前任务是否正在运行
     * @return Boolean
     */
    fun isRunning(): Boolean = isRunning.get()

    /**
     * 获取当前是否处于暂停状态
     * @return Boolean
     */
    fun isPaused(): Boolean = isPaused.get()

    /**
     * LoopProcessor构建器
     */
    class Builder {
        private val config = Config()

        /** 设置协程上下文 */
        fun context(context: CoroutineContext) = apply { config.coroutineContext = context }

        /** 设置初始延迟时间(毫秒) */
        fun initialDelay(delayMs: Long) = apply { config.initialDelayMs = delayMs }

        /** 设置执行间隔时间(毫秒) */
        fun interval(intervalMs: Long) = apply { config.intervalMs = intervalMs }

        /** 设置错误重试间隔时间(毫秒) */
        fun retryInterval(intervalMs: Long) = apply { config.retryIntervalMs = intervalMs }

        /** 设置错误处理回调 */
        fun onError(handler: (Throwable) -> Boolean) = apply { config.onError = handler }

        /** 设置开始执行回调 */
        fun onStart(handler: () -> Unit) = apply { config.onStart = handler }

        /** 设置停止执行回调 */
        fun onStop(handler: () -> Unit) = apply { config.onStop = handler }

        /** 设置回调执行的协程上下文 */
        fun callbackDispatcher(dispatcher: CoroutineContext) = apply {
            config.dispatcherForCallbacks = dispatcher
        }

        // 支持两种任务定义方式
        /** 设置要执行的任务块 */
        fun task(block: suspend () -> Unit) = apply { config.block = block }

        /** 设置要执行的任务块(Java) */
        fun task(block: Runnable) = apply {
            config.block = { withContext(config.coroutineContext) { block.run() } }
        }

        /** 构建LoopProcessor实例 */
        fun build(): LoopEngine {
            return LoopEngine(config)
        }
    }
}