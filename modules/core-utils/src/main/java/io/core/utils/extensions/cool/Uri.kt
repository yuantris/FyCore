package io.core.utils.extensions.cool

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import io.core.base.appCtx
import io.core.utils.FileDoc
import io.core.utils.extensions.ui.checkSelfUriPermission
import io.core.utils.tools.DocumentUtils
import io.core.utils.tools.FileTools
import io.core.utils.tools.RealPathUtil
import io.core.utils.tools.UriTools
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

fun Uri.isFileUri() = scheme.equals("file", ignoreCase = true)
fun Uri.isContentUri() = scheme.equals("content", ignoreCase = true)
fun Uri.toFile() = UriTools.uri2File(this)

/**
 * 读取URI
 */
fun AppCompatActivity.readUri(
    uri: Uri?,
    success: (fileDoc: FileDoc, inputStream: InputStream) -> Unit
) {
    uri ?: return
    try {
        if (uri.isContentUri()) {
            val doc = DocumentFile.fromSingleUri(this, uri)
            doc ?: throw RuntimeException("未获取到文件")
            val fileDoc = FileDoc.fromDocumentFile(doc)
            contentResolver.openInputStream(uri)!!.use { inputStream ->
                success.invoke(fileDoc, inputStream)
            }
        } else {
            if (hasReadStoragePermission()) {
                RealPathUtil.getPath(uri)?.let { path ->
                    val file = File(path)
                    val fileDoc = FileDoc.fromFile(file)
                    FileInputStream(file).use { inputStream ->
                        success.invoke(fileDoc, inputStream)
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printOnDebug()
        if (e is SecurityException) {
            throw e
        }
    }
}

/**
 * 读取URI
 */
fun Fragment.readUri(uri: Uri?, success: (fileDoc: FileDoc, inputStream: InputStream) -> Unit) {
    uri ?: return
    try {
        if (uri.isContentUri()) {
            val doc = DocumentFile.fromSingleUri(requireContext(), uri)
            doc ?: throw RuntimeException("未获取到文件")
            val fileDoc = FileDoc.fromDocumentFile(doc)
            requireContext().contentResolver.openInputStream(uri)!!.use { inputStream ->
                success.invoke(fileDoc, inputStream)
            }
        } else {
            if (requireContext().hasReadStoragePermission()) {
                RealPathUtil.getPath(uri)?.let { path ->
                    val file = File(path)
                    val fileDoc = FileDoc.fromFile(file)
                    FileInputStream(file).use { inputStream ->
                        success.invoke(fileDoc, inputStream)
                    }

                }
            }
        }
    } catch (e: Exception) {
        e.printOnDebug()
    }
}

@Throws(Exception::class)
fun Uri.readBytes(context: Context): ByteArray {
    return if (this.isContentUri()) {
        context.contentResolver.openInputStream(this)?.let {
            val len: Int = it.available()
            val buffer = ByteArray(len)
            it.read(buffer)
            it.close()
            return buffer
        } ?: throw RuntimeException("打开文件失败\n${this}")
    } else {
        val path = RealPathUtil.getPath(this)
        if (path?.isNotEmpty() == true) {
            File(path).readBytes()
        } else {
            throw RuntimeException("获取文件真实地址失败\n${this.path}")
        }
    }
}

@Throws(Exception::class)
fun Uri.readText(context: Context): String {
    readBytes(context).let {
        return String(it)
    }
}

@Throws(Exception::class)
fun Uri.writeBytes(
    context: Context,
    byteArray: ByteArray
): Boolean {
    if (this.isContentUri()) {
        context.contentResolver.openOutputStream(this)?.let {
            it.write(byteArray)
            it.close()
            return true
        }
        return false
    } else {
        val path = RealPathUtil.getPath(this)
        if (path?.isNotEmpty() == true) {
            File(path).writeBytes(byteArray)
            return true
        }
    }
    return false
}

@Throws(Exception::class)
fun Uri.writeText(context: Context, text: String, charset: Charset = Charsets.UTF_8): Boolean {
    return writeBytes(context, text.toByteArray(charset))
}

fun Uri.writeBytes(
    context: Context,
    fileName: String,
    byteArray: ByteArray
): Boolean {
    if (this.isContentUri()) {
        DocumentFile.fromTreeUri(context, this)?.let { pDoc ->
            DocumentUtils.createFileIfNotExist(pDoc, fileName)?.let {
                return it.uri.writeBytes(context, byteArray)
            }
        }
    } else {
        FileTools.createFileWithReplace(path + File.separatorChar + fileName)
            .writeBytes(byteArray)
        return true
    }
    return false
}

fun Uri.inputStream(context: Context): Result<InputStream> {
    val uri = this
    return kotlin.runCatching {
        try {
            if (isContentUri()) {
                DocumentFile.fromSingleUri(context, uri)
                    ?: throw RuntimeException("未获取到文件")
                return@runCatching context.contentResolver.openInputStream(uri)!!
            } else {
                val path = RealPathUtil.getPath(uri)
                    ?: throw RuntimeException("未获取到文件")
                val file = File(path)
                if (file.exists()) {
                    return@runCatching FileInputStream(file)
                } else {
                    throw RuntimeException("文件不存�?)
                }
            }
        } catch (e: Exception) {
            e.printOnDebug()
            throw e
        }
    }
}

fun Uri.outputStream(context: Context): Result<OutputStream> {
    val uri = this
    return kotlin.runCatching {
        try {
            if (isContentUri()) {
                DocumentFile.fromSingleUri(context, uri)
                    ?: throw RuntimeException("未获取到文件")
                return@runCatching context.contentResolver.openOutputStream(uri)!!
            } else {
                val path = RealPathUtil.getPath(uri)
                    ?: throw RuntimeException("未获取到文件")
                val file = File(path)
                if (file.exists()) {
                    return@runCatching FileOutputStream(file)
                } else {
                    throw RuntimeException("文件不存�?)
                }
            }
        } catch (e: Exception) {
            e.printOnDebug()
            throw e
        }
    }
}

fun Uri.toReadPfd(context: Context): Result<ParcelFileDescriptor> {
    val uri = this
    return kotlin.runCatching {
        try {
            if (isContentUri()) {
                DocumentFile.fromSingleUri(context, uri)
                    ?: throw RuntimeException("未获取到文件")
                return@runCatching context.contentResolver.openFileDescriptor(uri, "r")!!
            } else {
                val path = RealPathUtil.getPath(uri)
                    ?: throw RuntimeException("未获取到文件")
                val file = File(path)
                if (file.exists()) {
                    return@runCatching ParcelFileDescriptor.open(
                        file,
                        ParcelFileDescriptor.MODE_READ_ONLY
                    )
                } else {
                    throw RuntimeException("文件不存�?)
                }
            }


        } catch (e: Exception) {
            e.printOnDebug()
            throw e
        }
    }
}

fun Uri.toWritePfd(context: Context): Result<ParcelFileDescriptor> {
    val uri = this
    return kotlin.runCatching {
        try {
            if (isContentUri()) {
                DocumentFile.fromSingleUri(context, uri)
                    ?: throw RuntimeException("未获取到文件")
                return@runCatching context.contentResolver.openFileDescriptor(uri, "w")!!
            } else {
                val path = RealPathUtil.getPath(uri)
                    ?: throw RuntimeException("未获取到文件")
                val file = File(path)
                if (file.exists()) {
                    return@runCatching ParcelFileDescriptor.open(
                        file,
                        ParcelFileDescriptor.MODE_WRITE_ONLY
                    )
                } else {
                    throw RuntimeException("文件不存�?)
                }
            }


        } catch (e: Exception) {
            e.printOnDebug()
            throw e
        }
    }
}


fun Uri.canRead(): Boolean {
    return appCtx.checkSelfUriPermission(
        this,
        Intent.FLAG_GRANT_READ_URI_PERMISSION
    ) == PackageManager.PERMISSION_GRANTED
}
