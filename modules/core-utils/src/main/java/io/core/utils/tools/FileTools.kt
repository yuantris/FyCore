package io.core.utils.tools

import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.annotation.IntDef
import io.core.base.appCtx
import io.core.utils.extensions.cool.cnCompare
import io.core.utils.extensions.cool.printOnDebug
import io.core.utils.extensions.currentTimeMillis
import io.core.utils.tools.file.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.regex.Pattern

/**
 * 文件工具�?- 重构版本
 * 
 * 这是原始 FileTools 的重构版本，保持完全的向后兼容�?
 * 内部实现已经重构为模块化架构，提供更好的性能、安全性和可维护�?
 * 
 * 重构改进�?
 * 1. 模块化设计：将功能拆分为 FileOperations、FileReader、FileWriter、PathUtils、FileListManager、FileMetadata 等模�?
 * 2. 类型安全：使�?FileOperationResult 封装操作结果，提供更好的错误处理
 * 3. 现代化特性：支持协程、扩展函数、密封类�?Kotlin 特�?
 * 4. 性能优化：使�?NIO、优化缓冲区、减少内存分�?
 * 5. 安全增强：路径验证、权限检查、防止路径遍历攻�?
 * 
 * 使用建议�?
 * - 新代码推荐直接使�?FileToolsV2 或具体的模块�?
 * - 现有代码可以继续使用此类，无需修改
 * - 逐步迁移到新 API 以获得更好的性能和功�?
 */
object FileTools {

    // ==================== 向后兼容的常量定�?====================
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

    // ==================== 委托�?FileToolsV2 的实�?====================

    /**
     * 创建指定路径的文件（若不存在），自动创建所有必需的父目录
     */
    @JvmStatic
    fun createFileIfNotExist(root: File, vararg subDirFiles: String): File {
        return FileToolsV2.createFileIfNotExist(root, *subDirFiles)
    }

    @JvmStatic
    fun createFolderIfNotExist(root: File, vararg subDirs: String): File {
        return FileToolsV2.createFolderIfNotExist(root, *subDirs)
    }

    @JvmStatic
    fun createFolderIfNotExist(filePath: String): File {
        return FileToolsV2.createFolderIfNotExist(filePath)
    }

    @JvmStatic
    @Synchronized
    fun createFileIfNotExist(filePath: String): File {
        return FileToolsV2.createFileIfNotExist(filePath)
    }

    /**
     * 创建文件，如果文件已存在则删除并重新创建
     */
    @JvmStatic
    fun createFileWithReplace(filePath: String): File {
        return FileToolsV2.createFileWithReplace(filePath)
    }

    /**
     * 生成完整的文件路�?
     */
    @JvmStatic
    fun getPath(rootPath: String, vararg subDirFiles: String): String {
        return FileToolsV2.getPath(rootPath, *subDirFiles)
    }

    /**
     * 根据根目录和子目录文件名生成完整路径字符�?
     */
    @JvmStatic
    fun getPath(root: File, vararg subDirFiles: String): String {
        return FileToolsV2.getPath(root, *subDirFiles)
    }

    @JvmStatic
    fun getSdCardPath(): String {
        return FileToolsV2.getSdCardPath()
    }

    /**
     * 将目录分隔符统一为平台默认的分隔符，并为目录结尾添加分隔�?
     */
    @JvmStatic
    fun separator(path: String): String {
        return FileToolsV2.separator(path)
    }

    /**
     * 列出指定目录下的所有子目录
     */
    @JvmStatic
    @JvmOverloads
    fun listDirs(
        startDirPath: String,
        excludeDirs: Array<String>? = null, @SortType sortType: Int = BY_NAME_ASC
    ): Array<File> {
        return FileToolsV2.listDirs(startDirPath, excludeDirs, sortType)
    }

