package io.core.base.constant

import android.webkit.MimeTypeMap
import java.io.File

/**
 * FileType 是一个用于存储文件扩展名与 MIME 类型映射的工具类。
 * 它提供了常见的文件扩展名及其对应的 MIME 类型，便于在文件操作中使用。
 *
 * @author Yuan
 * @since 2025/1/4 13:45
 */
object FileType {

    /**
     * 文件扩展名与 MIME 类型的映射表。
     * 键为文件扩展名（如 ".txt"），值为对应的 MIME 类型列表。
     */
    private val MIME_TYPE_MAP: Map<String, List<String>> = mapOf(
        // 图片类型
        ".bmp" to listOf("image/bmp"),
        ".gif" to listOf("image/gif"),
        ".heic" to listOf("image/heic"),
        ".ico" to listOf("image/x-icon"),
        ".jpeg" to listOf("image/jpeg"),
        ".jpg" to listOf("image/jpeg"),
        ".png" to listOf("image/png"),
        ".psd" to listOf("image/vnd.adobe.photoshop"),
        ".svg" to listOf("image/svg+xml"),
        ".tiff" to listOf("image/tiff"),
        ".webp" to listOf("image/webp"),

        // 视频类型
        ".3gp" to listOf("video/3gpp"),
        ".asf" to listOf("video/x-ms-asf"),
        ".avi" to listOf("video/x-msvideo"),
        ".m4u" to listOf("video/vnd.mpegurl"),
        ".m4v" to listOf("video/x-m4v"),
        ".mkv" to listOf("video/x-matroska", "video/mkv"),
        ".mov" to listOf("video/quicktime"),
        ".mp4" to listOf("video/mp4"),
        ".mpe" to listOf("video/mpeg"),
        ".mpeg" to listOf("video/mpeg"),
        ".mpg" to listOf("video/mpeg"),
        ".mpg4" to listOf("video/mp4"),
        ".rmvb" to listOf("video/vnd.rn-realvideo"),
        ".wmv" to listOf("video/x-ms-wmv"),

        // 音频类型
        ".aac" to listOf("audio/aac"),
        ".flac" to listOf("audio/flac", "audio/x-flac"),
        ".m4a" to listOf("audio/mp4", "audio/m4a", "audio/mp4a-latm"),
        ".m4b" to listOf("audio/mp4a-latm"),
        ".m4p" to listOf("audio/mp4a-latm"),
        ".mid" to listOf("audio/midi"),
        ".midi" to listOf("audio/midi"),
        ".mp2" to listOf("audio/x-mpeg"),
        ".mp3" to listOf("audio/mpeg", "audio/x-mpeg"),
        ".mpga" to listOf("audio/mpeg"),
        ".ogg" to listOf("audio/ogg"),
        ".wav" to listOf("audio/wav", "audio/x-wav", "audio/wave"),
        ".wma" to listOf("audio/x-ms-wma"),

        // 文档类型
        ".csv" to listOf("text/csv"),
        ".doc" to listOf("application/msword"),
        ".docx" to listOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        ".pdf" to listOf("application/pdf"),
        ".ppt" to listOf("application/vnd.ms-powerpoint"),
        ".pptx" to listOf("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
        ".rtf" to listOf("application/rtf"),
        ".txt" to listOf("text/plain"),
        ".epub" to listOf("application/epub+zip"),
        ".xls" to listOf("application/vnd.ms-excel"),
        ".xlsx" to listOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),

        // 压缩文件
        ".7z" to listOf("application/x-7z-compressed"),
        ".dmg" to listOf("application/x-apple-diskimage"),
        ".gz" to listOf("application/x-gzip"),
        ".iso" to listOf("application/x-iso9660-image"),
        ".rar" to listOf("application/x-rar-compressed"),
        ".tar" to listOf("application/x-tar"),
        ".tgz" to listOf("application/x-compressed"),
        ".zip" to listOf("application/zip"),
        ".z" to listOf("application/x-compress"),

        // 可执行文件
        ".apk" to listOf("application/vnd.android.package-archive"),
        ".bin" to listOf("application/octet-stream"),
        ".class" to listOf("application/octet-stream"),
        ".exe" to listOf("application/octet-stream"),
        ".jar" to listOf("application/java-archive"),

        // 编程代码
        ".c" to listOf("text/x-c"),
        ".conf" to listOf("text/plain"),
        ".cpp" to listOf("text/x-c++src"),
        ".go" to listOf("text/x-go"),
        ".h" to listOf("text/x-c-header"),
        ".java" to listOf("text/x-java-source"),
        ".js" to listOf("application/javascript"),
        ".json" to listOf("application/json"),
        ".kt" to listOf("text/x-kotlin"),
        ".log" to listOf("text/plain"),
        ".md" to listOf("text/markdown"),
        ".markdown" to listOf("text/markdown"),
        ".php" to listOf("application/x-httpd-php"),
        ".prop" to listOf("text/plain"),
        ".py" to listOf("text/x-python"),
        ".rc" to listOf("text/plain"),
        ".rs" to listOf("text/rust"),
        ".sh" to listOf("application/x-sh"),
        ".sql" to listOf("application/sql"),
        ".swift" to listOf("text/x-swift"),
        ".ts" to listOf("application/typescript"),
        ".xml" to listOf("application/xml", "text/xml"),
        ".yaml" to listOf("application/yaml"),
        ".yml" to listOf("application/yaml"),

        // 其他
        ".gtar" to listOf("application/x-gtar"),
        ".htm" to listOf("text/html"),
        ".html" to listOf("text/html"),
        ".m3u" to listOf("audio/x-mpegurl"),
        ".mpc" to listOf("application/vnd.mpohun.certificate"),
        ".msg" to listOf("application/vnd.ms-outlook"),
        ".pps" to listOf("application/vnd.ms-powerpoint"),
        ".wps" to listOf("application/vnd.ms-works"),

        // 默认
        "" to listOf("*/*")
    )

