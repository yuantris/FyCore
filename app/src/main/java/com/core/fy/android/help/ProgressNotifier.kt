package com.core.fy.android.help

import android.os.Handler
import android.os.Looper
import androidx.annotation.WorkerThread
import androidx.core.util.Pools
import com.core.fy.android.help.ProgressNotifier.ProgressCallback
import com.core.fy.android.help.ProgressNotifier.TimeoutCallback
import io.core.common.util.extensions.cool.withMain
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.tools.buildMainHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 进度通知器，用于通知进度变化和超时
 */
object ProgressNotifier {
    // 主线程Handler，用于回调到UI线程
    private val mainHandler = Handler(Looper.getMainLooper())

    // 自动清理配置（10分钟未更新的任务视为闲置）
    private const val MAX_IDLE_TIME_MS = 10 * 60 * 1000L

    // 默认线程池（用于异步通知监听器）
    private val notificationScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName("notificationScope"))
    private val timeoutScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName("timeoutScope"))

    // 添加对象池
    private val dataPool = Pools.SynchronizedPool<ProgressData>(10)

    init {
        timeoutScope.launch {
            while (isActive) {
                checkTimeoutsAndIdleTasks()
                delay(1000)
            }
        }
    }

    /**
     * 进度回调接口（兼容Java调用）
     */
    fun interface ProgressCallback {
        fun onProgress(taskId: String, percent: Int)
    }

    /**
     * 超时回调接口（兼容Java调用）
     */
    fun interface TimeoutCallback {
        @WorkerThread
        fun onTimeout(taskId: String)
    }

    // 注册默认超时监听（可选）
    var defaultTimeoutHandler = TimeoutCallback {
        // 默认处理：取消任务
    }

    // 使用弱引用存储监听器
    private val listeners = ConcurrentHashMap<String, WeakReference<ProgressCallback>>()
    private val tasks = ConcurrentHashMap<String, ProgressData>()

    // 注册进度监听（自动切换到主线程）
    @JvmStatic
    fun register(listener: ProgressCallback): String {
        val id = UUID.randomUUID().toString()
        listeners[id] = WeakReference(ProgressCallback { taskId, percent ->
            listener.onProgress(
                taskId,
                percent
            )
        })
        return id
    }

    // 启动新任务（增加超时参数）
    @JvmStatic
    fun startTask(
        totalSteps: Int,
        timeoutMillis: Long? = null,
        onTimeout: TimeoutCallback? = null
    ): String {
        require(totalSteps > 0) { "Total steps must be greater than 0" }
        val taskId = UUID.randomUUID().toString()
        val data = obtainProgressData(totalSteps, timeoutMillis?.takeIf { it > 0 }, onTimeout)
        data.updateTimestamp()
        tasks[taskId] = data
        return taskId
    }

    // 更新进度（增量）
    @JvmStatic
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
    @JvmStatic
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
    @JvmStatic
    fun complete(taskId: String) {
        tasks[taskId]?.let { data ->
            synchronized(data) {
                data.current = data.total
                checkAndNotify(taskId, data)
                tasks.remove(taskId)
                recycleProgressData(data)
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

        // 对象池重置方法
        fun reset() {
            current = 0
            lastPercent = -1
            lastUpdate.set(currentTimeMillis)
        }
    }

    // 从对象池获取或创建ProgressData
    private fun obtainProgressData(
        total: Int,
        timeout: Long?,
        onTimeout: TimeoutCallback?
    ): ProgressData {
        val data = dataPool.acquire()
        return if (data != null && data.total == total && data.timeout == timeout && data.onTimeout == onTimeout) {
            data.reset()
            data
        } else {
            ProgressData(total, 0, -1, timeout, AtomicLong(currentTimeMillis), onTimeout)
        }
    }

    // 释放ProgressData到对象池
    private fun recycleProgressData(data: ProgressData) {
        if (!dataPool.release(data)) {
            // 对象池已满，丢弃对象
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
        notificationScope.launch {
            // 使用缓存有效监听器
            val validListeners = listeners.values
                .mapNotNull { it.get() }
                .takeIf { it.isNotEmpty() } ?: return@launch

            validListeners.forEach { callback ->
                withContext(Dispatchers.Main) {
                    callback.onProgress(taskId, percent)
                }
            }
        }
    }

    // 合并超时检查和闲置任务清理
    private fun checkTimeoutsAndIdleTasks() {
        val now = currentTimeMillis
        val taskSnapshot = tasks.toMap()
        taskSnapshot.forEach { (taskId, data) ->
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
                data.onTimeout?.onTimeout(taskId)
                defaultTimeoutHandler.onTimeout(taskId)
            }
            notifyListeners(taskId, data.percent)
        }
    }
}