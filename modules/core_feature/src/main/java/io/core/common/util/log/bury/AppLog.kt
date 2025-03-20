package io.core.common.util.log.bury

import android.os.Build
import android.os.StatFs
import android.util.Log
import io.core.BuildConfig
import io.core.appCtx
import io.core.common.util.tools.OsUtils
import io.core.constant.ANDROID_4_3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
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
        var maxFileSize: Long = 30 * 1024 * 1024, // 30MB
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
    // region 关键修复1：分离目录和文件路径
    private var logDirectory: File? = null  // 明确存储日志目录
    private var currentLogFile: File? = null // 始终指向当前日志文件

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

        if (!logQueue.offer(fullMessage)) {
            // 队列已满时淘汰最旧日志
            logQueue.poll()
            logQueue.offer(fullMessage)
        }

        if (shouldPrint(level)){
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
                // region 修复点1：使用新的目录验证方法
                val logDir = verifyAndGetLogDir() ?: run {
                    handleInternalError("Invalid log directory", null)
                    return@withLock
                }

                // region 修复点2：增强磁盘空间检查
                if (!ensureDiskSpace(logDir)) {
                    handleInternalError("Insufficient disk space (available: ${getAvailableSpace(logDir)} bytes)", null)
                    return@withLock
                }

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
                if (file != currentLogFile) {
                    rotateLogFile()
                    currentLogFile = file
                }

                // region 修复点5：带缓冲的批量写入
                currentLogWriter = currentLogWriter ?: BufferedWriter(FileWriter(file, true).also {
                    // 文件首次创建时写入头信息
                    if (file.length() == 0L) {
                        it.write("${dateFormat.get()?.format(Date())} | LOG INIT \n")
                    }
                })

                val batch = ArrayList<String>(MAX_BATCH_SIZE).apply {
                    logQueue.drainTo(this, MAX_BATCH_SIZE)
                }

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

                // region 修复点6：带缓冲区的滚动检查
                val actualSize = file.length()
                if (actualSize > config.maxFileSize * 0.9) { // 增加10%缓冲
                    rotateLogFile()
                }
            } catch (e: SecurityException) {
                handleInternalError("Permission denied", e)
            } catch (e: Exception) {
                handleInternalError("Unexpected error", e)
            }
        }
    }

    private fun getAvailableSpace(dir: File): Long {
        return try {
            StatFs(dir.absolutePath).run {
                if (OsUtils.higherThan(ANDROID_4_3)) availableBytes else {
                    @Suppress("DEPRECATION")
                    availableBlocks.toLong() * blockSize
                }
            }
        } catch (e: Exception) {
            -1
        }
    }

    // region 关键修复3：安全的文件轮转逻辑
    private fun rotateLogFile() {
        closeWriter()
        currentLogFile?.let { file ->
            // 防御性检查：确保操作的是文件
            if (!file.isFile || !file.exists()) return@let

            try {
                val sequence = getNextFileSequence(file)
                val rotatedFile = File(file.parent, "${file.nameWithoutExtension}_$sequence.log")
                if (!file.renameTo(rotatedFile)) {
                    handleInternalError("File rotation failed", null)
                }
            } catch (e: Exception) {
                handleInternalError("File rotation error", e)
            }
        }
        currentLogFile = getCurrentLogFile()
        cleanupOldFiles()
    }

    private fun getNextFileSequence(file: File): Int {
        val pattern = "${file.nameWithoutExtension}_(\\d+).log".toRegex()
        return file.parentFile?.listFiles()
            ?.mapNotNull { pattern.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            ?.maxOrNull()?.plus(1) ?: 1
    }

    // region 关键修复4：统一使用logDirectory
    private fun cleanupOldFiles() {
        logDirectory?.let { dir ->
            dir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("app_") }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(config.maxFileCount)
                ?.forEach {
                    try {
                        if (!it.delete()) {
                            Log.w("AppLog", "Failed to delete old log: ${it.absolutePath}")
                        }
                    } catch (e: SecurityException) {
                        handleInternalError("File deletion failed", e)
                    }
                }
        }
    }

    // region 关键修复2：正确的文件获取逻辑
    private fun getCurrentLogFile(): File? {
        logDirectory?.let { dir ->
            return File(dir, "app_${SimpleDateFormat(config.fileDateFormat, Locale.getDefault()).format(Date())}.log").apply {
                if (!exists()) {
                    parentFile?.mkdirs() // 防御性创建目录
                    try {
                        createNewFile()
                    } catch (e: IOException) {
                        handleInternalError("File creation failed", e)
                    }
                }
            }
        }
        return null
    }
    // endregion

    // region Storage Management
    private fun verifyAndGetLogDir(): File? {
        if (System.currentTimeMillis() - storageCheckedTime > STORAGE_CHECK_INTERVAL) {
            determineStoragePath()
            storageCheckedTime = System.currentTimeMillis()
        }
        return logDirectory?.takeIf { it.exists() && it.canWrite() }
    }

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

    private fun ensureDiskSpace(logDir: File): Boolean {
        return try {
            val stat = StatFs(logDir.absolutePath)
            val availableBytes = if (OsUtils.higherThan(ANDROID_4_3)) {
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