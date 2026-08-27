package io.core.common.util.share

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import io.core.utils.BuildConfig
import io.core.appCtx
import io.core.common.util.extensions.cool.getUri
import io.core.common.util.tools.UriTools
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ShareAir {

    sealed interface ShareContent {
        data class Text(
            val content: String,
            val config: ShareConfig
        ) : ShareContent

        data class File(
            val uri: Uri,
            val mimeType: String,
            val config: ShareConfig
        ) : ShareContent

        data class MultiFiles(
            val uris: List<Uri>,
            val mimeType: String,
            val config: ShareConfig
        ) : ShareContent

        data class Resource(
            val resId: Int,
            val fileName: String?,
            val config: ShareConfig
        ) : ShareContent
    }

    private object MimeType {
        const val UNKNOWN = "*/*"
        private val typeMap = mapOf(
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "pdf" to "application/pdf",
            "doc" to "application/msword",
            "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls" to "application/vnd.ms-excel",
            "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "zip" to "application/zip",
            "txt" to "text/plain",
            "apk" to "application/vnd.android.package-archive"
        )

        fun fromUri(uri: Uri): String {
            val path = uri.toString()
            return typeMap[path.substringAfterLast('.')] ?: UNKNOWN
        }
    }

    class ShareConfig private constructor() {
        var context: Context = appCtx
            private set
        var title: String = "分享到"
            private set

        companion object {
            operator fun invoke(block: ShareConfig.() -> Unit): ShareContent {
                val config = ShareConfig().apply(block)
                return config.content ?: throw IllegalArgumentException("必须指定分享内容")
            }
        }

        private var content: ShareContent? = null

        fun text(content: String) {
            this.content = ShareContent.Text(content, this)
        }

        @JvmOverloads
        fun file(uri: Uri, mimeType: String = MimeType.UNKNOWN) {
            this.content = ShareContent.File(uri, mimeType, this)
        }

        @JvmOverloads
        fun file(file: File, mimeType: String = MimeType.UNKNOWN) {
            this.content = ShareContent.File(UriTools.file2Uri(file), mimeType, this)
        }

        @JvmOverloads
        fun files(vararg uris: Uri, mimeType: String = MimeType.UNKNOWN) {
            this.content = ShareContent.MultiFiles(uris.toList(), mimeType, this)
        }

        fun resource(resId: Int, fileName: String? = null) {
            this.content = ShareContent.Resource(resId, fileName, this)
        }

        fun context(ctx: Context) {
            context = ctx.applicationContext
        }

        fun title(text: String) {
            title = text
        }
    }

    @JvmStatic
    fun share(config: ShareConfig.() -> Unit) = try {
        when (val content = ShareConfig(config)) {
            is ShareContent.Text -> handleText(content)
            is ShareContent.File -> handleFile(content)
            is ShareContent.MultiFiles -> handleMultipleFiles(content)
            is ShareContent.Resource -> handleResource(content)
        }
    } catch (e: Exception) {
        showDetailedError(appCtx, e)
    }

    private fun handleText(content: ShareContent.Text) {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content.content)
            startSharing(content.config)
        }
    }

    private fun handleFile(content: ShareContent.File) {
        Intent(Intent.ACTION_SEND).apply {
            type = content.mimeType.takeIf { it != MimeType.UNKNOWN }
                ?: MimeType.fromUri(content.uri)
            putExtra(Intent.EXTRA_STREAM, content.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startSharing(content.config)
        }
    }

    private fun handleMultipleFiles(content: ShareContent.MultiFiles) {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = content.mimeType.takeIf { it != MimeType.UNKNOWN }
                ?: content.uris.firstOrNull()?.let { MimeType.fromUri(it) } ?: MimeType.UNKNOWN
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(content.uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startSharing(content.config)
        }
    }

    private fun handleResource(content: ShareContent.Resource) = try {
        val uri =
            createTempFileFromResource(content.config.context, content.resId, content.fileName)
        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startSharing(content.config)
        }
    } catch (e: IOException) {
        throw ResourceProcessingException("资源文件处理失败", e)
    }


    private fun createTempFileFromResource(context: Context, resId: Int, fileName: String?): Uri {
        val resources = context.resources
        val outputFile = File(
            context.cacheDir,
            fileName ?: "${resources.getResourceEntryName(resId)}_${System.currentTimeMillis()}.png"
        )

        FileOutputStream(outputFile).use { output ->
            resources.openRawResource(resId).use { input ->
                input.copyTo(output)
            }
        }
        return outputFile.getUri()
    }

    private fun Intent.startSharing(config: ShareConfig) = try {
        val finalIntent = Intent.createChooser(this, config.title).apply {
            if (config.context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        when (config.context) {
            is Activity -> config.context.startActivity(finalIntent)
            else -> config.context.applicationContext.startActivity(finalIntent)
        }
    } catch (e: ActivityNotFoundException) {
        throw SharingException("未找到可用的分享应用", e)
    }


    private fun showDetailedError(context: Context, e: Exception) {
        val errorMessage = when (e) {
            is ResourceProcessingException -> "资源处理错误: ${e.message}"
            is SharingException -> "分享失败: ${e.message}"
            else -> "系统错误: ${e.localizedMessage}"
        }

        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        if (BuildConfig.DEBUG) {
            Log.e("ShareUtil", errorMessage, e)
        }
    }

    private class ResourceProcessingException(message: String, cause: Throwable) : Exception(message, cause)
    private class SharingException(message: String, cause: Throwable) : Exception(message, cause)
}