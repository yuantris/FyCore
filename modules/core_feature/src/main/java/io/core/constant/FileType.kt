package io.core.constant

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
     * 键为文件扩展名（如 ".txt"），值为对应的 MIME 类型（如 "text/plain"）。
     */
    val MIME_TYPE_MAP = mapOf(
        // 图片类型
        ".bmp" to "image/bmp",
        ".gif" to "image/gif",
        ".heic" to "image/heic",
        ".ico" to "image/x-icon",
        ".jpeg" to "image/jpeg",
        ".jpg" to "image/jpeg",
        ".png" to "image/png",
        ".psd" to "image/vnd.adobe.photoshop",
        ".svg" to "image/svg+xml",
        ".tiff" to "image/tiff",
        ".webp" to "image/webp",
        
        // 视频类型
        ".3gp" to "video/3gpp",
        ".asf" to "video/x-ms-asf",
        ".avi" to "video/x-msvideo",
        ".m4u" to "video/vnd.mpegurl",
        ".m4v" to "video/x-m4v",
        ".mov" to "video/quicktime",
        ".mp4" to "video/mp4",
        ".mpe" to "video/mpeg",
        ".mpeg" to "video/mpeg",
        ".mpg" to "video/mpeg",
        ".mpg4" to "video/mp4",
        ".rmvb" to "video/vnd.rn-realvideo",
        ".wmv" to "video/x-ms-wmv",
        
        // 音频类型
        ".aac" to "audio/aac",
        ".flac" to "audio/flac",
        ".m4a" to "audio/mp4a-latm",
        ".m4b" to "audio/mp4a-latm",
        ".m4p" to "audio/mp4a-latm",
        ".mid" to "audio/midi",
        ".midi" to "audio/midi",
        ".mp2" to "audio/x-mpeg",
        ".mp3" to "audio/x-mpeg",
        ".mpga" to "audio/mpeg",
        ".ogg" to "audio/ogg",
        ".wav" to "audio/x-wav",
        ".wma" to "audio/x-ms-wma",
        
        // 文档类型
        ".csv" to "text/csv",
        ".doc" to "application/msword",
        ".docx" to "application/msword",
        ".pdf" to "application/pdf",
        ".ppt" to "application/vnd.ms-powerpoint",
        ".pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        ".rtf" to "application/rtf",
        ".txt" to "text/plain",
        ".xls" to "application/vnd.ms-excel",
        ".xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        
        // 压缩文件
        ".7z" to "application/x-7z-compressed",
        ".dmg" to "application/x-apple-diskimage",
        ".gz" to "application/x-gzip",
        ".iso" to "application/x-iso9660-image",
        ".rar" to "application/x-rar-compressed",
        ".tar" to "application/x-tar",
        ".tgz" to "application/x-compressed",
        ".zip" to "application/zip",
        ".z" to "application/x-compress",
        
        // 可执行文件
        ".apk" to "application/vnd.android.package-archive",
        ".bin" to "application/octet-stream",
        ".class" to "application/octet-stream",
        ".exe" to "application/octet-stream",
        ".jar" to "application/java-archive",
        
        // 编程代码
        ".c" to "text/plain",
        ".conf" to "text/plain",
        ".cpp" to "text/plain",
        ".go" to "text/plain",
        ".h" to "text/plain",
        ".java" to "text/plain",
        ".js" to "application/x-javascript",
        ".json" to "application/json",
        ".kt" to "text/plain",
        ".log" to "text/plain",
        ".md" to "text/markdown",
        ".markdown" to "text/markdown",
        ".php" to "application/x-httpd-php", 
        ".prop" to "text/plain",
        ".py" to "text/x-python",
        ".rc" to "text/plain",
        ".rs" to "text/plain",
        ".sh" to "text/plain",
        ".sql" to "application/sql",
        ".swift" to "text/plain",
        ".ts" to "application/typescript",
        ".xml" to "text/plain",
        ".yaml" to "text/yaml",
        ".yml" to "text/yaml",
        
        // 其他
        ".gtar" to "application/x-gtar",
        ".htm" to "text/html",
        ".html" to "text/html",
        ".m3u" to "audio/x-mpegurl",
        ".m4u" to "video/vnd.mpegurl",
        ".mpc" to "application/vnd.mpohun.certificate",
        ".msg" to "application/vnd.ms-outlook",
        ".pps" to "application/vnd.ms-powerpoint",
        ".wps" to "application/vnd.ms-works",
        
        // 默认
        "" to "*/*"
    )

    /**
     * 根据文件扩展名获取对应的MIME类型
     * @param extension 文件扩展名（可以带点号如".jpg"，也可以不带如"jpg"）
     * @return 对应的MIME类型
     */
    fun getMimeType(extension: String): String {
        return MIME_TYPE_MAP[normalizeExtension(extension)] ?: "*/*"
    }

    /**
     * 根据文件名或文件路径获取对应的MIME类型
     * @param fileNameOrPath 文件名（如"test.jpg"）或完整路径（如"/path/to/test.jpg"）
     * @return 对应的MIME类型
     */
    fun getMimeTypeFromFile(fileNameOrPath: String): String {
        val lastDotIndex = fileNameOrPath.lastIndexOf('.')
        return getMimeType(
            if (lastDotIndex != -1) {
                fileNameOrPath.substring(lastDotIndex).lowercase()
            } else ""
        )
    }

    /**
     * 根据MIME类型获取所有对应的文件扩展名
     * @param mimeType MIME类型（如"image/jpeg"）
     * @return 对应的文件扩展名列表（如[".jpg", ".jpeg"]）
     */
    fun getExtensionsByMimeType(mimeType: String): List<String> {
        return MIME_TYPE_MAP.entries
            .filter { it.value.equals(mimeType, ignoreCase = true) }
            .map { it.key }
            .toList()
    }

    /**
     * 检查扩展名是否有对应的MIME类型
     */
    fun containsExtension(extension: String): Boolean {
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
