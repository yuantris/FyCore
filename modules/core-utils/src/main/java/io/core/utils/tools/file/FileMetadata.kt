package io.core.utils.tools.file

import android.webkit.MimeTypeMap
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件元数据工具类
 * 提供文件属性、MIME类型、时间格式化等功�?
 */
object FileMetadata {
    
    private const val DEFAULT_DATE_FORMAT = "yyyy年MM月dd�?HH:mm"
    private const val DEFAULT_EXTENSION = "unknown"
    private const val DEFAULT_MIME_TYPE = "*/*"
    
    /**
     * 获取文件扩展名（不包含点�?
     */
    fun getExtension(filePath: String): String {
        return PathUtils.getExtension(filePath).ifEmpty { DEFAULT_EXTENSION }
    }
    
    /**
     * 获取文件名（不包含扩展名�?
     */
    fun getNameWithoutExtension(filePath: String): String {
        return PathUtils.getNameWithoutExtension(filePath)
    }
    
    /**
     * 获取文件名（包含扩展名）
     */
    fun getFileName(filePath: String): String {
        return File(filePath).name
    }
    
    /**
     * 获取文件�?MIME 类型
     */
    fun getMimeType(filePath: String): String {
        val extension = getExtension(filePath)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) 
            ?: DEFAULT_MIME_TYPE
    }
    
    /**
     * 根据 MIME 类型判断文件类型
     */
    fun getFileType(filePath: String): FileType {
        val mimeType = getMimeType(filePath)
        return when {
            mimeType.startsWith("image/") -> FileType.IMAGE
            mimeType.startsWith("video/") -> FileType.VIDEO
            mimeType.startsWith("audio/") -> FileType.AUDIO
            mimeType.startsWith("text/") -> FileType.TEXT
            mimeType == "application/pdf" -> FileType.PDF
            mimeType.startsWith("application/") && (
                mimeType.contains("zip") || 
                mimeType.contains("rar") || 
                mimeType.contains("7z") ||
                mimeType.contains("tar")
            ) -> FileType.ARCHIVE
            mimeType.startsWith("application/") && (
                mimeType.contains("msword") ||
                mimeType.contains("wordprocessingml") ||
                mimeType.contains("spreadsheet") ||
                mimeType.contains("presentation")
            ) -> FileType.DOCUMENT
            else -> FileType.OTHER
        }
    }
    
    /**
     * 格式化文件大�?
     */
    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
        
        return String.format(
            Locale.getDefault(),
            "%.1f %s",
            sizeInBytes / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }
    
    /**
     * 获取格式化的文件大小
     */
    fun getFormattedSize(filePath: String): String {
        val size = FileOperations.getFileSize(filePath)
        return formatFileSize(size)
    }
    
    /**
     * 获取格式化的目录大小
     */
    fun getFormattedDirectorySize(dirPath: String): String {
        val size = FileOperations.getDirectorySize(dirPath)
        return formatFileSize(size)
    }
    
    /**
     * 获取格式化的文件修改时间
     */
    fun getFormattedDateTime(filePath: String, format: String = DEFAULT_DATE_FORMAT): String {
        return getFormattedDateTime(File(filePath), format)
    }
    
    /**
     * 获取格式化的文件修改时间
     */
    fun getFormattedDateTime(file: File, format: String = DEFAULT_DATE_FORMAT): String {
        if (!file.exists()) return "未知时间"
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = file.lastModified()
        
        return try {
            SimpleDateFormat(format, Locale.getDefault()).format(calendar.time)
        } catch (e: Exception) {
            "时间格式错误"
        }
    }
    
    /**
     * 获取文件的详细信�?
     */
    fun getFileInfo(filePath: String): FileOperationResult<FileInfo> = safeFileOperation {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $filePath")
        }
        
        FileInfo(
            name = file.name,
            nameWithoutExtension = getNameWithoutExtension(filePath),
            extension = getExtension(filePath),
            path = file.absolutePath,
            parentPath = file.parent ?: "",
            size = if (file.isFile) file.length() else FileOperations.getDirectorySize(filePath),
            formattedSize = if (file.isFile) formatFileSize(file.length()) else getFormattedDirectorySize(filePath),
            lastModified = file.lastModified(),
            formattedLastModified = getFormattedDateTime(file),
            isFile = file.isFile,
            isDirectory = file.isDirectory,
            isHidden = file.isHidden,
            canRead = file.canRead(),
            canWrite = file.canWrite(),
            canExecute = file.canExecute(),
            mimeType = if (file.isFile) getMimeType(filePath) else "inode/directory",
            fileType = if (file.isFile) getFileType(filePath) else FileType.DIRECTORY
        )
    }
    
    /**
     * 比较两个文件的修改时�?
     * @return 1: file1 更新, -1: file2 更新, 0: 相同
     */
    fun compareLastModified(filePath1: String, filePath2: String): Int {
        return FileOperations.compareLastModified(filePath1, filePath2)
    }
    
    /**
     * 检查文件是否为图片
     */
    fun isImage(filePath: String): Boolean {
        return getFileType(filePath) == FileType.IMAGE
    }
    
    /**
     * 检查文件是否为视频
     */
    fun isVideo(filePath: String): Boolean {
        return getFileType(filePath) == FileType.VIDEO
    }
    
    /**
     * 检查文件是否为音频
     */
    fun isAudio(filePath: String): Boolean {
        return getFileType(filePath) == FileType.AUDIO
    }
    
    /**
     * 检查文件是否为文本文件
     */
    fun isText(filePath: String): Boolean {
        return getFileType(filePath) == FileType.TEXT
    }
    
    /**
     * 检查文件是否为压缩�?
     */
    fun isArchive(filePath: String): Boolean {
        return getFileType(filePath) == FileType.ARCHIVE
    }
    
    /**
     * 检查文件是否为文档
     */
    fun isDocument(filePath: String): Boolean {
        return getFileType(filePath) == FileType.DOCUMENT
    }
    
    /**
     * 检查文件是否为 PDF
     */
    fun isPdf(filePath: String): Boolean {
        return getFileType(filePath) == FileType.PDF
    }
    
    /**
     * 获取文件的哈希值（用于文件完整性校验）
     */
    fun getFileHash(filePath: String, algorithm: String = "MD5"): FileOperationResult<String> = safeFileOperation {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("File does not exist or is not a file: $filePath")
        }
        
        val digest = java.security.MessageDigest.getInstance(algorithm)
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        
        digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 检查两个文件是否相同（通过哈希值比较）
     */
    fun areFilesIdentical(filePath1: String, filePath2: String): FileOperationResult<Boolean> = safeFileOperation {
        val hash1 = getFileHash(filePath1).getOrThrow()
        val hash2 = getFileHash(filePath2).getOrThrow()
        hash1 == hash2
    }
    
    /**
     * 获取文件权限信息
     */
    fun getPermissions(filePath: String): FileOperationResult<FilePermissions> = safeFileOperation {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $filePath")
        }
        
        FilePermissions(
            canRead = file.canRead(),
            canWrite = file.canWrite(),
            canExecute = file.canExecute(),
            isHidden = file.isHidden
        )
    }
}

