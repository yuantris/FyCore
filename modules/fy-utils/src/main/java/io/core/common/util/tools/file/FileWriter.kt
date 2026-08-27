package io.core.common.util.tools.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * 文件写入工具类
 * 提供各种文件写入功能，支持同步和异步操作
 */
object FileWriter {
    
    private const val DEFAULT_CHARSET = "UTF-8"
    private const val DEFAULT_BUFFER_SIZE = 8192
    
    /**
     * 写入文本内容到文件
     * @param filePath 文件路径
     * @param content 文本内容
     * @param charset 字符编码，默认 UTF-8
     * @param append 是否追加模式，默认覆盖
     */
    fun writeText(
        filePath: String,
        content: String,
        charset: String = DEFAULT_CHARSET,
        append: Boolean = false
    ): FileOperationResult<Unit> = safeFileOperation {
        val file = File(filePath)
        
        // 确保父目录存在
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // 使用 NIO 写入（Android O+）
            val options = if (append) {
                arrayOf(StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            } else {
                arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            }
            Files.write(file.toPath(), content.toByteArray(Charset.forName(charset)), *options)
        } else {
            // 降级到传统 IO
            FileWriter(file, append).use { writer ->
                writer.write(content)
            }
        }
    }
    
    /**
     * 异步写入文本内容到文件
     */
    suspend fun writeTextAsync(
        filePath: String,
        content: String,
        charset: String = DEFAULT_CHARSET,
        append: Boolean = false
    ): FileOperationResult<Unit> = withContext(Dispatchers.IO) {
        writeText(filePath, content, charset, append)
    }
    
    /**
     * 写入字节数组到文件
     * @param filePath 文件路径
     * @param data 字节数组
     * @param append 是否追加模式，默认覆盖
     */
    fun writeBytes(
        filePath: String,
        data: ByteArray,
        append: Boolean = false
    ): FileOperationResult<Unit> = safeFileOperation {
        val file = File(filePath)
        
        // 确保父目录存在
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // 使用 NIO 写入（Android O+）
            val options = if (append) {
                arrayOf(StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            } else {
                arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            }
            Files.write(file.toPath(), data, *options)
        } else {
            // 降级到传统 IO
            FileOutputStream(file, append).use { output ->
                output.write(data)
                output.flush()
            }
        }
    }
    
    /**
     * 异步写入字节数组到文件
     */
    suspend fun writeBytesAsync(
        filePath: String,
        data: ByteArray,
        append: Boolean = false
    ): FileOperationResult<Unit> = withContext(Dispatchers.IO) {
        writeBytes(filePath, data, append)
    }
    
