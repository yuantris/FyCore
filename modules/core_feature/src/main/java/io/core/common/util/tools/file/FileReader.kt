package io.core.common.util.tools.file

import android.content.Context
import io.core.appCtx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files

/**
 * 文件读取工具类
 * 提供各种文件读取功能，支持同步和异步操作
 */
object FileReader {
    
    private const val DEFAULT_BUFFER_SIZE = 8192
    private const val DEFAULT_CHARSET = "UTF-8"
    
    /**
     * 读取文本文件内容
     * @param filePath 文件路径
     * @param charset 字符编码，默认 UTF-8
     * @return 文件内容字符串
     */
    fun readText(filePath: String, charset: String = DEFAULT_CHARSET): FileOperationResult<String> = safeFileOperation {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IOException("File not found or is not a file: $filePath")
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // 使用 NIO 读取（Android O+）
            String(Files.readAllBytes(file.toPath()), Charset.forName(charset))
        } else {
            // 降级到传统 IO
            file.readText(Charset.forName(charset))
        }
    }
    
    /**
     * 异步读取文本文件内容
     */
    suspend fun readTextAsync(filePath: String, charset: String = DEFAULT_CHARSET): FileOperationResult<String> = 
        withContext(Dispatchers.IO) {
            readText(filePath, charset)
        }
    
    /**
     * 读取文件字节数组
     * @param filePath 文件路径
     * @return 文件字节数组
     */
    fun readBytes(filePath: String): FileOperationResult<ByteArray> = safeFileOperation {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IOException("File not found or is not a file: $filePath")
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // 使用 NIO 读取（Android O+）
            Files.readAllBytes(file.toPath())
        } else {
            // 降级到传统 IO
            FileInputStream(file).use { input ->
                ByteArrayOutputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.toByteArray()
                }
            }
        }
    }
    
    /**
     * 异步读取文件字节数组
     */
    suspend fun readBytesAsync(filePath: String): FileOperationResult<ByteArray> = 
        withContext(Dispatchers.IO) {
            readBytes(filePath)
        }
    
    /**
     * 分块读取大文件
     * @param filePath 文件路径
     * @param bufferSize 缓冲区大小
     * @param onChunk 每读取一块数据时的回调
     */
    fun readFileInChunks(
        filePath: String,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        onChunk: (ByteArray, Int) -> Unit
    ): FileOperationResult<Unit> = safeFileOperation {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IOException("File not found or is not a file: $filePath")
        }
        
        FileInputStream(file).use { input ->
            val buffer = ByteArray(bufferSize)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                onChunk(buffer, bytesRead)
            }
        }
    }
    
    /**
     * 异步分块读取大文件
     */
    suspend fun readFileInChunksAsync(
        filePath: String,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        onChunk: (ByteArray, Int) -> Unit
    ): FileOperationResult<Unit> = withContext(Dispatchers.IO) {
        readFileInChunks(filePath, bufferSize, onChunk)
    }
    
    /**
     * 读取 Assets 目录下的文件
     * @param fileName 文件名或相对路径
     * @param context 上下文，默认使用全局应用上下文
     * @return 文件内容字符串
     */
    fun readFromAssets(
        fileName: String,
        context: Context = appCtx
    ): FileOperationResult<String> = safeFileOperation {
        context.assets.open(fileName).use { input ->
            input.bufferedReader().use { reader ->
                reader.readText()
            }
        }
    }
    
    /**
     * 读取 Assets 目录下的文件为字节数组
     */
    fun readBytesFromAssets(
        fileName: String,
        context: Context = appCtx
    ): FileOperationResult<ByteArray> = safeFileOperation {
        context.assets.open(fileName).use { input ->
            ByteArrayOutputStream().use { output ->
                input.copyTo(output)
                output.toByteArray()
            }
        }
    }
    
    /**
     * 读取 Raw 资源文件
     * @param resourceId 资源 ID
     * @param context 上下文，默认使用全局应用上下文
     * @return 文件内容字符串
     */
    fun readFromRaw(
        resourceId: Int,
        context: Context = appCtx
    ): FileOperationResult<String> = safeFileOperation {
        context.resources.openRawResource(resourceId).use { input ->
            input.bufferedReader().use { reader ->
                reader.readText()
            }
        }
    }
    
    /**
     * 读取输入流内容为字符串
     * @param inputStream 输入流
     * @param charset 字符编码
     * @return 内容字符串
     */
    fun readFromInputStream(
        inputStream: InputStream,
        charset: String = DEFAULT_CHARSET
    ): FileOperationResult<String> = safeFileOperation {
        inputStream.bufferedReader(Charset.forName(charset)).use { reader ->
            reader.readText()
        }
    }
    
    /**
     * 读取输入流内容为字节数组
     */
    fun readBytesFromInputStream(inputStream: InputStream): FileOperationResult<ByteArray> = safeFileOperation {
        ByteArrayOutputStream().use { output ->
            inputStream.copyTo(output)
            output.toByteArray()
        }
    }
    
    /**
     * 按行读取文件
     * @param filePath 文件路径
     * @param charset 字符编码
     * @return 文件行列表
     */
    fun readLines(filePath: String, charset: String = DEFAULT_CHARSET): FileOperationResult<List<String>> = safeFileOperation {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IOException("File not found or is not a file: $filePath")
        }
        
        file.readLines(Charset.forName(charset))
    }
    
    /**
     * 异步按行读取文件
     */
    suspend fun readLinesAsync(filePath: String, charset: String = DEFAULT_CHARSET): FileOperationResult<List<String>> = 
        withContext(Dispatchers.IO) {
            readLines(filePath, charset)
        }
    
    /**
     * 流式按行读取文件（适用于大文件）
     * @param filePath 文件路径
     * @param charset 字符编码
     * @param onLine 每读取一行时的回调
     */
    fun readLinesStreaming(
        filePath: String,
        charset: String = DEFAULT_CHARSET,
        onLine: (String, Int) -> Unit
    ): FileOperationResult<Unit> = safeFileOperation {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IOException("File not found or is not a file: $filePath")
        }
        
        file.bufferedReader(Charset.forName(charset)).use { reader ->
            var lineNumber = 0
            reader.forEachLine { line ->
                onLine(line, lineNumber++)
            }
        }
    }
    
    /**
     * 异步流式按行读取文件
     */
    suspend fun readLinesStreamingAsync(
        filePath: String,
        charset: String = DEFAULT_CHARSET,
        onLine: (String, Int) -> Unit
    ): FileOperationResult<Unit> = withContext(Dispatchers.IO) {
        readLinesStreaming(filePath, charset, onLine)
    }
    
    /**
     * 检查文件是否可读
     */
    fun isReadable(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists() && file.isFile && file.canRead()
    }
    
    /**
     * 获取文件编码
     * 简单的编码检测，仅支持常见编码
     */
    fun detectEncoding(filePath: String): FileOperationResult<String> = safeFileOperation {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IOException("File not found or is not a file: $filePath")
        }
        
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1024)
            val bytesRead = input.read(buffer)
            
            when {
                // UTF-8 BOM
                bytesRead >= 3 && buffer[0] == 0xEF.toByte() && 
                buffer[1] == 0xBB.toByte() && buffer[2] == 0xBF.toByte() -> "UTF-8"
                
                // UTF-16 BE BOM
                bytesRead >= 2 && buffer[0] == 0xFE.toByte() && buffer[1] == 0xFF.toByte() -> "UTF-16BE"
                
                // UTF-16 LE BOM
                bytesRead >= 2 && buffer[0] == 0xFF.toByte() && buffer[1] == 0xFE.toByte() -> "UTF-16LE"
                
                // 简单的 ASCII/UTF-8 检测
                buffer.take(bytesRead).all { it >= 0 } -> "ASCII"
                
                else -> "UTF-8" // 默认假设为 UTF-8
            }
        }
    }
}