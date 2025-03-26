package io.core.common.util.log.bury

import android.util.Log
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import io.core.Android
import io.core.BuildConfig
import io.core.appCtx
import io.core.common.util.extensions.ui.appVersionCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean


/**
 * 应用程序日志工具，支持控制台输出和文件存储
 *
 * 功能特性：
 * - 多级别日志输出（DEBUG/INFO/WARN/ERROR）
 * - 线程安全的异步日志处理
 * - 存储策略配置（优先外部存储/内部存储）
 * - 日志文件上传支持
 *
 * 使用示例：
 * ```
 * // 初始化配置（可选）
 * AppLog.initialize(AppLog.LogConfig(
 *     maxFileCount = 7,
 *     releasePrintLevel = LogLevel.INFO
 * ))
 *
 * // 记录日志
 * AppLog.debug("TAG", "Debug message")
 * AppLog.error("Network", "Request failed", exception)
 *
 * // 获取日志文件
 * val logs = AppLog.getLogFiles()
 *
 * // 上传日志
 * viewModelScope.launch {
 *     AppLog.uploadLogFiles { files ->
 *         // 实现上传逻辑
 *     }
 * }
 * ```
 */
object AppLog {
    private const val DEFAULT_MAX_QUEUE_SIZE = 5000
    private const val BUFFER_FLUSH_INTERVAL = 2000L
    private const val MAX_BATCH_SIZE = 500

    data class LogConfig(
        var maxFileCount: Int = 7,
        var dateFormat: String = "yyyy-MM-dd HH:mm:ss.SSS",
        var fileDateFormat: String = "yyyy-MM-dd",
        @LogLevel var releasePrintLevel: Int = LogLevel.ERROR,
        var storageStrategy: StorageStrategy = StorageStrategy.EXTERNAL_FIRST,
        var enableThreadInfo: Boolean = true
    )

    enum class StorageStrategy {
        EXTERNAL_ONLY,
        INTERNAL_ONLY,
        EXTERNAL_FIRST
    }

    private val config = LogConfig()
    private val isDebugMode = Android.debug
    private val logBuffer = LinkedBlockingQueue<String>(DEFAULT_MAX_QUEUE_SIZE)
    private val logSignalChannel = Channel<Unit>(Channel.CONFLATED)
    private var logJob: Job? = null
    private val mutex = Mutex()
    private val isInitialized = AtomicBoolean(false)
    private var currentLogWriter: BufferedWriter? = null

    // region 关键修复1：分离目录和文件路径
    private var logDirectory: File? = null  // 明确存储日志目录
    private var currentLogFile: File? = null // 始终指向当前日志文件

