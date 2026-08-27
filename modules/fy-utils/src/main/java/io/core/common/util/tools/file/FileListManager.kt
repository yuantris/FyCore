package io.core.common.util.tools.file

import java.io.File
import java.io.FileFilter
import java.util.regex.Pattern

/**
 * 文件列表管理器
 * 提供文件和目录的列表、过滤、排序功能
 */
object FileListManager {
    
    /**
     * 列出目录下的所有子目录
     * @param dirPath 目录路径
     * @param excludeDirs 排除的目录名列表
     * @param sortStrategy 排序策略
     * @return 目录文件数组
     */
    fun listDirectories(
        dirPath: String,
        excludeDirs: Set<String> = emptySet(),
        sortStrategy: FileSortStrategy = FileSortStrategy.NameAsc
    ): FileOperationResult<List<File>> = safeFileOperation {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            throw IllegalArgumentException("Directory does not exist or is not a directory: $dirPath")
        }
        
        val directories = dir.listFiles(DirectoryFilter(excludeDirs))?.toList() ?: emptyList()
        FileSorter.sort(directories, sortStrategy)
    }
    
    /**
     * 列出目录下的所有文件
     * @param dirPath 目录路径
     * @param filterPattern 文件名过滤正则表达式
     * @param sortStrategy 排序策略
     * @return 文件数组
     */
    fun listFiles(
        dirPath: String,
        filterPattern: Pattern? = null,
        sortStrategy: FileSortStrategy = FileSortStrategy.NameAsc
    ): FileOperationResult<List<File>> = safeFileOperation {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            throw IllegalArgumentException("Directory does not exist or is not a directory: $dirPath")
        }
        
        val files = dir.listFiles(FilePatternFilter(filterPattern))?.toList() ?: emptyList()
        FileSorter.sort(files, sortStrategy)
    }
    
    /**
     * 列出目录下指定扩展名的文件
     * @param dirPath 目录路径
     * @param allowedExtensions 允许的扩展名列表，null 表示所有文件
     * @param sortStrategy 排序策略
     * @return 文件数组
     */
    fun listFilesByExtension(
        dirPath: String,
        allowedExtensions: Set<String>? = null,
        sortStrategy: FileSortStrategy = FileSortStrategy.NameAsc
    ): FileOperationResult<List<File>> = safeFileOperation {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            throw IllegalArgumentException("Directory does not exist or is not a directory: $dirPath")
        }
        
        val files = dir.listFiles(ExtensionFilter(allowedExtensions))?.toList() ?: emptyList()
        FileSorter.sort(files, sortStrategy)
    }
    
    /**
     * 列出目录下的所有文件和子目录
     * @param dirPath 目录路径
     * @param allowedExtensions 允许的文件扩展名，null 表示所有文件
     * @param sortStrategy 排序策略
     * @return 文件和目录数组
     */
    fun listDirectoriesAndFiles(
        dirPath: String,
        allowedExtensions: Set<String>? = null,
        sortStrategy: FileSortStrategy = FileSortStrategy.NameAsc
    ): FileOperationResult<List<File>> = safeFileOperation {
        val directories = listDirectories(dirPath, sortStrategy = sortStrategy).getOrThrow()
        val files = listFilesByExtension(dirPath, allowedExtensions, sortStrategy).getOrThrow()
        directories + files
    }
    
    /**
     * 递归列出目录下的所有文件
     * @param dirPath 目录路径
     * @param maxDepth 最大递归深度，-1 表示无限制
     * @param filterPattern 文件名过滤正则表达式
     * @param sortStrategy 排序策略
     * @return 文件列表
     */
    fun listFilesRecursively(
        dirPath: String,
        maxDepth: Int = -1,
        filterPattern: Pattern? = null,
        sortStrategy: FileSortStrategy = FileSortStrategy.NameAsc
    ): FileOperationResult<List<File>> = safeFileOperation {
        val result = mutableListOf<File>()
        collectFilesRecursively(File(dirPath), result, 0, maxDepth, filterPattern)
        FileSorter.sort(result, sortStrategy)
    }
    
    /**
     * 递归收集文件
     */
    private fun collectFilesRecursively(
        dir: File,
        result: MutableList<File>,
        currentDepth: Int,
        maxDepth: Int,
        filterPattern: Pattern?
    ) {
        if (maxDepth >= 0 && currentDepth > maxDepth) return
        if (!dir.exists() || !dir.isDirectory) return
        
        dir.listFiles()?.forEach { file ->
            when {
                file.isFile -> {
                    val shouldInclude = filterPattern?.matcher(file.name)?.find() ?: true
                    if (shouldInclude) {
                        result.add(file)
                    }
                }
                file.isDirectory -> {
                    collectFilesRecursively(file, result, currentDepth + 1, maxDepth, filterPattern)
                }
            }
        }
    }
    
    /**
     * 递归列出目录下的所有目录
     * @param dirPath 目录路径
     * @param maxDepth 最大递归深度，-1 表示无限制
     * @param excludeDirs 排除的目录名列表
     * @param sortStrategy 排序策略
     * @return 目录列表
     */
    fun listDirectoriesRecursively(
        dirPath: String,
        maxDepth: Int = -1,
        excludeDirs: Set<String> = emptySet(),
        sortStrategy: FileSortStrategy = FileSortStrategy.NameAsc
    ): FileOperationResult<List<File>> = safeFileOperation {
        val result = mutableListOf<File>()
        collectDirectoriesRecursively(File(dirPath), result, 0, maxDepth, excludeDirs)
        FileSorter.sort(result, sortStrategy)
    }
    
    /**
     * 递归收集目录
     */
    private fun collectDirectoriesRecursively(
        dir: File,
        result: MutableList<File>,
        currentDepth: Int,
        maxDepth: Int,
        excludeDirs: Set<String>
    ) {
        if (maxDepth >= 0 && currentDepth > maxDepth) return
        if (!dir.exists() || !dir.isDirectory) return
        
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.name !in excludeDirs) {
                result.add(file)
                collectDirectoriesRecursively(file, result, currentDepth + 1, maxDepth, excludeDirs)
            }
        }
    }
    
    /**
     * 查找文件
     * @param dirPath 搜索目录
     * @param fileName 文件名（支持通配符 * 和 ?）
     * @param recursive 是否递归搜索
     * @param ignoreCase 是否忽略大小写
     * @return 找到的文件列表
     */
    fun findFiles(
        dirPath: String,
        fileName: String,
        recursive: Boolean = false,
        ignoreCase: Boolean = true
    ): FileOperationResult<List<File>> = safeFileOperation {
        val pattern = createWildcardPattern(fileName, ignoreCase)
        
        if (recursive) {
            listFilesRecursively(dirPath, filterPattern = pattern).getOrThrow()
        } else {
            listFiles(dirPath, filterPattern = pattern).getOrThrow()
        }
    }
    
    /**
     * 查找目录
     * @param dirPath 搜索目录
     * @param dirName 目录名（支持通配符 * 和 ?）
     * @param recursive 是否递归搜索
     * @param ignoreCase 是否忽略大小写
     * @return 找到的目录列表
     */
    fun findDirectories(
        dirPath: String,
        dirName: String,
        recursive: Boolean = false,
        ignoreCase: Boolean = true
    ): FileOperationResult<List<File>> = safeFileOperation {
        val pattern = createWildcardPattern(dirName, ignoreCase)
        val filter = DirectoryPatternFilter(pattern)
        
        val result = mutableListOf<File>()
        if (recursive) {
            collectDirectoriesWithPattern(File(dirPath), result, filter, 0, -1)
        } else {
            File(dirPath).listFiles(filter)?.let { result.addAll(it) }
        }
        result
    }
    
    /**
     * 使用模式收集目录
     */
    private fun collectDirectoriesWithPattern(
        dir: File,
        result: MutableList<File>,
        filter: FileFilter,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (maxDepth >= 0 && currentDepth > maxDepth) return
        if (!dir.exists() || !dir.isDirectory) return
        
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (filter.accept(file)) {
                    result.add(file)
                }
                collectDirectoriesWithPattern(file, result, filter, currentDepth + 1, maxDepth)
            }
        }
    }
    
    /**
     * 创建通配符模式
     */
    private fun createWildcardPattern(pattern: String, ignoreCase: Boolean): Pattern {
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        
        val flags = if (ignoreCase) Pattern.CASE_INSENSITIVE else 0
        return Pattern.compile(regexPattern, flags)
    }
    
    /**
     * 获取目录统计信息
     * @param dirPath 目录路径
     * @param recursive 是否递归统计
     * @return 目录统计信息
     */
    fun getDirectoryStats(dirPath: String, recursive: Boolean = false): FileOperationResult<DirectoryStats> = safeFileOperation {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            throw IllegalArgumentException("Directory does not exist or is not a directory: $dirPath")
        }
        
        if (recursive) {
            calculateStatsRecursively(dir)
        } else {
            calculateStatsShallow(dir)
        }
    }
    
    /**
     * 浅层统计目录信息
     */
    private fun calculateStatsShallow(dir: File): DirectoryStats {
        var fileCount = 0
        var dirCount = 0
        var totalSize = 0L
        
        dir.listFiles()?.forEach { file ->
            when {
                file.isFile -> {
                    fileCount++
                    totalSize += file.length()
                }
                file.isDirectory -> dirCount++
            }
        }
        
        return DirectoryStats(fileCount, dirCount, totalSize)
    }
    
    /**
     * 递归统计目录信息
     */
    private fun calculateStatsRecursively(dir: File): DirectoryStats {
        var fileCount = 0
        var dirCount = 0
        var totalSize = 0L
        
        fun traverse(currentDir: File) {
            currentDir.listFiles()?.forEach { file ->
                when {
                    file.isFile -> {
                        fileCount++
                        totalSize += file.length()
                    }
                    file.isDirectory -> {
                        dirCount++
                        traverse(file)
                    }
                }
            }
        }
        
        traverse(dir)
        return DirectoryStats(fileCount, dirCount, totalSize)
    }
}

