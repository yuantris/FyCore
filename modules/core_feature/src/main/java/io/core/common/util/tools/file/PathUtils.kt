package io.core.common.util.tools.file

import java.io.File
import java.security.MessageDigest

/**
 * 路径处理工具类
 * 提供安全的路径操作和验证功能
 * 兼容 Android API 24+
 */
object PathUtils {
    
    private const val MAX_PATH_LENGTH = 4096
    private val INVALID_CHARS = charArrayOf('<', '>', ':', '"', '|', '?', '*', '\u0000')
    private val RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )
    
    /**
     * 安全地构建路径
     * 自动处理路径分隔符和规范化
     */
    fun buildPath(root: String, vararg segments: String): String {
        if (segments.isEmpty()) return normalizePath(root)
        
        val path = StringBuilder(root)
        segments.forEach { segment ->
            if (segment.isNotEmpty()) {
                if (!path.endsWith(File.separator)) {
                    path.append(File.separator)
                }
                path.append(sanitizePathSegment(segment))
            }
        }
        return normalizePath(path.toString())
    }
    
    /**
     * 构建路径（File 版本）
     */
    fun buildPath(root: File, vararg segments: String): String {
        return buildPath(root.absolutePath, *segments)
    }
    
    /**
     * 规范化路径
     * 处理 . 和 .. 以及多余的分隔符
     * 使用传统方法兼容 API 24
     */
    fun normalizePath(path: String): String {
        return try {
            // 使用 File 类进行路径规范化，兼容 API 24
            val file = File(path)
            file.canonicalPath
        } catch (e: Exception) {
            // 降级到手动处理
            normalizePathManually(path)
        }
    }
    
    /**
     * 手动规范化路径
     */
    private fun normalizePathManually(path: String): String {
        // 统一分隔符
        var normalized = path.replace("\\", File.separator)
            .replace("/", File.separator)
        
        // 处理多个连续分隔符
        while (normalized.contains("${File.separator}${File.separator}")) {
            normalized = normalized.replace("${File.separator}${File.separator}", File.separator)
        }
        
        // 处理 . 和 .. 
        val parts = normalized.split(File.separator).toMutableList()
        val result = mutableListOf<String>()
        
        for (part in parts) {
            when (part) {
                "", "." -> continue
                ".." -> {
                    if (result.isNotEmpty() && result.last() != "..") {
                        result.removeLastOrNull()
                    } else if (!isAbsolutePathString(normalized)) {
                        result.add(part)
                    }
                }
                else -> result.add(part)
            }
        }
        
        val finalPath = if (isAbsolutePathString(normalized)) {
            File.separator + result.joinToString(File.separator)
        } else {
            result.joinToString(File.separator)
        }
        
        return if (finalPath.isEmpty()) "." else finalPath
    }
    
    /**
     * 检查字符串是否表示绝对路径
     */
    private fun isAbsolutePathString(path: String): Boolean {
        return path.startsWith(File.separator) || 
               (path.length >= 2 && path[1] == ':') // Windows 驱动器路径
    }
    
    /**
     * 验证路径是否安全
     * 防止路径遍历攻击
     */
    fun isPathSafe(path: String, baseDir: String? = null): Boolean {
        return try {
            val normalizedPath = normalizePath(path)
            
            // 检查路径长度
            if (normalizedPath.length > MAX_PATH_LENGTH) return false
            
            // 检查是否包含非法字符
            if (normalizedPath.any { it in INVALID_CHARS }) return false
            
            // 检查路径遍历
            if (normalizedPath.contains("..")) return false
            
            // 检查保留名称
            val fileName = File(normalizedPath).name.uppercase()
            if (fileName in RESERVED_NAMES) return false
            
            // 如果指定了基础目录，检查是否在允许范围内
            baseDir?.let { base ->
                val basePath = File(normalizePath(base)).absolutePath
                val targetPath = File(normalizedPath).absolutePath
                if (!targetPath.startsWith(basePath)) return false
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 清理路径段，移除危险字符
     */
    private fun sanitizePathSegment(segment: String): String {
        return segment.filterNot { it in INVALID_CHARS }
            .trim()
            .takeIf { it.isNotEmpty() } ?: "unnamed"
    }
    
    /**
     * 获取文件扩展名（不包含点）
     */
    fun getExtension(path: String): String {
        val fileName = File(path).name
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1).lowercase()
        } else {
            ""
        }
    }
    
    /**
     * 获取文件名（不包含扩展名）
     */
    fun getNameWithoutExtension(path: String): String {
        val fileName = File(path).name
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0) {
            fileName.substring(0, dotIndex)
        } else {
            fileName
        }
    }
    
    /**
     * 生成唯一文件名
     * 如果文件已存在，自动添加数字后缀
     */
    fun generateUniqueFileName(directory: String, baseName: String, extension: String = ""): String {
        val ext = if (extension.isNotEmpty() && !extension.startsWith(".")) ".$extension" else extension
        var fileName = "$baseName$ext"
        var counter = 1
        
        while (File(directory, fileName).exists()) {
            fileName = "${baseName}_$counter$ext"
            counter++
        }
        
        return fileName
    }
    
    /**
     * 计算文件路径的哈希值
     * 用于缓存键或文件标识
     */
    fun getPathHash(path: String): String {
        val normalizedPath = normalizePath(path)
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(normalizedPath.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 检查路径是否为绝对路径
     * 兼容 API 24 的实现
     */
    fun isAbsolutePath(path: String): Boolean {
        return try {
            File(path).isAbsolute
        } catch (e: Exception) {
            isAbsolutePathString(path)
        }
    }
    
    /**
     * 将相对路径转换为绝对路径
     */
    fun toAbsolutePath(path: String, basePath: String = System.getProperty("user.dir")): String {
        return if (isAbsolutePath(path)) {
            normalizePath(path)
        } else {
            buildPath(basePath, path)
        }
    }
}