package io.core.utils.tools.file

import android.os.Environment
import io.core.utils.extensions.cool.printOnDebug
import io.core.utils.extensions.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 文件基础操作�?
 * 提供创建、删除、复制、移动等核心文件操作
 */
object FileOperations {
    
    private const val BUFFER_SIZE = 8192
    
    /**
     * 创建文件（如果不存在�?
     * 自动创建所有必需的父目录
     */
    fun createFileIfNotExists(filePath: String): FileOperationResult<File> = safeFileOperation {
        val file = File(filePath)
        if (!file.exists()) {
            // 创建父目�?
            file.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }
            file.createNewFile()
        }
        file
    }
    
    /**
     * 创建文件（基于根目录和子路径�?
     */
    fun createFileIfNotExists(root: File, vararg subPaths: String): FileOperationResult<File> {
        val filePath = PathUtils.buildPath(root, *subPaths)
        return createFileIfNotExists(filePath)
    }
    
    /**
     * 创建目录（如果不存在�?
     */
    fun createDirectoryIfNotExists(dirPath: String): FileOperationResult<File> = safeFileOperation {
        val dir = File(dirPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }
    
    /**
     * 创建目录（基于根目录和子路径�?
     */
    fun createDirectoryIfNotExists(root: File, vararg subPaths: String): FileOperationResult<File> {
        val dirPath = PathUtils.buildPath(root, *subPaths)
        return createDirectoryIfNotExists(dirPath)
    }
    
    /**
     * 创建文件并替换已存在的文�?
     */
    fun createFileWithReplace(filePath: String): FileOperationResult<File> = safeFileOperation {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
        // 创建父目�?
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        file.createNewFile()
        file
    }
    
    /**
     * 删除文件或目�?
     * @param deleteRootDir 是否删除根目录本�?
     */
    fun delete(filePath: String, deleteRootDir: Boolean = true): FileOperationResult<Boolean> {
        return delete(File(filePath), deleteRootDir)
    }
    
    /**
     * 删除文件或目�?
     */
    fun delete(file: File, deleteRootDir: Boolean = true): FileOperationResult<Boolean> = safeFileOperation {
        when {
            !file.exists() -> false
            file.isFile -> deleteFileWithEBUSYFix(file)
            file.isDirectory -> deleteDirectory(file, deleteRootDir)
            else -> false
        }
    }
    
    /**
     * 递归删除目录
     */
    private fun deleteDirectory(dir: File, deleteRootDir: Boolean): Boolean {
        var success = true
        dir.listFiles()?.forEach { child ->
            success = delete(child, true).getOrNull() ?: false && success
        }
        return if (deleteRootDir) {
            deleteFileWithEBUSYFix(dir) && success
        } else {
            success
        }
    }
    
    /**
     * 解决 EBUSY 错误的删除方�?
     * 通过重命名文件再删除来避免设备忙碌错�?
     */
    private fun deleteFileWithEBUSYFix(file: File): Boolean {
        return try {
            val tempFile = File("${file.absolutePath}_${currentTimeMillis}")
            if (file.renameTo(tempFile)) {
                tempFile.delete()
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 复制文件或目�?
     */
    fun copy(srcPath: String, destPath: String): FileOperationResult<Boolean> {
        return copy(File(srcPath), File(destPath))
    }
    
    /**
     * 复制文件或目�?
     */
    fun copy(src: File, dest: File): FileOperationResult<Boolean> = safeFileOperation {
        when {
            !src.exists() -> false
            src.isFile -> copyFile(src, dest)
            src.isDirectory -> copyDirectory(src, dest)
            else -> false
        }
    }
    
    /**
     * 异步复制文件或目�?
     */
    suspend fun copyAsync(src: File, dest: File): FileOperationResult<Boolean> = withContext(Dispatchers.IO) {
        copy(src, dest)
    }
    
    /**
     * 复制单个文件
     */
    private fun copyFile(src: File, dest: File): Boolean {
        return try {
            // 确保目标目录存在
            dest.parentFile?.mkdirs()
            
            // 使用传统 IO 确保兼容�?
            FileInputStream(src).use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output, BUFFER_SIZE)
                }
            }
            true
        } catch (e: Exception) {
            e.printOnDebug()
            false
        }
    }
    
    /**
     * 递归复制目录
     */
    private fun copyDirectory(src: File, dest: File): Boolean {
        if (!dest.exists()) {
            dest.mkdirs()
        }
        
        return src.listFiles()?.all { child ->
            val destChild = File(dest, child.name)
            copy(child, destChild).getOrNull() ?: false
        } ?: false
    }
    
    /**
     * 移动文件或目�?
     */
    fun move(srcPath: String, destPath: String): FileOperationResult<Boolean> {
        return move(File(srcPath), File(destPath))
    }
    
    /**
     * 移动文件或目�?
     */
    fun move(src: File, dest: File): FileOperationResult<Boolean> = safeFileOperation {
        when {
            !src.exists() -> false
            src.renameTo(dest) -> true
            else -> {
                // 如果重命名失败，尝试复制后删�?
                val copyResult = copy(src, dest).getOrNull() ?: false
                if (copyResult) {
                    delete(src).getOrNull() ?: false
                } else {
                    false
                }
            }
        }
    }
    
    /**
     * 重命名文件或目录
     */
    fun rename(oldPath: String, newPath: String): FileOperationResult<Boolean> {
        return rename(File(oldPath), File(newPath))
    }
    
    /**
     * 重命名文件或目录
     */
    fun rename(src: File, dest: File): FileOperationResult<Boolean> = safeFileOperation {
        src.renameTo(dest)
    }
    
    /**
     * 检查文件或目录是否存在
     */
    fun exists(path: String): Boolean = File(path).exists()
    
    /**
     * 检查是否为文件
     */
    fun isFile(path: String): Boolean = File(path).isFile
    
    /**
     * 检查是否为目录
     */
    fun isDirectory(path: String): Boolean = File(path).isDirectory
    
    /**
     * 获取 SD 卡路�?
     */
    fun getSdCardPath(): String {
        return try {
            val sdCardDir = Environment.getExternalStorageDirectory()
            sdCardDir.canonicalPath
        } catch (e: IOException) {
            e.printOnDebug()
            Environment.getExternalStorageDirectory().absolutePath
        }
    }
    
    /**
     * 获取文件大小
     */
    fun getFileSize(path: String): Long {
        val file = File(path)
        return if (file.exists() && file.isFile) {
            file.length()
        } else {
            0L
        }
    }
    
    /**
     * 获取目录大小（递归计算�?
     */
    fun getDirectorySize(path: String): Long {
        return getDirectorySize(File(path))
    }
    
    /**
     * 获取目录大小（递归计算�?
     */
    fun getDirectorySize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) return 0L
        
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isFile) {
                file.length()
            } else {
                getDirectorySize(file)
            }
        }
        return size
    }
    
    /**
     * 创建多级目录
     */
    fun createDirectories(path: String): FileOperationResult<Boolean> = safeFileOperation {
        File(path).mkdirs()
    }
    
    /**
     * 比较两个文件的最后修改时�?
     * @return 1: file1 更新, -1: file2 更新, 0: 相同
     */
    fun compareLastModified(path1: String, path2: String): Int {
        val time1 = File(path1).lastModified()
        val time2 = File(path2).lastModified()
        return time1.compareTo(time2)
    }
}