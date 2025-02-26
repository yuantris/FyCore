package com.core.fy.android.help

import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.tools.buildMainHandler
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

// 进度回调类型（默认在主线程执行）
typealias ProgressCallback = (taskId: String, percent: Int) -> Unit
typealias TimeoutCallback = (taskId: String) -> Unit

/**
 * 进度通知器，用于通知进度变化和超时
 */
object ProgressNotifier {
    // 主线程Handler，用于回调到UI线程
    private val mainHandler = buildMainHandler()

    // 自动清理配置（10分钟未更新的任务视为闲置）
    private const val MAX_IDLE_TIME_MS = 10 * 60 * 1000L

    // 默认线程池（用于异步通知监听器）
    // 使用单线程池（保持常驻）
    private val notificationExecutor = Executors.newSingleThreadExecutor()
    private val timeoutChecker = Executors.newSingleThreadScheduledExecutor().apply {
        scheduleAtFixedRate(::checkTimeoutsAndIdleTasks, 1, 1, TimeUnit.SECONDS)
    }

    // 注册默认超时监听（可选）
    var defaultTimeoutHandler: TimeoutCallback = { _ -> }

    // 使用弱引用存储监听器
    private val listeners = ConcurrentHashMap<String, WeakReference<ProgressCallback>>()
    private val tasks = ConcurrentHashMap<String, ProgressData>()

    // 注册进度监听（自动切换到主线程）
    fun register(listener: ProgressCallback): String {
        val id = UUID.randomUUID().toString()
        listeners[id] = WeakReference { taskId, percent ->
            mainHandler.post { listener(taskId, percent) }
        }
        return id
    }

    // 启动新任务（增加超时参数）
    fun startTask(
        totalSteps: Int,
        timeoutMillis: Long? = null,
        onTimeout: TimeoutCallback? = null
    ): String {
        require(totalSteps > 0) { "Total steps must be greater than 0" }
        val taskId = UUID.randomUUID().toString()
        tasks[taskId] = ProgressData(
            total = totalSteps,
            timeout = timeoutMillis?.takeIf { it > 0 },
            onTimeout = onTimeout
        ).apply { updateTimestamp() }
        return taskId
    }

    // 更新进度（增量）
    fun incrementProgress(taskId: String, step: Int = 1) {
        tasks[taskId]?.let { data ->
            synchronized(data) {
                val newCurrent = (data.current + step).coerceAtMost(data.total)
                if (newCurrent != data.current) {
                    data.current = newCurrent
                    data.updateTimestamp()
                    checkAndNotify(taskId, data)
                }
            }
        }
    }

    // 直接设置当前进度
    fun setCurrentProgress(taskId: String, current: Int) {
        tasks[taskId]?.let { data ->
            synchronized(data) {
                val newCurrent = current.coerceIn(0, data.total)
                if (newCurrent != data.current) {
                    data.current = newCurrent
                    data.updateTimestamp()
                    checkAndNotify(taskId, data)
                }
            }
        }
    }

    // 完成进度（自动补满）
    fun complete(taskId: String) {
        tasks[taskId]?.let { data ->
            synchronized(data) {
                data.current = data.total
                checkAndNotify(taskId, data)
                tasks.remove(taskId)
            }
        }
    }

    // 内部数据类（使用AtomicLong优化并发读取）
    private data class ProgressData(
        val total: Int,
        var current: Int = 0,
        var lastPercent: Int = -1,
        val timeout: Long?,
        val lastUpdate: AtomicLong = AtomicLong(currentTimeMillis),
        val onTimeout: TimeoutCallback?
    ) {
        val percent: Int
            get() = when {
                total == 0 -> 100
                else -> (current.toFloat() / total * 100).toInt().coerceIn(0, 100)
            }

        fun updateTimestamp() {
            lastUpdate.set(currentTimeMillis)
        }
    }

    // 检查并通知进度变化
    private fun checkAndNotify(taskId: String, data: ProgressData) {
        val newPercent = data.percent
        if (newPercent != data.lastPercent) {
            data.lastPercent = newPercent
            notifyListeners(taskId, newPercent)
        }
    }

    // 异步通知所有监听器（自动清理无效监听器）
    private fun notifyListeners(taskId: String, percent: Int) {
        notificationExecutor.execute {
            // 清理无效监听器
            listeners.values.removeAll { it.get() == null }

            // 通知有效监听器
            listeners.values.forEach { ref ->
                ref.get()?.invoke(taskId, percent)
            }
        }
    }

    // 合并超时检查和闲置任务清理
    private fun checkTimeoutsAndIdleTasks() {
        val now = currentTimeMillis
        tasks.forEach { (taskId, data) ->
            // 处理超时任务
            data.timeout?.let { timeout ->
                if (now - data.lastUpdate.get() > timeout) {
                    handleTaskRemoval(taskId, data, isTimeout = true)
                }
            }

            // 清理闲置任务（即使没有设置超时）
            if (now - data.lastUpdate.get() > MAX_IDLE_TIME_MS) {
                handleTaskRemoval(taskId, data, isTimeout = false)
            }
        }
    }

    private fun handleTaskRemoval(taskId: String, data: ProgressData, isTimeout: Boolean) {
        if (tasks.remove(taskId, data)) {
            if (isTimeout) {
                mainHandler.post {
                    data.onTimeout?.invoke(taskId)
                    defaultTimeoutHandler.invoke(taskId)
                }
            }
            notifyListeners(taskId, data.percent)
        }
    }
}