    /**
     * 获取只读的文件扩展名与MIME类型映射表（兼容旧版本）
     */
    @JvmStatic
    fun getMimeTypeMap(): Map<String, List<String>> = MIME_TYPE_MAP

    /**
     * 根据文件扩展名获取对应的MIME类型
     * @param extension 文件扩展名（可以带点号如".jpg"，也可以不带如"jpg"）
     * @return 对应的MIME类型
     */
    @JvmStatic
    fun resolveMimeType(extension: String): String {
        // 先尝试自定义映射
        return MIME_TYPE_MAP[normalizeExtension(extension)]?.firstOrNull()
        // 自定义没有则使用系统API
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(normalizeExtension(extension).removePrefix("."))
            // 最后回退到默认值
            ?: "*/*"
    }

    /**
     * 获取文件扩展名对应的所有MIME类型
     * @param extension 文件扩展名（可以带点号如".jpg"，也可以不带如"jpg"）
     * @return 对应的MIME类型列表，如果没有则返回空列表
     */
    @JvmStatic
    fun resolveAllMimeTypes(extension: String): List<String> {
        return MIME_TYPE_MAP[normalizeExtension(extension)].orEmpty()
    }

    /**
     * 根据MIME类型获取所有对应的文件扩展名
     * @param mimeType MIME类型（如"image/jpeg"）
     * @return 对应的文件扩展名列表（如[".jpg", ".jpeg"]）
     */
    @JvmStatic
    fun resolveExtensions(mimeType: String): List<String> {
        return MIME_TYPE_MAP.entries
            .filter { it.value.any { v -> v.equals(mimeType, ignoreCase = true) } }
            .map { it.key }
    }


    /**
     * 根据文件获取对应的MIME类型
     * @param file 文件
     * @return 对应的MIME类型
     */
    @JvmStatic
    fun mimeTypeOf(file: File): String = mimeTypeOf(file.name)

    /**
     * 根据文件名或文件路径获取对应的MIME类型
     * @param fileNameOrPath 文件名（如"test.jpg"）或完整路径（如"/path/to/test.jpg"）
     * @return 对应的MIME类型
     */
    @JvmStatic
    fun mimeTypeOf(fileNameOrPath: String): String {
        val extension = fileNameOrPath.substringAfterLast('.', "")
        return resolveMimeType(extension)
    }

    /**
     * 检查扩展名是否有对应的MIME类型
     */
    @JvmStatic
    fun isExtSupported(extension: String): Boolean {
        return MIME_TYPE_MAP.containsKey(normalizeExtension(extension))
    }

    /**
     * 规范化文件扩展名（确保带点号且小写）
     * @param extension 原始扩展名
     * @return 规范化后的扩展名（如".jpg"）
     */
    private fun normalizeExtension(extension: String): String {
        return if (extension.isNotEmpty() && !extension.startsWith('.')) {
            ".${extension.lowercase()}"
        } else {
            extension.lowercase()
        }
    }
}
