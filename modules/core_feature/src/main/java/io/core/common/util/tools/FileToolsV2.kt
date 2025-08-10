package io.core.common.util.tools

import androidx.annotation.IntDef
import io.core.common.util.extensions.cool.cnCompare
import io.core.common.util.tools.file.DirectoryStats
import io.core.common.util.tools.file.FileInfo
import io.core.common.util.tools.file.FileListManager
import io.core.common.util.tools.file.FileMetadata
import io.core.common.util.tools.file.FileOperations
import io.core.common.util.tools.file.FileReader
import io.core.common.util.tools.file.FileSortStrategy
import io.core.common.util.tools.file.FileSorter
import io.core.common.util.tools.file.FileWriter
import io.core.common.util.tools.file.PathUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.regex.Pattern

/**
 * 重构后的文件工具类
 * 提供现代化的文件操作 API，同时保持向后兼容性
 * 
 * 主要改进：
 * 1. 模块化设计，职责分离
 * 2. 类型安全的错误处理
 * 3. 协程支持
 * 4. 更好的性能和安全性
 * 5. 现代化的 Kotlin 特性
 */
object FileToolsV2 {

    // ==================== 向后兼容的常量定义 ====================
    const val BY_NAME_ASC = 0
    const val BY_NAME_DESC = 1
    const val BY_TIME_ASC = 2
    const val BY_TIME_DESC = 3
    const val BY_SIZE_ASC = 4
    const val BY_SIZE_DESC = 5
    const val BY_EXTENSION_ASC = 6
    const val BY_EXTENSION_DESC = 7

    @IntDef(value = [BY_NAME_ASC, BY_NAME_DESC, BY_TIME_ASC, BY_TIME_DESC, BY_SIZE_ASC, BY_SIZE_DESC, BY_EXTENSION_ASC, BY_EXTENSION_DESC])
    @Retention(AnnotationRetention.SOURCE)
    annotation class SortType

    // ==================== 文件创建操作 ====================

    /**
     * 创建指定路径的文件（若不存在），自动创建所有必需的父目录
     */
    @JvmStatic
    fun createFileIfNotExist(root: File, vararg subDirFiles: String): File {
        return FileOperations.createFileIfNotExists(root, *subDirFiles)
            .getOrNull() ?: File(PathUtils.buildPath(root, *subDirFiles))
    }

    @JvmStatic
    fun createFolderIfNotExist(root: File, vararg subDirs: String): File {
        return FileOperations.createDirectoryIfNotExists(root, *subDirs)
            .getOrNull() ?: File(PathUtils.buildPath(root, *subDirs))
    }

    @JvmStatic
    fun createFolderIfNotExist(filePath: String): File {
        return FileOperations.createDirectoryIfNotExists(filePath)
            .getOrNull() ?: File(filePath)
    }

    @JvmStatic
    @Synchronized
    fun createFileIfNotExist(filePath: String): File {
        return FileOperations.createFileIfNotExists(filePath)
            .getOrNull() ?: File(filePath)
    }

    /**
     * 创建文件，如果文件已存在则删除并重新创建
     */
    @JvmStatic
    fun createFileWithReplace(filePath: String): File {
        return FileOperations.createFileWithReplace(filePath)
            .getOrNull() ?: File(filePath)
    }

    // ==================== 路径处理 ====================

    /**
     * 生成完整的文件路径
     */
    @JvmStatic
    fun getPath(rootPath: String, vararg subDirFiles: String): String {
        return PathUtils.buildPath(rootPath, *subDirFiles)
    }

    /**
     * 根据根目录和子目录文件名生成完整路径字符串
     */
    @JvmStatic
    fun getPath(root: File, vararg subDirFiles: String): String {
        return PathUtils.buildPath(root, *subDirFiles)
    }

    @JvmStatic
    fun getSdCardPath(): String {
        return FileOperations.getSdCardPath()
    }

    /**
     * 将目录分隔符统一为平台默认的分隔符，并为目录结尾添加分隔符
     */
    @JvmStatic
    fun separator(path: String): String {
        return PathUtils.normalizePath(path).let { normalized ->
            if (!normalized.endsWith(File.separator)) {
                "$normalized${File.separator}"
            } else {
                normalized
            }
        }
    }

    // ==================== 文件列表操作 ====================

