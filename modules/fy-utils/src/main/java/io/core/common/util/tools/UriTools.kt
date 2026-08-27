package io.core.common.util.tools

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import io.core.appCtx
import io.core.common.util.extensions.authority
import io.core.common.util.extensions.cool.isContentUri
import io.core.common.util.extensions.cool.isFileUri
import java.io.File

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/8 15:48
 * @description
 * @author Yuan
 */
object UriTools {

    // 新增常用方法：转换为可读路径
    fun uriToReadablePath(uri: Uri): String? {
        return when {
            uri.isFileUri() -> uri.path?.replace("file:".toRegex(), "")
            uri.isContentUri() -> RealPathUtil.getPath( uri)
            else -> null
        }
    }

    // 新增常用方法：资源转 URI
    fun resourceToUri(resId: Int): Uri {
        return "android.resource://${appCtx.packageName}/$resId".toUri()
    }

    // 获取安全的文件 URI
    @JvmStatic
    fun file2Uri(file: File): Uri {
        return if (isAndroid7Plus) {
            FileProvider.getUriForFile(appCtx, authority, file)
        } else {
            Uri.fromFile(file)
        }
    }

    @JvmStatic
    fun path2Uri(path: String): Uri? = try {
        file2Uri(File(path))
    } catch (e: Exception) {
        null
    }

    @JvmStatic
    fun uri2File(uri: Uri): File? {
        return if (isAndroid7Plus) {
            RealPathUtil.getPath(uri)?.let(::File)
        } else {
            uri.path?.let(::File)
        }?.takeIf { it.exists() }
    }
}