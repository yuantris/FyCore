package io.core.common.util.extensions.cool

import android.content.Context
import android.os.Environment
import java.io.File

val dcimDir: File
    get() = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)

val documentsDir: File
    get() = getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

val downloadsDir: File
    get() = getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

val picturesDir: File
    get() = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

val musicDir: File
    get() = getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

val PathType.publicDir: File?
    get() = dirType?.let { getExternalStoragePublicDirectory(it) }

fun Context.getBasePath(type: PathType): File = when {
    type.isPublic -> Environment.getExternalStoragePublicDirectory(type.dirType)
    else -> when (type) {
        PathType.CACHE -> this.cacheDir
        PathType.INTERNAL_FILES -> this.filesDir
        PathType.EXTERNAL_FILES -> this.getExternalFilesDir(null) ?: this.filesDir
        PathType.EXTERNAL_CACHE -> this.externalCacheDir ?: this.cacheDir
        PathType.DCIM, PathType.DOCUMENTS, PathType.DOWNLOADS, PathType.PICTURES, PathType.MUSIC ->
            Environment.getExternalStoragePublicDirectory(type.dirType)
    }
}

fun getExternalStoragePublicDirectory(dirType: String): File {
    return Environment.getExternalStoragePublicDirectory(dirType)
}

/**
 * 获取系统标准路径的扩展函数
 * @param type 路径类型枚举
 * @param subDir 可选子目录（支持多级目录格式）
 * @param autoCreate 是否自动创建目录（默认true）
 * @return 完整路径字符串，若目录不可用返回空字符串
 */
@JvmOverloads
fun Context.getSettingsPath(
    type: PathType,
    subDir: String? = null,
    autoCreate: Boolean = true
): String {
    // 获取基础目录
    val baseDir = type.getBasePath(this) ?: return ""

    // 构建目标目录
    val targetDir = subDir?.let { File(baseDir, it) } ?: baseDir

    // 自动创建目录（如果需要）
    if (autoCreate && !targetDir.exists()) {
        targetDir.mkdirs()
    }

    return targetDir.absolutePath
}

/**
 * 路径拼接优化版：自动转换路径分隔符
 */
@JvmOverloads
fun Context.getSettingsPathV2(
    type: PathType,
    subDir: String? = null,
    autoCreate: Boolean = true
): String {
    // 获取基础目录（同上）
    val baseDir = type.getBasePath(this) ?: return ""

    // 智能路径处理
    val normalizedSubDir = subDir?.replace("/", File.separator) // 统一转换分隔符
        ?.replace(Regex("[/\\\\]+"), File.separator) // 合并多余分隔符

    // 构建目标目录
    val targetDir = normalizedSubDir?.let {
        File(baseDir, it).apply {
            if (autoCreate && !exists()) mkdirs()
        }
    } ?: baseDir

    return targetDir.absolutePath
}

/**
 * 路径构建辅助函数（可选）
 * 示例："MyApp/Config".toPath() → "MyApp${File.separator}Config"
 */
fun String.toPath(): String = this
    .replace("/", File.separator)
    .replace(Regex("[/\\\\]+"), File.separator)


/**
 * 安全拼接路径的扩展函数
 * @param parts 路径组成部分（支持多级目录）
 * @return 使用系统分隔符拼接的标准路径
 */
fun String.joinPath(vararg parts: String): String {
    // 预处理所有路径段
    val processedParts = (listOf(this) + parts)
        .filterNot { it.isBlank() } // 过滤空字符串
        .map { part ->
            part.trim()
                .replace(Regex("[/\\\\]"), File.separator) // 统一转换分隔符
                .replace(Regex("${Regex.escape(File.separator)}+"), File.separator) // 合并重复分隔符
                .removePrefix(File.separator) // 去除头部多余分隔符
                .removeSuffix(File.separator) // 去除尾部多余分隔符
        }
        .filterNot { it.isEmpty() } // 再次过滤空字符串

    // 构建最终路径
    return processedParts.joinToString(File.separator) {
        if (it.contains(File.separator)) {
            // 处理包含多级路径的片段
            it.split(File.separator)
                .filterNot { s -> s.isEmpty() }
                .joinToString(File.separator)
        } else {
            it
        }
    }.let {
        // 处理根路径特殊情况
        if (this.startsWith(File.separator) && processedParts.isNotEmpty()) {
            File.separator + it
        } else {
            it
        }
    }
}

/**
 * 从空字符串开始拼接的扩展函数
 */
fun joinPath(vararg parts: String): String = "".joinPath(*parts)

/**
 * 路径类型枚举，整合路径配置信息
 * @property dirType 对应Environment常量值
 * @property isPublic 是否为公共存储目录
 * @property description 目录用途说明
 */
enum class PathType(
    val dirType: String? = null,
    val isPublic: Boolean = false,
    val description: String
) {
    // 应用私有目录
    CACHE(
        description = "内部缓存目录（自动清理）"
    ),
    INTERNAL_FILES(
        description = "内部持久化文件目录"
    ),
    EXTERNAL_FILES(
        dirType = null,
        description = "外部私有文件目录"
    ),
    EXTERNAL_CACHE(
        description = "外部缓存目录"
    ),

    // 公共目录
    DCIM(
        dirType = Environment.DIRECTORY_DCIM,
        isPublic = true,
        description = "公共相册目录"
    ),
    DOCUMENTS(
        dirType = Environment.DIRECTORY_DOCUMENTS,
        isPublic = true,
        description = "公共文档目录"
    ),
    DOWNLOADS(
        dirType = Environment.DIRECTORY_DOWNLOADS,
        isPublic = true,
        description = "公共下载目录"
    ),
    PICTURES(
        dirType = Environment.DIRECTORY_PICTURES,
        isPublic = true,
        description = "公共图片目录"
    ),
    MUSIC(
        dirType = Environment.DIRECTORY_MUSIC,
        isPublic = true,
        description = "公共音乐目录"
    );

    /**
     * 获取基础目录路径（不包含子目录）
     */
    fun getBasePath(context: Context): File? = when {
        isPublic -> Environment.getExternalStoragePublicDirectory(dirType)
        else -> when (this) {
            CACHE -> context.cacheDir
            INTERNAL_FILES -> context.filesDir
            EXTERNAL_FILES -> context.getExternalFilesDir(null)
            EXTERNAL_CACHE -> context.externalCacheDir
            else -> null
        }
    }
}