    private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat(config.dateFormat, Locale.getDefault())
    }

    fun initialize(customConfig: LogConfig? = null) {
        if (!isInitialized.compareAndSet(false, true)) return

        customConfig?.let { config.applyFrom(it) }
        determineStoragePath()
        startLogConsumer()
        writeLogStart()
    }

    // region Public API
    @JvmStatic
    fun debug(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)

    @JvmStatic
    fun info(tag: String, message: String) = log(LogLevel.INFO, tag, message)

    @JvmStatic
    fun warning(tag: String, message: String, e: Throwable? = null) =
        log(LogLevel.WARN, tag, message, e)

    @JvmStatic
    fun error(tag: String, message: String, e: Throwable? = null) =
        log(LogLevel.ERROR, tag, message, e)

    @JvmStatic
    fun getLogFiles(): List<File> {
        val logDir = logDirectory ?: return emptyList()
        return logDir.listFiles { file ->
            file.isFile && file.name.startsWith("app_") && file.extension == "log"
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    suspend fun uploadLogFiles(uploader: suspend (List<File>) -> Unit) {
        mutex.withLock {
            flushAndClose()
            uploader(getLogFiles())
        }
    }

    suspend fun updateConfig(newConfig: LogConfig) {
        mutex.withLock {
            config.applyFrom(newConfig)
            determineStoragePath()
        }
    }

    @JvmStatic
    fun release() {
        logJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            logJob?.join()
            flushAndClose()
        }
    }
    // endregion

    // region Core Implementation
    private fun log(level: Int, tag: String, message: String, e: Throwable? = null) {
        val threadInfo = if (config.enableThreadInfo) {
            "${Thread.currentThread().name}_${Thread.currentThread().id}"
        } else ""
        val stackTrace = e?.stackTraceToString()?.prependIndent() ?: ""
        val fullMessage = buildLogEntry(level, threadInfo, tag, "$message$stackTrace")

        // 添加日志到缓冲队列
        synchronized(logBuffer) {
            if (logBuffer.size >= DEFAULT_MAX_QUEUE_SIZE) {
                logBuffer.poll() // 淘汰最旧日志
            }
            logBuffer.offer(fullMessage)
        }

        // 发送非阻塞信号（CONFLATED策略自动处理多余信号）
        logSignalChannel.trySend(Unit)

        if (shouldPrint(level)) {
            printToConsole(level, tag, message, e)
        }

    }

    private fun printToConsole(level: Int, tag: String, message: String, e: Throwable?) {
        if (isDebugMode) {
            when (level) {
                LogLevel.DEBUG -> Log.d(tag, message, e)
                LogLevel.INFO -> Log.i(tag, message, e)
                LogLevel.WARN -> Log.w(tag, message, e)
                LogLevel.ERROR -> Log.e(tag, message, e)
            }
        }
    }

    private fun shouldPrint(level: Int): Boolean {
        return isDebugMode || level >= config.releasePrintLevel
    }

    private fun buildLogEntry(
        level: Int,
        threadInfo: String,
        tag: String,
        message: String
    ): String {
        val levelString = when (level) {
            LogLevel.DEBUG -> "DEBUG"
            LogLevel.INFO -> "INFO"
            LogLevel.WARN -> "WARN"
            LogLevel.ERROR -> "ERROR"
            else -> "VERBOSE"
        }
        return buildString {
            append(dateFormat.get()?.format(Date()))
            append(" | ")
            append(levelString.padEnd(5))
            append(" | [")
            append(threadInfo.ifEmpty { "-" })
            append("|")
            append(tag)
            append("] | ")
            append(message)
            append("\n")
        }
    }

    private fun writeLogStart() {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                try {
                    val file = getCurrentLogFile() ?: run {
                        handleInternalError("File creation failed during init", null)
                        return@withLock
                    }

                    // 强制写入初始化头
                    FileWriter(file, true).use {
                        it.write(
                            "${dateFormat.get()?.format(Date())} | " +
                                    "✅ APP START | " +
                                    "Version ${appCtx.appVersionCode} | "+
                                    "DebugMode: $isDebugMode\n"
                        )
                    }

                    // 重置写入器确保后续日志正常
                    currentLogWriter?.close()
                    currentLogWriter = BufferedWriter(FileWriter(file, true))
                } catch (e: Exception) {
                    handleInternalError("Failed to write init header", e)
                }
            }
        }
    }

    private fun startLogConsumer() {
        logJob = CoroutineScope(Dispatchers.IO).launch {
            val batch = ArrayList<String>(MAX_BATCH_SIZE)
            var lastFlushTime = System.currentTimeMillis()

            while (isActive) {
                try {
                    // 双重触发条件：信号或超时
                    val waitTime = BUFFER_FLUSH_INTERVAL - (System.currentTimeMillis() - lastFlushTime)
                    val receivedSignal = withTimeoutOrNull(waitTime.coerceAtLeast(0)) {
                        logSignalChannel.receive()
                    } != null

                    // 智能批量处理
                    synchronized(logBuffer) {
                        val maxFetch = if (receivedSignal) MAX_BATCH_SIZE else logBuffer.size
                        while (batch.size < maxFetch && logBuffer.isNotEmpty()) {
                            logBuffer.poll()?.let { batch.add(it) }
                        }
                    }

                    // 强制刷新条件
                    if (batch.isNotEmpty() || System.currentTimeMillis() - lastFlushTime >= BUFFER_FLUSH_INTERVAL) {
                        flushLogs(batch)
                        batch.clear()
                        lastFlushTime = System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    handleInternalError("Log consumer error", e)
                    delay(1000) // 错误恢复间隔
                }
            }
        }
    }
    // endregion

    // region File Operations
    private suspend fun flushLogs(batch: List<String>) {
        mutex.withLock {
            try {
                // region 修复点3：安全获取当前日志文件
                val file = getCurrentLogFile() ?: run {
                    handleInternalError("File creation failed", null)
                    return@withLock
                }

                // 防御性检查：确保操作的是文件
                if (file.isDirectory) {
                    handleInternalError("Path is directory: ${file.absolutePath}", null)
                    return@withLock
                }

                // region 修复点4：智能文件切换逻辑
                if (currentLogFile != null && file != currentLogFile) {
                    closeWriter()
                    currentLogFile = file
                    cleanupOldFiles()
                }

                // region 修复点5：带缓冲的批量写入
                currentLogWriter = currentLogWriter ?: BufferedWriter(FileWriter(file, true))

                if (batch.isNotEmpty()) {
                    try {
                        currentLogWriter?.let { writer ->
                            batch.forEach { line ->
                                writer.write(line)
                                // 内存中记录当前写入位置（可用于崩溃恢复）
                                file.length() + line.toByteArray().size
                            }
                            writer.flush() // 注意：保持打开状态以提高性能
                        }
                    } catch (e: IOException) {
                        handleInternalError("Batch write failed", e)
                        closeWriter()
                        currentLogFile = null // 触发下次重建
                    }
                }

            } catch (e: SecurityException) {
                handleInternalError("Permission denied", e)
            } catch (e: Exception) {
                handleInternalError("Unexpected error", e)
            }
        }
    }

    // region 关键修复4：统一使用logDirectory
    private fun cleanupOldFiles() {
        logDirectory?.let { dir ->
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -config.maxFileCount)
            }
            val cutoffDate = calendar.time

            dir.listFiles { file ->
                try {
                    val dateStr = file.name.removePrefix("app_").removeSuffix(".log")
                    val logDate = SimpleDateFormat(config.fileDateFormat, Locale.getDefault())
                        .parse(dateStr)
                    logDate?.before(cutoffDate) == true
                } catch (e: Exception) {
                    false // 忽略解析失败的文件
                }
            }?.forEach {
                try {
                    it.delete()
                } catch (e: Exception) {
                    handleInternalError("Delete failed: ${it.absolutePath}", e)
                }
            }
        }
    }

    // region 关键修复2：正确的文件获取逻辑
    private fun getCurrentLogFile(): File? {
        logDirectory?.let { dir ->
            val fileName = "app_${
                SimpleDateFormat(
                    config.fileDateFormat,
                    Locale.getDefault()
                ).format(Date())
            }.log"
            return File(dir, fileName).apply {
                try {
                    if (!exists()) {
                        parentFile?.mkdirs() // 确保目录存在
                        createNewFile()
                        // 写入初始化头到新文件
                        FileWriter(this).use { it.write("Initialization [${appCtx.packageName}] Log \n") }
                    }
                } catch (e: IOException) {
                    handleInternalError("Create file failed", e)
                    return null
                }
            }
        }
        return null
    }
    // endregion

    private fun determineStoragePath() {
        val strategies = when (config.storageStrategy) {
            StorageStrategy.EXTERNAL_ONLY -> listOf(AppLog::getExternalDir)
            StorageStrategy.INTERNAL_ONLY -> listOf(AppLog::getInternalDir)
            StorageStrategy.EXTERNAL_FIRST -> listOf(AppLog::getExternalDir, AppLog::getInternalDir)
        }

        // 确定日志目录，而非文件
        logDirectory = strategies.firstNotNullOfOrNull { it() }?.takeIf { dir ->
            dir.mkdirs() // 确保目录存在
            dir.isDirectory && dir.canWrite()
        }
    }

    private fun getExternalDir() = appCtx.getExternalFilesDir("logs")
    private fun getInternalDir() = File(appCtx.filesDir, "logs")

    // region Error Handling
    private fun handleInternalError(message: String, e: Exception?) {
        if (isDebugMode) {
            Log.e("AppLogger", message, e)
        }
        closeWriter()
    }

    private fun closeWriter() {
        try {
            currentLogWriter?.close()
        } catch (e: IOException) {
            Log.e("AppLogger", "Failed to close writer", e)
        } finally {
            currentLogWriter = null
        }
    }

    private suspend fun flushAndClose() {
        mutex.withLock {
            try {
                currentLogWriter?.flush()
            } finally {
                closeWriter()
            }
        }
    }
    // endregion

    // region Extension Functions
    private fun LogConfig.applyFrom(other: LogConfig) {
        maxFileCount = other.maxFileCount
        dateFormat = other.dateFormat
        fileDateFormat = other.fileDateFormat
        releasePrintLevel = other.releasePrintLevel
        storageStrategy = other.storageStrategy
        enableThreadInfo = other.enableThreadInfo
    }
    // endregion
}