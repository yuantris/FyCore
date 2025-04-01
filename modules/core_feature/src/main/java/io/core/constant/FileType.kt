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
        ".3gp" to "video/3gpp",
        ".apk" to "application/vnd.android.package-archive",
        ".asf" to "video/x-ms-asf",
        ".avi" to "video/x-msvideo",
        ".bin" to "application/octet-stream",
        ".bmp" to "image/bmp",
        ".c" to "text/plain",
        ".class" to "application/octet-stream",
        ".conf" to "text/plain",
        ".cpp" to "text/plain",
        ".doc" to "application/msword",
        ".docx" to "application/msword",
        ".xls" to "application/msword",
        ".xlsx" to "application/msword",
        ".exe" to "application/octet-stream",
        ".gif" to "image/gif",
        ".gtar" to "application/x-gtar",
        ".gz" to "application/x-gzip",
        ".h" to "text/plain",
        ".htm" to "text/html",
        ".html" to "text/html",
        ".jar" to "application/java-archive",
        ".java" to "text/plain",
        ".jpeg" to "image/jpeg",
        ".jpg" to "image/jpeg",
        ".js" to "application/x-javascript",
        ".log" to "text/plain",
        ".m3u" to "audio/x-mpegurl",
        ".m4a" to "audio/mp4a-latm",
        ".m4b" to "audio/mp4a-latm",
        ".m4p" to "audio/mp4a-latm",
        ".m4u" to "video/vnd.mpegurl",
        ".m4v" to "video/x-m4v",
        ".mov" to "video/quicktime",
        ".mp2" to "audio/x-mpeg",
        ".mp3" to "audio/x-mpeg",
        ".mp4" to "video/mp4",
        ".mpc" to "application/vnd.mpohun.certificate",
        ".mpe" to "video/mpeg",
        ".mpeg" to "video/mpeg",
        ".mpg" to "video/mpeg",
        ".mpg4" to "video/mp4",
        ".mpga" to "audio/mpeg",
        ".msg" to "application/vnd.ms-outlook",
        ".ogg" to "audio/ogg",
        ".pdf" to "application/pdf",
        ".png" to "image/png",
        ".pps" to "application/vnd.ms-powerpoint",
        ".ppt" to "application/vnd.ms-powerpoint",
        ".prop" to "text/plain",
        ".rar" to "application/x-rar-compressed",
        ".rc" to "text/plain",
        ".rmvb" to "audio/x-pn-realaudio",
        ".rtf" to "application/rtf",
        ".sh" to "text/plain",
        ".tar" to "application/x-tar",
        ".tgz" to "application/x-compressed",
        ".txt" to "text/plain",
        ".wav" to "audio/x-wav",
        ".wma" to "audio/x-ms-wma",
        ".wmv" to "audio/x-ms-wmv",
        ".wps" to "application/vnd.ms-works",
        ".xml" to "text/plain",
        ".z" to "application/x-compress",
        ".zip" to "application/zip",
        "" to "*/*"
    )

    /**
     * 根据文件扩展名获取对应的MIME类型
     * @param extension 文件扩展名（带点号，如".jpg"）
     * @return 对应的MIME类型
     */
    fun getMimeType(extension: String): String {
        return MIME_TYPE_MAP[extension.lowercase()] ?: "*/*"
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
     * 检查扩展名是否有对应的MIME类型
     */
    fun containsExtension(extension: String): Boolean {
        return MIME_TYPE_MAP.containsKey(extension.lowercase())
    }
}
