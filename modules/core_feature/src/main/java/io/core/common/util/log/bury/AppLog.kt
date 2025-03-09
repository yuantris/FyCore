package io.core.common.util.log.bury

import android.os.Build
import android.os.StatFs
import android.util.Log
import io.core.BuildConfig
import io.core.appCtx
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean


/**
 * 应用程序日志工具，支持控制台输出和文件存储
 *
 * 功能特性：
 * - 多级别日志输出（DEBUG/INFO/WARN/ERROR）
 * - 线程安全的异步日志处理
 * - 自动日志文件滚动（按大小和日期）
 * - 存储策略配置（优先外部存储/内部存储）
 * - 日志文件上传支持
 *
 * 使用示例：
 * ```
 * // 初始化配置（可选）
 * AppLog.initialize(AppLog.LogConfig(
 *     maxFileSize = 1 * 1024 * 1024, // 1MB
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
        var maxFileSize: Long = 2 * 1024 * 1024,
        var maxFileCount: Int = 5,
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
    private val isDebugMode = BuildConfig.DEBUG
    private val logQueue = LinkedBlockingQueue<String>(DEFAULT_MAX_QUEUE_SIZE)
    private var logJob: Job? = null
    private val mutex = Mutex()
    private val isInitialized = AtomicBoolean(false)
    private var currentLogWriter: BufferedWriter? = null
    private var currentLogFile: File? = null
    private var storageCheckedTime = 0L
    private const val STORAGE_CHECK_INTERVAL = 60_000L

    private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat(config.dateFormat, Locale.getDefault())
    }

    fun initialize(customConfig: LogConfig? = null) {
        if (!isInitialized.compareAndSet(false, true)) return

        customConfig?.let { config.applyFrom(it) }
        determineStoragePath()
        startLogConsumer()
    }

    // region Public API
    fun debug(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun info(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun warning(tag: String, message: String, e: Throwable? = null) =
        log(LogLevel.WARN, tag, message, e)

    fun error(tag: String, message: String, e: Throwable? = null) =
        log(LogLevel.ERROR, tag, message, e)

    fun getLogFiles(): List<File> {
        val logDir = getLogDirectory() ?: return emptyList()
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

    fun release() {
        logJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            flushAndClose()
        }
    }
    // endregion

    // region Core Implementation
    private fun log(level: Int, tag: String, message: String, e: Throwable? = null) {
        if (!shouldPrint(level)) return

        val threadInfo = if (config.enableThreadInfo) {
            "${Thread.currentThread().name}_${Thread.currentThread().id}"
        } else ""
        val stackTrace = e?.stackTraceToString()?.prependIndent() ?: ""
        val fullMessage = buildLogEntry(level, threadInfo, tag, "$message$stackTrace")

        if (!logQueue.offer(fullMessage)) {
            // 队列已满时淘汰最旧日志
            logQueue.poll()
            logQueue.offer(fullMessage)
        }

        printToConsole(level, tag, message, e)
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

    private fun buildLogEntry(level: Int, threadInfo: String, tag: String, message: String): String {
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

    private fun startLogConsumer() {
        logJob = CoroutineScope(Dispatchers.IO).launch {
            var lastFlushTime = System.currentTimeMillis()

            while (isActive) {
                try {
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastFlush = currentTime - lastFlushTime

                    if (logQueue.isNotEmpty() && (logQueue.size >= MAX_BATCH_SIZE || timeSinceLastFlush >= BUFFER_FLUSH_INTERVAL)) {
                        flushLogs()
                        lastFlushTime = currentTime
                    }

                    delay(500)
                } catch (e: Exception) {
                    handleInternalError("Log consumer error", e)
                }
            }
        }
    }
    // endregion

    // region File Operations
    private suspend fun flushLogs() {
        mutex.withLock {
            try {
                val logDir = verifyAndGetLogDir() ?: return@withLock
                if (!ensureDiskSpace(logDir)) {
                    handleInternalError("Insufficient disk space", null)
                    return@withLock
                }

                val file = getCurrentLogFile(logDir)
                if (file != currentLogFile) {
                    rotateLogFile(file)
                }

                currentLogWriter = currentLogWriter ?: BufferedWriter(FileWriter(file, true).also {
                    if (file.length() == 0L) {
                        it.write("${dateFormat.get()?.format(Date())} | LOG FILE INITIALIZED\n")
                    }
                })

                val batch = ArrayList<String>(MAX_BATCH_SIZE)
                logQueue.drainTo(batch, MAX_BATCH_SIZE)
                if (batch.isNotEmpty()) {
                    currentLogWriter?.let { writer ->
                        batch.forEach { writer.write(it) }
                        writer.flush() // 仅刷新，不关闭
                    }
                }

                if (file.length() > config.maxFileSize) {
                    rotateLogFile(null)
                }
            } catch (e: Exception) {
                handleInternalError("Log flush failed", e)
                closeWriter()
                determineStoragePath()
            }
        }
    }

    private fun rotateLogFile(newFile: File?) {
        closeWriter()
        currentLogFile?.let {
            try {
                val sequence = getNextFileSequence(it)
                val rotatedFile = File(it.parent, "${it.nameWithoutExtension}_$sequence.log")
                if (!it.renameTo(rotatedFile)) {
                    handleInternalError("File rotation failed", null)
                }
            } catch (e: Exception) {
                handleInternalError("File rotation error", e)
            }
        }
        currentLogFile = newFile
        cleanupOldFiles()
    }

    private fun getNextFileSequence(file: File): Int {
        val pattern = "${file.nameWithoutExtension}_(\\d+).log".toRegex()
        return file.parentFile?.listFiles()
            ?.mapNotNull { pattern.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            ?.maxOrNull()?.plus(1) ?: 1
    }

    private fun cleanupOldFiles() {
        getLogDirectory()?.let { dir ->
            dir.listFiles()
                ?.filter { it.name.startsWith("app_") }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(config.maxFileCount)
                ?.forEach { it.delete() }
        }
    }

    private fun getCurrentLogFile(logDir: File): File {
        return File(logDir, "app_${SimpleDateFormat(config.fileDateFormat, Locale.getDefault()).format(Date())}.log").apply {
            if (!exists()) {
                parentFile?.mkdirs()
                createNewFile()
            }
        }
    }
    // endregion

    // region Storage Management
    private fun verifyAndGetLogDir(): File? {
        if (System.currentTimeMillis() - storageCheckedTime > STORAGE_CHECK_INTERVAL) {
            determineStoragePath()
            storageCheckedTime = System.currentTimeMillis()
        }
        return getLogDirectory()?.takeIf { it.exists() && it.canWrite() }
    }

    private fun determineStoragePath() {
        val strategies = when (config.storageStrategy) {
            StorageStrategy.EXTERNAL_ONLY -> listOf(AppLog::getExternalDir)
            StorageStrategy.INTERNAL_ONLY -> listOf(AppLog::getInternalDir)
            StorageStrategy.EXTERNAL_FIRST -> listOf(AppLog::getExternalDir, AppLog::getInternalDir)
        }

        currentLogFile = strategies.firstNotNullOfOrNull { it() }?.also { dir ->
            if (!dir.exists()) dir.mkdirs()
            if (dir.isDirectory && dir.canWrite()) dir else null
        }
    }

    private fun getExternalDir() = appCtx.getExternalFilesDir("logs")
    private fun getInternalDir() = File(appCtx.filesDir, "logs")

    private fun getLogDirectory(): File? = currentLogFile?.parentFile

    private fun ensureDiskSpace(logDir: File): Boolean {
        return try {
            val stat = StatFs(logDir.absolutePath)
            val availableBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stat.availableBytes
            } else {
                @Suppress("DEPRECATION")
                stat.availableBlocks.toLong() * stat.blockSize
            }
            availableBytes > config.maxFileSize * 2
        } catch (e: Exception) {
            false
        }
    }
    // endregion

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
        maxFileSize = other.maxFileSize
        maxFileCount = other.maxFileCount
        dateFormat = other.dateFormat
        fileDateFormat = other.fileDateFormat
        releasePrintLevel = other.releasePrintLevel
        storageStrategy = other.storageStrategy
        enableThreadInfo = other.enableThreadInfo
    }
    // endregion
}