    /**
     * 列出指定目录下的所有子目录及所有文�?
     */
    @JvmStatic
    @JvmOverloads
    fun listDirsAndFiles(
        startDirPath: String,
        allowExtensions: Array<String>? = null
    ): Array<File>? {
        return FileToolsV2.listDirsAndFiles(startDirPath, allowExtensions)
    }

    /**
     * 列出指定目录下的所有文�?
     */
    @JvmStatic
    @JvmOverloads
    fun listFiles(
        startDirPath: String,
        filterPattern: Pattern? = null, @SortType sortType: Int = BY_NAME_ASC
    ): Array<File> {
        return FileToolsV2.listFiles(startDirPath, filterPattern, sortType)
    }

    /**
     * 列出指定目录下的所有文�?
     */
    @JvmStatic
    fun listFiles(startDirPath: String, allowExtensions: Array<String>?): Array<File>? {
        return FileToolsV2.listFiles(startDirPath, allowExtensions)
    }

    /**
     * 列出指定目录下的所有文�?
     */
    @JvmStatic
    fun listFiles(startDirPath: String, allowExtension: String?): Array<File>? {
        return FileToolsV2.listFiles(startDirPath, allowExtension)
    }

    /**
     * 判断文件或目录是否存�?
     */
    @JvmStatic
    fun exist(path: String): Boolean {
        return FileToolsV2.exist(path)
    }

    /**
     * 删除文件或目�?
     */
    @JvmStatic
    @JvmOverloads
    fun delete(file: File, deleteRootDir: Boolean = false): Boolean {
        return FileToolsV2.delete(file, deleteRootDir)
    }

    /**
     * 删除文件或目�?
     */
    @JvmStatic
    @JvmOverloads
    fun delete(path: String, deleteRootDir: Boolean = true): Boolean {
        return FileToolsV2.delete(path, deleteRootDir)
    }

    /**
     * 复制文件为另一个文件，或复制某目录下的所有文件及目录到另一个目录下
     */
    @JvmStatic
    fun copy(src: String, tar: String): Boolean {
        return FileToolsV2.copy(src, tar)
    }

    /**
     * 复制文件或目�?
     */
    @JvmStatic
    fun copy(src: File, tar: File): Boolean {
        return FileToolsV2.copy(src, tar)
    }

    /**
     * 移动文件或目�?
     */
    @JvmStatic
    fun move(src: String, tar: String): Boolean {
        return FileToolsV2.move(src, tar)
    }

    /**
     * 移动文件或目�?
     */
    @JvmStatic
    fun move(src: File, tar: File): Boolean {
        return FileToolsV2.move(src, tar)
    }

    /**
     * 文件重命�?
     */
    @JvmStatic
    fun rename(oldPath: String, newPath: String): Boolean {
        return FileToolsV2.rename(oldPath, newPath)
    }

    /**
     * 文件重命�?
     */
    @JvmStatic
    fun rename(src: File, tar: File): Boolean {
        return FileToolsV2.rename(src, tar)
    }

    /**
     * 读取 assets 目录下的文件内容
     */
    @JvmStatic
    @Throws(IOException::class)
    fun readFromAssets(fileName: String): String {
        return FileToolsV2.readFromAssets(fileName)
    }

    /**
     * 读取文本文件, 失败将返回空�?
     */
    @JvmStatic
    @JvmOverloads
    fun readText(filepath: String, charset: String = "utf-8"): String {
        return FileToolsV2.readText(filepath, charset)
    }

    /**
     * 读取文件内容, 失败将返回空�?
     */
    @JvmStatic
    fun readBytes(filepath: String): ByteArray? {
        return FileToolsV2.readBytes(filepath)
    }

    /**
     * 保存文本内容
     */
    @JvmStatic
    @JvmOverloads
    fun writeText(filepath: String, content: String, charset: String = "utf-8"): Boolean {
        return FileToolsV2.writeText(filepath, content, charset)
    }