/**
 * 文件类型枚举
 */
enum class FileType {
    IMAGE,      // 图片
    VIDEO,      // 视频
    AUDIO,      // 音频
    TEXT,       // 文本
    PDF,        // PDF文档
    DOCUMENT,   // 办公文档
    ARCHIVE,    // 压缩�?
    DIRECTORY,  // 目录
    OTHER       // 其他
}

/**
 * 文件信息数据�?
 */
data class FileInfo(
    val name: String,                    // 文件�?
    val nameWithoutExtension: String,    // 不含扩展名的文件�?
    val extension: String,               // 扩展�?
    val path: String,                    // 完整路径
    val parentPath: String,              // 父目录路�?
    val size: Long,                      // 文件大小（字节）
    val formattedSize: String,           // 格式化的文件大小
    val lastModified: Long,              // 最后修改时间（时间戳）
    val formattedLastModified: String,   // 格式化的最后修改时�?
    val isFile: Boolean,                 // 是否为文�?
    val isDirectory: Boolean,            // 是否为目�?
    val isHidden: Boolean,               // 是否为隐藏文�?
    val canRead: Boolean,                // 是否可读
    val canWrite: Boolean,               // 是否可写
    val canExecute: Boolean,             // 是否可执�?
    val mimeType: String,                // MIME类型
    val fileType: FileType               // 文件类型
)

/**
 * 文件权限信息数据�?
 */
data class FilePermissions(
    val canRead: Boolean,      // 可读
    val canWrite: Boolean,     // 可写
    val canExecute: Boolean,   // 可执�?
    val isHidden: Boolean      // 隐藏
)