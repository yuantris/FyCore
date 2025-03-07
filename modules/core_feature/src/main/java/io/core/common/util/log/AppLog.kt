@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package io.core.common.util.log

import android.util.Log
import io.core.appCtx
import io.core.common.util.extensions.cool.timeFormat
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.tools.toastOnUI
import io.core.constant.TimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

/**
 * 增强型日志工具（支持埋点统计）
 *
 * 主要功能：
 * - 多级别日志记录
 * - 异步批量处理
 * - 内存映射文件导出
 * - 用户行为埋点
 * - 性能监控
 */
object AppLog {
    // region 日志级别常量
    private const val VERBOSE = 0
    private const val DEBUG = 1
    private const val INFO = 2
    private const val WARN = 3
    private const val ERROR = 4

    private const val DEFAULT_TAG = "AppLog"
    // endregion

    // region 配置参数
    var maxStoreCount = 200               // 内存最大存储条数
    var enableLogcat = true               // 是否输出到Logcat
    var bufferSize: Int = 100              // 内存缓冲区条数
    var asyncQueueCapacity: Int = 1000    // 异步队列容量
    var fileEncoding: String = "UTF-8"    // 文件编码格式
    var autoFlushInterval: Long = 5000    // 自动刷新间隔（ms）
    // endregion

    // region 数据结构
    private data class LogEntry(
        val timestamp: String,
        val level: Int,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
        val threadId: Long,
        val threadName: String
    )

    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val buffer = ArrayDeque<LogEntry>()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val flushCounter = AtomicLong(0)
    // endregion

    init {
        startAutoFlush()
    }

    /*---------------- 核心日志方法 ----------------*/

    @JvmStatic
    @JvmOverloads
    fun verbose(message: String, tag: String = getCallerTag(), toast: Boolean = false) {
        logInternal(VERBOSE, tag, message, null, toast)
    }

    @JvmStatic
    @JvmOverloads
    fun debug(message: String, tag: String = getCallerTag()) {
        logInternal(DEBUG, tag, message)
    }

    @JvmStatic
    @JvmOverloads
    fun info(message: String, tag: String = getCallerTag()) {
        logInternal(INFO, tag, message)
    }

    @JvmStatic
    @JvmOverloads
    fun warn(message: String, tag: String = getCallerTag(), throwable: Throwable? = null) {
        logInternal(WARN, tag, message, throwable)
    }

    @JvmStatic
    @JvmOverloads
    fun error(message: String, tag: String = getCallerTag(), throwable: Throwable? = null) {
        logInternal(ERROR, tag, message, throwable)
    }

    /*---------------- 埋点专用方法 ----------------*/

    @JvmStatic
    fun trackEvent(eventName: String, params: Map<String, Any?> = emptyMap()) {
        val safeParams = params.mapValues { (_, v) ->
            v?.toString()?.replace(Regex("[\n|]"), "_") ?: "null"
        }
        val message = buildString {
            append("EVENT:$eventName")
            safeParams.forEach { (k, v) -> append("|$k=$v") }
        }
        logInternal(INFO, "TRACKING", message)
    }

    @JvmStatic
    fun <T> trackDuration(eventName: String, block: () -> T): T {
        val start = System.currentTimeMillis()
        val result = block()
        val cost = System.currentTimeMillis() - start
        logInternal(INFO, "PERF", "$eventName cost:${cost}ms")
        return result
    }

    /*---------------- 日志管理方法 ----------------*/

    @JvmStatic
    fun exportToFile(file: File, append: Boolean = false, callback: ((Boolean, String?) -> Unit)? = null) {
        scope.launch {
            try {
                flushBuffer()
                val success = withContext(Dispatchers.IO) {
                    RandomAccessFile(file, "rw").use { raf ->
                        val channel = raf.channel
                        val buffer = channel.map(
                            FileChannel.MapMode.READ_WRITE,
                            if (append) raf.length() else 0,
                            calculateExportSize()
                        )
                        logQueue.forEach { writeEntryToBuffer(buffer, it) }
                        true
                    }
                }
                callback?.invoke(success, null)
            } catch (e: Exception) {
                callback?.invoke(false, parseExportError(e))
            }
        }
    }


    @JvmStatic
    fun clear() {
        logQueue.clear()
        buffer.clear()
    }

    /*---------------- 内部实现 ----------------*/

    private fun logInternal(
        level: Int,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        toast: Boolean = false
    ) {
        if (toast) appCtx.toastOnUI(message)

        val entry = LogEntry(
            timestamp = currentTimeMillis.timeFormat(TimeFormat.LOG_TIMESTAMP),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
            threadId = Thread.currentThread().id,
            threadName = Thread.currentThread().name
        )

        // 异步队列处理
        if (logQueue.size >= asyncQueueCapacity) {
            logQueue.poll()
        }
        logQueue.offer(entry)

        // 实时输出到Logcat
        if (enableLogcat) {
            outputToLogcat(entry)
        }
    }

    private fun startAutoFlush() {
        scope.launch {
            while (true) {
                delay(autoFlushInterval)
                if (buffer.isNotEmpty()) flushBuffer()
            }
        }
    }

    private fun flushBuffer() {
        synchronized(buffer) {
            logQueue.addAll(buffer)
            buffer.clear()
            while (logQueue.size > maxStoreCount) {
                logQueue.poll()
            }
            flushCounter.incrementAndGet()
        }
    }

    private fun outputToLogcat(entry: LogEntry) {
        try {
            when (entry.level) {
                VERBOSE -> Log.v(entry.tag, entry.message, entry.throwable)
                DEBUG -> Log.d(entry.tag, entry.message, entry.throwable)
                INFO -> Log.i(entry.tag, entry.message, entry.throwable)
                WARN -> Log.w(entry.tag, entry.message, entry.throwable)
                ERROR -> Log.e(entry.tag, entry.message, entry.throwable)
            }
        } catch (e: Exception) {
            Log.e(DEFAULT_TAG, "Logcat输出失败: ${e.message}")
        }
    }

    private fun calculateExportSize(): Long {
        return logQueue.sumOf { buildLogString(it).toByteArray(Charset.forName(fileEncoding)).size.toLong() }
    }

    private fun writeEntryToBuffer(buffer: MappedByteBuffer, entry: LogEntry) {
        val logStr = buildLogString(entry)
        buffer.put(logStr.toByteArray(Charset.forName(fileEncoding)))
    }

    private fun buildLogString(entry: LogEntry): String {
        return StringBuilder().apply {
            append(entry.timestamp)
            append(" ${levelToString(entry.level)}/${entry.tag}:")
            append(" [${entry.threadName}#${entry.threadId}]")
            append(" ${entry.message}")
            entry.throwable?.let {
                append("\n${Log.getStackTraceString(it)}")
            }
            append("\n")
        }.toString()
    }

    private fun parseExportError(e: Exception): String {
        return when (e) {
            is SecurityException -> "缺少存储权限 (${e.message})"
            is IOException -> "文件写入失败 (${e.message})"
            else -> "未知错误: ${e.javaClass.simpleName} (${e.message})"
        }
    }

    private fun levelToString(level: Int) = when (level) {
        VERBOSE -> "V"
        DEBUG -> "D"
        INFO -> "I"
        WARN -> "W"
        else -> "E"
    }

    private fun getCallerTag(): String {
        return Throwable().stackTrace
            .firstOrNull { it.className != this::class.java.name }
            ?.className
            ?.substringAfterLast('.')
            ?: DEFAULT_TAG
    }
}