    /**
     * 保存文件内容
     */
    @JvmStatic
    fun writeBytes(filepath: String, data: ByteArray): Boolean {
        return FileToolsV2.writeBytes(filepath, data)
    }

    /**
     * 保存文件内容
     */
    @JvmStatic
    fun writeInputStream(filepath: String, data: InputStream): Boolean {
        return FileToolsV2.writeInputStream(filepath, data)
    }

    /**
     * 保存文件内容
     */
    @JvmStatic
    fun writeInputStream(file: File, data: InputStream): Boolean {
        return FileToolsV2.writeInputStream(file, data)
    }

    /**
     * 追加文本内容
     */
    @JvmStatic
    fun appendText(path: String, content: String): Boolean {
        return FileToolsV2.appendText(path, content)
    }

    /**
     * 获取文件大小
     */
    @JvmStatic
    fun getLength(path: String): Long {
        return FileToolsV2.getLength(path)
    }

    /**
     * 获取文件或网址的名称（包括后缀�?
     */
    @JvmStatic
    fun getName(path: String?): String {
        return FileToolsV2.getName(path)
    }

    /**
     * 获取文件名（不包括扩展名�?
     */
    @JvmStatic
    fun getNameExcludeExtension(path: String): String {
        return FileToolsV2.getNameExcludeExtension(path)
    }

    /**
     * 获取格式化后的文件大�?
     */
    @JvmStatic
    fun getSize(path: String): String {
        return FileToolsV2.getSize(path)
    }

    /**
     * 获取文件后缀,不包�?."
     */
    @JvmStatic
    fun getExtension(pathOrUrl: String): String {
        return FileToolsV2.getExtension(pathOrUrl)
    }

    /**
     * 获取文件的MIME类型
     */
    @JvmStatic
    fun getMimeType(pathOrUrl: String): String {
        return FileToolsV2.getMimeType(pathOrUrl)
    }

    /**
     * 获取格式化后的文�?目录创建或最后修改时�?
     */
    @JvmStatic
    @JvmOverloads
    fun getDateTime(path: String, format: String = "yyyy年MM月dd日HH:mm"): String {
        return FileToolsV2.getDateTime(path, format)
    }

    /**
     * 获取格式化后的文�?目录创建或最后修改时�?
     */
    @JvmStatic
    fun getDateTime(file: File, format: String): String {
        return FileToolsV2.getDateTime(file, format)
    }

    /**
     * 比较两个文件的最后修改时�?
     */
    @JvmStatic
    fun compareLastModified(path1: String, path2: String): Int {
        return FileToolsV2.compareLastModified(path1, path2)
    }

    /**
     * 创建多级别的目录
     */
    @JvmStatic
    fun makeDirs(path: String): Boolean {
        return FileToolsV2.makeDirs(path)
    }

    /**
     * 创建多级别的目录
     */
    @JvmStatic
    fun makeDirs(file: File): Boolean {
        return FileToolsV2.makeDirs(file)
    }

    // ==================== 保持原有的排序器�?====================

    class SortByExtension : Comparator<File> {
        override fun compare(f1: File?, f2: File?): Int {
            return FileToolsV2.SortByExtension().compare(f1, f2)
        }
    }

    class SortByName : Comparator<File> {
        private var caseSensitive: Boolean = false

        constructor(caseSensitive: Boolean) {
            this.caseSensitive = caseSensitive
        }

        constructor() {
            this.caseSensitive = false
        }

        override fun compare(f1: File?, f2: File?): Int {
            return FileToolsV2.SortByName(caseSensitive).compare(f1, f2)
        }
    }

    class SortBySize : Comparator<File> {
        override fun compare(f1: File?, f2: File?): Int {
            return FileToolsV2.SortBySize().compare(f1, f2)
        }
    }

    class SortByTime : Comparator<File> {
        override fun compare(f1: File?, f2: File?): Int {
            return FileToolsV2.SortByTime().compare(f1, f2)
        }
    }
}