    /**
     * 列出指定目录下的所有子目录
     */
    @JvmStatic
    @JvmOverloads
    fun listDirs(
        startDirPath: String,
        excludeDirs: Array<String>? = null,
        @SortType sortType: Int = BY_NAME_ASC
    ): Array<File> {
        val excludeSet = excludeDirs?.toSet() ?: emptySet()
        val sortStrategy = convertSortType(sortType)
        
        return FileListManager.listDirectories(startDirPath, excludeSet, sortStrategy)
            .getOrNull()?.toTypedArray() ?: arrayOf()
    }

    /**
     * 列出指定目录下的所有子目录及所有文件
     */
    @JvmStatic
    @JvmOverloads
    fun listDirsAndFiles(
        startDirPath: String,
        allowExtensions: Array<String>? = null
    ): Array<File>? {
        val extensionSet = allowExtensions?.toSet()
        return FileListManager.listDirectoriesAndFiles(startDirPath, extensionSet)
            .getOrNull()?.toTypedArray()
    }

    /**
     * 列出指定目录下的所有文件
     */
    @JvmStatic
    @JvmOverloads
    fun listFiles(
        startDirPath: String,
        filterPattern: Pattern? = null,
        @SortType sortType: Int = BY_NAME_ASC
    ): Array<File> {
        val sortStrategy = convertSortType(sortType)
        return FileListManager.listFiles(startDirPath, filterPattern, sortStrategy)
            .getOrNull()?.toTypedArray() ?: arrayOf()
    }

    /**
     * 列出指定目录下的所有文件
     */
    @JvmStatic
    fun listFiles(startDirPath: String, allowExtensions: Array<String>?): Array<File>? {
        val extensionSet = allowExtensions?.toSet()
        return FileListManager.listFilesByExtension(startDirPath, extensionSet)
            .getOrNull()?.toTypedArray()
    }

    /**
     * 列出指定目录下的所有文件
     */
    @JvmStatic
    fun listFiles(startDirPath: String, allowExtension: String?): Array<File>? {
        val extensionSet = allowExtension?.let { setOf(it) }
        return FileListManager.listFilesByExtension(startDirPath, extensionSet)
            .getOrNull()?.toTypedArray()
    }

    // ==================== 文件基础操作 ====================

    /**
     * 判断文件或目录是否存在
     */
    @JvmStatic
    fun exist(path: String): Boolean {
        return FileOperations.exists(path)
    }

    /**
     * 删除文件或目录
     */
    @JvmStatic
    @JvmOverloads
    fun delete(file: File, deleteRootDir: Boolean = false): Boolean {
        return FileOperations.delete(file, deleteRootDir).getOrNull() ?: false
    }

    /**
     * 删除文件或目录
     */
    @JvmStatic
    @JvmOverloads
    fun delete(path: String, deleteRootDir: Boolean = true): Boolean {
        return FileOperations.delete(path, deleteRootDir).getOrNull() ?: false
    }

    /**
     * 复制文件为另一个文件，或复制某目录下的所有文件及目录到另一个目录下
     */
    @JvmStatic
    fun copy(src: String, tar: String): Boolean {
        return FileOperations.copy(src, tar).getOrNull() ?: false
    }

    /**
     * 复制文件或目录
     */
    @JvmStatic
    fun copy(src: File, tar: File): Boolean {
        return FileOperations.copy(src, tar).getOrNull() ?: false
    }

    /**
     * 移动文件或目录
     */
    @JvmStatic
    fun move(src: String, tar: String): Boolean {
        return FileOperations.move(src, tar).getOrNull() ?: false
    }

    /**
     * 移动文件或目录
     */
    @JvmStatic
    fun move(src: File, tar: File): Boolean {
        return FileOperations.move(src, tar).getOrNull() ?: false
    }

    /**
     * 文件重命名
     */
    @JvmStatic
    fun rename(oldPath: String, newPath: String): Boolean {
        return FileOperations.rename(oldPath, newPath).getOrNull() ?: false
    }

    /**
     * 文件重命名
     */
    @JvmStatic
    fun rename(src: File, tar: File): Boolean {
        return FileOperations.rename(src, tar).getOrNull() ?: false
    }

    // ==================== 文件读取操作 ====================

    /**
     * 读取 assets 目录下的文件内容
     */
    @JvmStatic
    @Throws(IOException::class)
    fun readFromAssets(fileName: String): String {
        return FileReader.readFromAssets(fileName).getOrThrow()
    }

