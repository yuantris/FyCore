package io.core.common.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import io.core.common.util.ext.authority
import io.core.common.util.ext.ui.appPackageName
import java.io.File

class FileSharer private constructor(private val builder: Builder) {

    fun share(context: Context) {
        val files = builder.files
        if (files.isEmpty()) {
            showError(context, "No files to share")
            return
        }

        try {
            val uris = files.map { file ->
                checkFileExists(file)
                getFileUri(context, file)
            }

            val intent = createShareIntent(context, uris)
            launchShareIntent(context, intent)
        } catch (e: Exception) {
            showError(context, e.message ?: "File sharing failed")
        }
    }

    private fun checkFileExists(file: File) {
        if (!file.exists()) throw IllegalArgumentException("File not found: ${file.path}")
    }

    private fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, authority, file)
    }

    private fun createShareIntent(context: Context, uris: List<Uri>): Intent {
        return if (uris.size == 1) {
            createSingleFileIntent(context, uris.first())
        } else {
            createMultipleFilesIntent(context, uris)
        }.apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun createSingleFileIntent(context: Context, uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(uri) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
    }

    private fun createMultipleFilesIntent(context: Context, uris: List<Uri>): Intent {
        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = resolveMimeType(context, uris)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }

    private fun resolveMimeType(context: Context, uris: List<Uri>): String {
        val mimeTypes = uris.map { context.contentResolver.getType(it) ?: "*/*" }
        return if (mimeTypes.distinct().size == 1) mimeTypes.first() else "*/*"
    }

    private fun launchShareIntent(context: Context, intent: Intent) {
        try {
            val chooserIntent = Intent.createChooser(intent, builder.chooserTitle)
            context.startActivity(chooserIntent)
        } catch (e: ActivityNotFoundException) {
            showError(context, "No app available to handle sharing")
        }
    }

    private fun showError(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    class Builder() {
        internal var chooserTitle: String? = null
        internal var files: List<File> = emptyList()

        fun setChooserTitle(title: String) = apply { this.chooserTitle = title }

        fun setFileList(files: List<File>) = apply { this.files = files }

        fun share(context: Context) {
            FileSharer(this).share(context)
        }
    }
}