    /**
     * 从输入流写入到文件
     * @param filePath 文件路径
     * @param inputStream 输入流
     * @param append 是否追加模式，默认覆盖
     * @param bufferSize 缓冲区大小
     * @param progressCallback 进度回调 (已写入字节数, 总字节数)
     */
    fun writeFromInputStream(
        filePath: String,
        inputStream: InputStream,
        append: Boolean = false,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): FileOperationResult<Unit> = safeFileOperation {
        val file = File(filePath)
        
        // 确保父目录存在
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        
        val totalSize = try {
            inputStream.available().toLong()
        } catch (e: Exception) {
            -1L
        }
        
        var bytesWritten = 0L
        
        inputStream.use { input ->
            FileOutputStream(file, append).use { output ->
                val buffer = ByteArray(bufferSize)
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    bytesWritten += bytesRead
                    progressCallback?.invoke(bytesWritten, totalSize)
                }
                output.flush()
            }
        }
    }
    
    /**
     * 从输入流写入到文件（File 版本）
     */
    fun writeFromInputStream(
        file: File,
        inputStream: InputStream,
        append: Boolean = false,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): FileOperationResult<Unit> {
        return writeFromInputStream(file.absolutePath, inputStream, append, bufferSize, progressCallback)
    }
    
    /**
     * 异步从输入流写入到文件
     */
    suspend fun writeFromInputStreamAsync(
        filePath: String,
        inputStream: InputStream,
        append: Boolean = false,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): FileOperationResult<Unit> = withContext(Dispatchers.IO) {
        writeFromInputStream(filePath, inputStream, append, bufferSize, progressCallback)
    }
    
    /**
     * 追加文本内容到文件
     * @param filePath 文件路径
     * @param content 文本内容
     * @param charset 字符编码，默认 UTF-8
     */
    fun appendText(
        filePath: String,
        content: String,
        charset: String = DEFAULT_CHARSET
    ): FileOperationResult<Unit> {
        return writeText(filePath, content, charset, append = true)
    }
    
    /**
     * 异步追加文本内容到文件
     */
    suspend fun appendTextAsync(
        filePath: String,
        content: String,
        charset: String = DEFAULT_CHARSET
    ): FileOperationResult<Unit> {
        return writeTextAsync(filePath, content, charset, append = true)
    }
    
    /**
     * 追加字节数组到文件
     */
    fun appendBytes(filePath: String, data: ByteArray): FileOperationResult<Unit> {
        return writeBytes(filePath, data, append = true)
    }
    
    /**
     * 异步追加字节数组到文件
     */
    suspend fun appendBytesAsync(filePath: String, data: ByteArray): FileOperationResult<Unit> {
        return writeBytesAsync(filePath, data, append = true)
    }
    
    /**
     * 按行写入文本列表到文件
     * @param filePath 文件路径
     * @param lines 文本行列表
     * @param charset 字符编码，默认 UTF-8
     * @param lineSeparator 行分隔符，默认系统换行符
     * @param append 是否追加模式，默认覆盖
     */
    fun writeLines(
        filePath: String,
        lines: List<String>,
        charset: String = DEFAULT_CHARSET,
        lineSeparator: String = System.lineSeparator(),
        append: Boolean = false
    ): FileOperationResult<Unit> = safeFileOperation {
        val content = lines.joinToString(lineSeparator) + lineSeparator
        writeText(filePath, content, charset, append).getOrThrow()
    }
    
    /**
     * 异步按行写入文本列表到文件
     */
    suspend fun writeLinesAsync(
        filePath: String,
        lines: List<String>,
        charset: String = DEFAULT_CHARSET,
        lineSeparator: String = System.lineSeparator(),
        append: Boolean = false
    ): FileOperationResult<Unit> = withContext(Dispatchers.IO) {
        writeLines(filePath, lines, charset, lineSeparator, append)
    }
    
    /**
     * 流式写入大量数据
     * @param filePath 文件路径
     * @param dataProvider 数据提供者函数，返回 null 表示结束
     * @param append 是否追加模式，默认覆盖
     */
    fun writeStreaming(
        filePath: String,
        dataProvider: () -> ByteArray?,
        append: Boolean = false
    ): FileOperationResult<Unit> = safeFileOperation {
        val file = File(filePath)
        
        // 确保父目录存在
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        
        FileOutputStream(file, append).use { output ->
            while (true) {
                val data = dataProvider() ?: break
                output.write(data)
            }
            output.flush()
        }
    }
    
    /**
     * 异步流式写入大量数据
     */
    suspend fun writeStreamingAsync(
        filePath: String,
        dataProvider: () -> ByteArray?,
        append: Boolean = false
    ): FileOperationResult<Unit> = withContext(Dispatchers.IO) {
        writeStreaming(filePath, dataProvider, append)
    }
    
    /**
     * 创建临时文件并写入内容
     * @param content 文件内容
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @param charset 字符编码，默认 UTF-8
     * @return 临时文件对象
     */
    fun createTempFile(
        content: String,
        prefix: String = "temp",
        suffix: String = ".tmp",
        charset: String = DEFAULT_CHARSET
    ): FileOperationResult<File> = safeFileOperation {
        val tempFile = File.createTempFile(prefix, suffix)
        writeText(tempFile.absolutePath, content, charset).getOrThrow()
        tempFile
    }
    
    /**
     * 异步创建临时文件并写入内容
     */
    suspend fun createTempFileAsync(
        content: String,
        prefix: String = "temp",
        suffix: String = ".tmp",
        charset: String = DEFAULT_CHARSET
    ): FileOperationResult<File> = withContext(Dispatchers.IO) {
        createTempFile(content, prefix, suffix, charset)
    }
    
    /**
     * 批量写入文件
     * @param operations 写入操作列表
     */
    fun batchWrite(operations: List<WriteOperation>): FileOperationResult<List<Boolean>> = safeFileOperation {
        operations.map { operation ->
            when (operation) {
                is WriteOperation.Text -> writeText(
                    operation.filePath,
                    operation.content,
                    operation.charset,
                    operation.append
                ).getOrNull() != null
                
                is WriteOperation.Bytes -> writeBytes(
                    operation.filePath,
                    operation.data,
                    operation.append
                ).getOrNull() != null
                
                is WriteOperation.Lines -> writeLines(
                    operation.filePath,
                    operation.lines,
                    operation.charset,
                    operation.lineSeparator,
                    operation.append
                ).getOrNull() != null
            }
        }
    }
    
    /**
     * 异步批量写入文件
     */
    suspend fun batchWriteAsync(operations: List<WriteOperation>): FileOperationResult<List<Boolean>> = 
        withContext(Dispatchers.IO) {
            batchWrite(operations)
        }
    
    /**
     * 检查文件是否可写
     */
    fun isWritable(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) {
            file.canWrite()
        } else {
            // 检查父目录是否可写
            file.parentFile?.canWrite() ?: false
        }
    }
    
    /**
     * 确保文件可写（创建必要的目录）
     */
    fun ensureWritable(filePath: String): FileOperationResult<Unit> = safeFileOperation {
        val file = File(filePath)
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
    }
}

/**
 * 写入操作密封类
 */
sealed class WriteOperation {
    data class Text(
        val filePath: String,
        val content: String,
        val charset: String = "UTF-8",
        val append: Boolean = false
    ) : WriteOperation()
    
    data class Bytes(
        val filePath: String,
        val data: ByteArray,
        val append: Boolean = false
    ) : WriteOperation() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as Bytes
            
            if (filePath != other.filePath) return false
            if (!data.contentEquals(other.data)) return false
            if (append != other.append) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = filePath.hashCode()
            result = 31 * result + data.contentHashCode()
            result = 31 * result + append.hashCode()
            return result
        }
    }
    
    data class Lines(
        val filePath: String,
        val lines: List<String>,
        val charset: String = "UTF-8",
        val lineSeparator: String = System.lineSeparator(),
        val append: Boolean = false
    ) : WriteOperation()
}