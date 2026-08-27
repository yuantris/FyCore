package io.core.common.util.tools.file

import io.core.common.util.extensions.cool.cnCompare
import java.io.File

/**
 * 文件排序策略
 * 使用密封类替代常量定义，提供类型安全
 */
sealed class FileSortStrategy {
    object NameAsc : FileSortStrategy()
    object NameDesc : FileSortStrategy()
    object TimeAsc : FileSortStrategy()
    object TimeDesc : FileSortStrategy()
    object SizeAsc : FileSortStrategy()
    object SizeDesc : FileSortStrategy()
    object ExtensionAsc : FileSortStrategy()
    object ExtensionDesc : FileSortStrategy()
    data class Custom(val comparator: Comparator<File>) : FileSortStrategy()
}

/**
 * 文件排序器
 * 使用策略模式重构排序逻辑
 */
object FileSorter {
    
    fun sort(files: List<File>, strategy: FileSortStrategy): List<File> {
        val comparator = when (strategy) {
            is FileSortStrategy.NameAsc -> NameComparator(false)
            is FileSortStrategy.NameDesc -> NameComparator(true)
            is FileSortStrategy.TimeAsc -> TimeComparator(false)
            is FileSortStrategy.TimeDesc -> TimeComparator(true)
            is FileSortStrategy.SizeAsc -> SizeComparator(false)
            is FileSortStrategy.SizeDesc -> SizeComparator(true)
            is FileSortStrategy.ExtensionAsc -> ExtensionComparator(false)
            is FileSortStrategy.ExtensionDesc -> ExtensionComparator(true)
            is FileSortStrategy.Custom -> strategy.comparator
        }
        return files.sortedWith(comparator)
    }
    
    /**
     * 按名称排序比较器
     */
    private class NameComparator(private val descending: Boolean) : Comparator<File> {
        override fun compare(f1: File?, f2: File?): Int {
            val result = when {
                f1 == null && f2 == null -> 0
                f1 == null -> -1
                f2 == null -> 1
                f1.isDirectory && f2.isFile -> -1
                f1.isFile && f2.isDirectory -> 1
                else -> f1.name.cnCompare(f2.name)
            }
            return if (descending) -result else result
        }
    }
    
    /**
     * 按时间排序比较器
     */
    private class TimeComparator(private val descending: Boolean) : Comparator<File> {
        override fun compare(f1: File?, f2: File?): Int {
            val result = when {
                f1 == null && f2 == null -> 0
                f1 == null -> -1
                f2 == null -> 1
                f1.isDirectory && f2.isFile -> -1
                f1.isFile && f2.isDirectory -> 1
                else -> f1.lastModified().compareTo(f2.lastModified())
            }
            return if (descending) -result else result
        }
    }
    
    /**
     * 按大小排序比较器
     */
    private class SizeComparator(private val descending: Boolean) : Comparator<File> {
        override fun compare(f1: File?, f2: File?): Int {
            val result = when {
                f1 == null && f2 == null -> 0
                f1 == null -> -1
                f2 == null -> 1
                f1.isDirectory && f2.isFile -> -1
                f1.isFile && f2.isDirectory -> 1
                else -> f1.length().compareTo(f2.length())
            }
            return if (descending) -result else result
        }
    }
    
    /**
     * 按扩展名排序比较器
     */
    private class ExtensionComparator(private val descending: Boolean) : Comparator<File> {
        override fun compare(f1: File?, f2: File?): Int {
            val result = when {
                f1 == null && f2 == null -> 0
                f1 == null -> -1
                f2 == null -> 1
                f1.isDirectory && f2.isFile -> -1
                f1.isFile && f2.isDirectory -> 1
                else -> {
                    val ext1 = f1.extension
                    val ext2 = f2.extension
                    ext1.compareTo(ext2, ignoreCase = true)
                }
            }
            return if (descending) -result else result
        }
    }
}