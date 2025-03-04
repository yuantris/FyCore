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
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.Charset
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 增强版应用日志管理工具
 *
 * 新增特性：
 * 1. 异步批量写入（提升性能）
 * 2. 线程信息记录
 * 3. 智能缓冲区管理
 * 4. 增强错误处理
 */
object AppLog {
    // region 日志级别常量
    const val VERBOSE = 0
    const val DEBUG = 1
    const val INFO = 2
    const val WARN = 3
    const val ERROR = 4

    private const val DEFAULT_TAG = "AppLog"
    // endregion

    // region 配置参数
    var maxStoreCount = 200               // 内存最大存储条数
    var enableLogcat = true               // 是否输出到Android Logcat
    var defaultTag: String = DEFAULT_TAG  // 默认日志标签
    var bufferSize: Int = 50              // 日志批量写入缓冲区
    var fileEncoding: String = "UTF-8"    // 文件编码格式
    var autoFlushInterval: Long = 5000    // 自动刷新间隔（毫秒）
    // endregion

    // region 数据结构
    data class LogEntry(
        val timestamp: String,
        val level: Int,
        val tag: String = defaultTag,
        val message: String,
        val throwable: Throwable?,
        val threadId: Long = Thread.currentThread().id,
        val threadName: String = Thread.currentThread().name
    )

    private val logEntries = CopyOnWriteArrayList<LogEntry>()
    private val buffer = ArrayDeque<LogEntry>()
    private val outputChannels = CopyOnWriteArrayList<(LogEntry) -> Unit>()
    private val lock = ReentrantReadWriteLock()
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    // endregion

    init {
        startAutoFlush()
        addDefaultLogcatChannel()
    }

    private fun addDefaultLogcatChannel() {
        if (enableLogcat) {
            outputChannels.add { entry ->
                try {
                    when (entry.level) {
                        VERBOSE -> Log.v(entry.tag, entry.message, entry.throwable)
                        DEBUG -> Log.d(entry.tag, entry.message, entry.throwable)
                        INFO -> Log.i(entry.tag, entry.message, entry.throwable)
                        WARN -> Log.w(entry.tag, entry.message, entry.throwable)
                        ERROR -> Log.e(entry.tag, entry.message, entry.throwable)
                    }
                } catch (e: Exception) {
                    Log.e(DEFAULT_TAG, "Logcat output failed: ${e.message}")
                }
            }
        }
    }

    private fun log(
        level: Int = INFO,
        tag: String = defaultTag,
        message: String,
        throwable: Throwable? = null,
        toast: Boolean = false
    ) {
        if (toast && message.isNotBlank()) appCtx.toastOnUI(message)

        val entry = LogEntry(
            timestamp = currentTimeMillis.timeFormat(TimeFormat.LOG_TIMESTAMP),
            level = level.coerceIn(VERBOSE, ERROR),
            tag = tag,
            message = message,
            throwable = throwable
        )

        // 新增实时输出（不经过缓冲区）
        if (enableLogcat) {
            outputChannels.forEach { it(entry) }
        }

        buffer.add(entry)
        if (buffer.size >= bufferSize) {
            flushBuffer()
        }
    }

    // region 公共API
    fun addOutputChannel(channel: (LogEntry) -> Unit) {
        outputChannels.add(channel)
    }

    fun getFiltered(predicate: (LogEntry) -> Boolean): List<LogEntry> {
        return lock.read { logEntries.filter(predicate) }
    }

    fun exportToFileAsync(
        file: File,
        append: Boolean = false,
        callback: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                flushBuffer()
                val result = exportToFileSync(file, append)
                withContext(Dispatchers.Main) {
                    callback(result, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false, parseExportError(e))
                }
            }
        }
    }

    fun clear() = lock.write {
        logEntries.clear()
        buffer.clear()
    }
    // endregion

    // region 私有方法
    private fun startAutoFlush() {
        scope.launch {
            while (true) {
                delay(autoFlushInterval)
                if (buffer.isNotEmpty()) flushBuffer()
            }
        }
    }

    private fun flushBuffer() = lock.write {
        logEntries.addAll(0, buffer)
        while (logEntries.size > maxStoreCount) {
            logEntries.removeAt(logEntries.lastIndex)
        }
        buffer.clear()
    }

    private fun exportToFileSync(file: File, append: Boolean): Boolean {
        return PrintWriter(
            OutputStreamWriter(
                FileOutputStream(file, append),
                Charset.forName(fileEncoding)
            )
        ).use { writer ->
            logEntries.forEach { entry ->
                writer.println(buildLogString(entry))
            }
            true
        }
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
    // endregion

    // region 快捷方法
    @JvmStatic
    @JvmOverloads
    fun verbose(message: String, tag: String = defaultTag) =
        log(VERBOSE, tag, message, toast = true)

    @JvmStatic
    @JvmOverloads
    fun debug(message: String, tag: String = defaultTag) =
        log(DEBUG, tag, message)

    @JvmStatic
    @JvmOverloads
    fun info(message: String, tag: String = defaultTag) =
        log(INFO, tag, message)

    @JvmStatic
    @JvmOverloads
    fun warn(message: String, tag: String = defaultTag, throwable: Throwable? = null) =
        log(WARN, tag, message, throwable)

    @JvmStatic
    @JvmOverloads
    fun error(message: String, tag: String = defaultTag, throwable: Throwable? = null) =
        log(ERROR, tag, message, throwable)

    @JvmStatic
    @JvmOverloads
    fun put(
        message: Any,
        tag: String = defaultTag,
        toast: Boolean = false,
        throwable: Throwable? = null
    ) =
        log(INFO, tag, message.toString(), throwable, toast)
    // endregion
}