    /**
     * 读取文本文件, 失败将返回空串
     */
    @JvmStatic
    @JvmOverloads
    fun readText(filepath: String, charset: String = "utf-8"): String {
        return FileReader.readText(filepath, charset).getOrNull() ?: ""
    }

    /**
     * 读取文件内容, 失败将返回空串
     */
    @JvmStatic
    fun readBytes(filepath: String): ByteArray? {
        return FileReader.readBytes(filepath).getOrNull()
    }

    // ==================== 文件写入操作 ====================

    /**
     * 保存文本内容
     */
    @JvmStatic
    @JvmOverloads
    fun writeText(filepath: String, content: String, charset: String = "utf-8"): Boolean {
        return FileWriter.writeText(filepath, content, charset).getOrNull() != null
    }

    /**
     * 保存文件内容
     */
    @JvmStatic
    fun writeBytes(filepath: String, data: ByteArray): Boolean {
        return FileWriter.writeBytes(filepath, data).getOrNull() != null
    }

    /**
     * 保存文件内容
     */
    @JvmStatic
    fun writeInputStream(filepath: String, data: InputStream): Boolean {
        return FileWriter.writeFromInputStream(filepath, data).getOrNull() != null
    }

    /**
     * 保存文件内容
     */
    @JvmStatic
    fun writeInputStream(file: File, data: InputStream): Boolean {
        return FileWriter.writeFromInputStream(file, data).getOrNull() != null
    }

    /**
     * 追加文本内容
     */
    @JvmStatic
    fun appendText(path: String, content: String): Boolean {
        return FileWriter.appendText(path, content).getOrNull() != null
    }

    // ==================== 文件元数据操作 ====================

    /**
     * 获取文件大小
     */
    @JvmStatic
    fun getLength(path: String): Long {
        return FileOperations.getFileSize(path)
    }

    /**
     * 获取文件或网址的名称（包括后缀）
     */
    @JvmStatic
    fun getName(path: String?): String {
        return if (path == null) "" else FileMetadata.getFileName(path)
    }

    /**
     * 获取文件名（不包括扩展名）
     */
    @JvmStatic
    fun getNameExcludeExtension(path: String): String {
        return FileMetadata.getNameWithoutExtension(path)
    }

    /**
     * 获取格式化后的文件大小
     */
    @JvmStatic
    fun getSize(path: String): String {
        return FileMetadata.getFormattedSize(path)
    }

    /**
     * 获取文件后缀,不包括"."
     */
    @JvmStatic
    fun getExtension(pathOrUrl: String): String {
        return FileMetadata.getExtension(pathOrUrl)
    }

    /**
     * 获取文件的MIME类型
     */
    @JvmStatic
    fun getMimeType(pathOrUrl: String): String {
        return FileMetadata.getMimeType(pathOrUrl)
    }

    /**
     * 获取格式化后的文件/目录创建或最后修改时间
     */
    @JvmStatic
    @JvmOverloads
    fun getDateTime(path: String, format: String = "yyyy年MM月dd日HH:mm"): String {
        return FileMetadata.getFormattedDateTime(path, format)
    }

    /**
     * 获取格式化后的文件/目录创建或最后修改时间
     */
    @JvmStatic
    fun getDateTime(file: File, format: String): String {
        return FileMetadata.getFormattedDateTime(file, format)
    }

    /**
     * 比较两个文件的最后修改时间
     */
    @JvmStatic
    fun compareLastModified(path1: String, path2: String): Int {
        return FileMetadata.compareLastModified(path1, path2)
    }

    /**
     * 创建多级别的目录
     */
    @JvmStatic
    fun makeDirs(path: String): Boolean {
        return FileOperations.createDirectories(path).getOrNull() ?: false
    }

    /**
     * 创建多级别的目录
     */
    @JvmStatic
    fun makeDirs(file: File): Boolean {
        return FileOperations.createDirectories(file.absolutePath).getOrNull() ?: false
    }

    // ==================== 新增的现代化 API ====================

    /**
     * 获取文件详细信息
     */
    @JvmStatic
    fun getFileInfo(filePath: String): FileInfo? {
        return FileMetadata.getFileInfo(filePath).getOrNull()
    }