/**
 * 目录统计信息
 */
data class DirectoryStats(
    val fileCount: Int,
    val directoryCount: Int,
    val totalSize: Long
)

/**
 * 目录过滤器
 */
private class DirectoryFilter(private val excludeDirs: Set<String>) : FileFilter {
    override fun accept(file: File): Boolean {
        return file.isDirectory && file.name !in excludeDirs
    }
}

/**
 * 文件模式过滤器
 */
private class FilePatternFilter(private val pattern: Pattern?) : FileFilter {
    override fun accept(file: File): Boolean {
        if (!file.isFile) return false
        return pattern?.matcher(file.name)?.find() ?: true
    }
}

/**
 * 目录模式过滤器
 */
private class DirectoryPatternFilter(private val pattern: Pattern?) : FileFilter {
    override fun accept(file: File): Boolean {
        if (!file.isDirectory) return false
        return pattern?.matcher(file.name)?.find() ?: true
    }
}

/**
 * 扩展名过滤器
 */
private class ExtensionFilter(private val allowedExtensions: Set<String>?) : FileFilter {
    override fun accept(file: File): Boolean {
        if (!file.isFile) return false
        if (allowedExtensions == null) return true
        
        val extension = PathUtils.getExtension(file.name)
        return extension in allowedExtensions
    }
}