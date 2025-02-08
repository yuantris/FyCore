package io.core.common.util.tools

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import io.core.Android
import io.core.common.util.ext.appCtx
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
object UriUtils {

    // 获取安全的文件 URI
    fun file2Uri(file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                appCtx,
                "${appCtx.packageName}.fycore.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
    }

    fun path2Uri(path: String): Uri? {
        return file2Uri(File(path))
    }

    fun uri2File(uri: Uri): File? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = RealPathUtil.getPath(appCtx, uri)
            return if (path != null) {
                File(path)
            } else {
                null
            }
        } else {
            return uri.path?.let { File(it) }
        }
    }
}