    /**
     * 获取目录统计信息
     */
    @JvmStatic
    fun getDirectoryStats(dirPath: String, recursive: Boolean = false): DirectoryStats? {
        return FileListManager.getDirectoryStats(dirPath, recursive).getOrNull()
    }

    /**
     * 查找文件
     */
    @JvmStatic
    fun findFiles(
        dirPath: String,
        fileName: String,
        recursive: Boolean = false,
        ignoreCase: Boolean = true
    ): Array<File> {
        return FileListManager.findFiles(dirPath, fileName, recursive, ignoreCase)
            .getOrNull()?.toTypedArray() ?: arrayOf()
    }

    /**
     * 检查文件是否为指定类型
     */
    @JvmStatic
    fun isImage(filePath: String): Boolean = FileMetadata.isImage(filePath)

    @JvmStatic
    fun isVideo(filePath: String): Boolean = FileMetadata.isVideo(filePath)

    @JvmStatic
    fun isAudio(filePath: String): Boolean = FileMetadata.isAudio(filePath)

    @JvmStatic
    fun isText(filePath: String): Boolean = FileMetadata.isText(filePath)

    @JvmStatic
    fun isArchive(filePath: String): Boolean = FileMetadata.isArchive(filePath)

    @JvmStatic
    fun isDocument(filePath: String): Boolean = FileMetadata.isDocument(filePath)

    @JvmStatic
    fun isPdf(filePath: String): Boolean = FileMetadata.isPdf(filePath)

    // ==================== 工具方法 ====================

    /**
     * 转换排序类型
     */
    private fun convertSortType(@SortType sortType: Int): FileSortStrategy {
        return when (sortType) {
            BY_NAME_ASC -> FileSortStrategy.NameAsc
            BY_NAME_DESC -> FileSortStrategy.NameDesc
            BY_TIME_ASC -> FileSortStrategy.TimeAsc
            BY_TIME_DESC -> FileSortStrategy.TimeDesc
            BY_SIZE_ASC -> FileSortStrategy.SizeAsc
            BY_SIZE_DESC -> FileSortStrategy.SizeDesc
            BY_EXTENSION_ASC -> FileSortStrategy.ExtensionAsc
            BY_EXTENSION_DESC -> FileSortStrategy.ExtensionDesc
            else -> FileSortStrategy.NameAsc
        }
    }

    // ==================== 向后兼容的排序器类 ====================

    class SortByExtension : Comparator<File> {
        override fun compare(f1: File?, f2: File?): Int {
            return FileSorter.sort(listOfNotNull(f1, f2), FileSortStrategy.ExtensionAsc)
                .let { sorted ->
                    when {
                        f1 == null && f2 == null -> 0
                        f1 == null -> -1
                        f2 == null -> 1
                        else -> if (sorted.first() == f1) -1 else 1
                    }
                }
        }
    }

    class SortByName @JvmOverloads constructor(private val caseSensitive: Boolean = false) : Comparator<File> {
        override fun compare(f1: File?, f2: File?): Int {
            return when {
                f1 == null && f2 == null -> 0
                f1 == null -> -1
                f2 == null -> 1
                f1.isDirectory && f2.isFile -> -1
                f1.isFile && f2.isDirectory -> 1
                else -> {
                    val s1 = f1.name
                    val s2 = f2.name
                    if (caseSensitive) {
                        s1.cnCompare(s2)
                    } else {
                        s1.compareTo(s2, ignoreCase = true)
                    }
                }
            }
        }
    }

    class SortBySize : Comparator<File> {
        override fun compare(f1: File?, f2: File?): Int {
            return when {
                f1 == null && f2 == null -> 0
                f1 == null -> -1
                f2 == null -> 1
                f1.isDirectory && f2.isFile -> -1
                f1.isFile && f2.isDirectory -> 1
                else -> f1.length().compareTo(f2.length())
            }
        }
    }

    class SortByTime : Comparator<File> {
        override fun compare(f1: File?, f2: File?): Int {
            return when {
                f1 == null && f2 == null -> 0
                f1 == null -> -1
                f2 == null -> 1
                f1.isDirectory && f2.isFile -> -1
                f1.isFile && f2.isDirectory -> 1
                else -> -f1.lastModified().compareTo(f2.lastModified()) // 注意这里是负号，保持原有行为
            }
        }
    }
}