@file:Suppress("unused")

package io.core.utils

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import io.core.base.appCtx
import io.core.utils.extensions.cool.checkWrite
import io.core.utils.extensions.cool.inputStream
import io.core.utils.extensions.cool.isContentUri
import io.core.utils.extensions.cool.listFileDocs
import io.core.utils.extensions.cool.outputStream
import io.core.utils.extensions.cool.readBytes
import io.core.utils.extensions.cool.readText
import io.core.utils.extensions.cool.toReadPfd
import io.core.utils.extensions.cool.toWritePfd
import io.core.utils.extensions.cool.writeBytes
import io.core.utils.extensions.cool.writeText
import io.core.utils.extensions.currentTimeMillis
import io.core.utils.extensions.downloadManager
import io.core.utils.tools.DocumentUtils
import io.core.utils.tools.FileTools
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

data class FileDoc(
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val lastModified: Long,
    val uri: Uri
) {

    override fun toString(): String {
        return if (uri.isContentUri()) uri.toString() else uri.path!!
    }

    val isContentUri get() = uri.isContentUri()

    fun readBytes(): ByteArray {
        return uri.readBytes(appCtx)
    }

    fun readText(): String {
        return uri.readText(appCtx)
    }

    fun asDocumentFile(): DocumentFile? {
        if (isContentUri) {
            return if (isDir) {
                Class.forName("androidx.documentfile.provider.TreeDocumentFile")
                    .getDeclaredConstructor(
                        DocumentFile::class.java,
                        Context::class.java,
                        Uri::class.java
                    ).apply {
                        isAccessible = true
                    }.newInstance(null, appCtx, uri) as DocumentFile
            } else {
                DocumentFile.fromSingleUri(appCtx, uri)
            }
        }
        return null
    }

    fun asFile(): File? {
        if (isContentUri) {
            return null
        }
        return File(uri.path!!)
    }

    companion object {

        fun fromUri(uri: Uri, isDir: Boolean): FileDoc {
            if (uri.isContentUri()) {
                val doc = if (isDir) {
                    DocumentFile.fromTreeUri(appCtx, uri)!!
                } else if (uri.host == "downloads") {
                    val query = DownloadManager.Query()
                    query.setFilterById(uri.lastPathSegment!!.toLong())
                    downloadManager.query(query).use {
                        if (it.moveToFirst()) {
                            val lUriColum = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val lUri = it.getString(lUriColum)
                            DocumentFile.fromSingleUri(appCtx, Uri.parse(lUri))!!
                        } else {
                            DocumentFile.fromSingleUri(appCtx, uri)!!
                        }
                    }
                } else {
                    DocumentFile.fromSingleUri(appCtx, uri)!!
                }
                return FileDoc(doc.name ?: "", isDir, doc.length(), doc.lastModified(), doc.uri)
            }
            val file = File(uri.path!!)
            return FileDoc(file.name, isDir, file.length(), file.lastModified(), uri)
        }

        fun fromDocumentFile(doc: DocumentFile): FileDoc {
            return FileDoc(
                name = doc.name ?: "",
                isDir = doc.isDirectory,
                size = doc.length(),
                lastModified = doc.lastModified(),
                uri = doc.uri
            )
        }

        fun fromFile(file: File): FileDoc {
            return FileDoc(
                name = file.name,
                isDir = file.isDirectory,
                size = file.length(),
                lastModified = file.lastModified(),
                uri = Uri.fromFile(file)
            )
        }

    }
}

/**
 * 过滤�?
 */
typealias FileDocFilter = (file: FileDoc) -> Boolean

private val projection by lazy {
    arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_MIME_TYPE
    )
}

/**
 * 返回子文件列�?如果不是文件夹则返回null
 */
fun FileDoc.list(filter: FileDocFilter? = null): ArrayList<FileDoc>? {
    if (isDir) {
        if (uri.isContentUri()) {
            /**
             * DocumentFile �?listFiles() 非常的慢,所以这里直接从数据库查�?
             */
            val childrenUri = DocumentsContract
                .buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri))
            val docList = arrayListOf<FileDoc>()
            var cursor: Cursor? = null
            try {
                cursor = appCtx.contentResolver.query(
                    childrenUri,
                    projection,
                    null,
                    null,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                )
                cursor?.let {
                    val ici = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nci = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val sci = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val mci = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val dci = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    if (cursor.moveToFirst()) {
                        do {
                            val item = FileDoc(
                                name = cursor.getString(nci),
                                isDir = cursor.getString(mci) == DocumentsContract.Document.MIME_TYPE_DIR,
                                size = cursor.getLong(sci),
                                lastModified = cursor.getLong(dci),
                                uri = DocumentsContract.buildDocumentUriUsingTree(
                                    uri,
                                    cursor.getString(ici)
                                )
                            )
                            if (filter == null || filter.invoke(item)) {
                                docList.add(item)
                            }
                        } while (cursor.moveToNext())
                    }
                }
            } finally {
                cursor?.close()
            }
            return docList
        } else {
            return File(uri.path!!).listFileDocs(filter)
        }
    }
    return null
}

/**
 * 查找文档, 如果存在则返回文�?如果不存在返回空
 * @param name 文件�?
 * @param depth 查找文件夹深�?
 */
fun FileDoc.find(name: String, depth: Int = 0): FileDoc? {
    val list = list()
    list?.forEach {
        if (it.name == name) {
            return it
        }
    }
    if (depth > 0) {
        list?.forEach {
            if (it.isDir) {
                val fileDoc = it.find(name, depth - 1)
                if (fileDoc != null) {
                    return fileDoc
                }
            }
        }
    }
    return null
}

fun FileDoc.createFileIfNotExist(
    fileName: String,
    vararg subDirs: String
): FileDoc {
    return if (uri.isContentUri()) {
        val documentFile = asDocumentFile()!!
        val tmp = DocumentUtils.createFileIfNotExist(documentFile, fileName, *subDirs)!!
        FileDoc.fromDocumentFile(tmp)
    } else {
        val path = FileTools.getPath(uri.path!!, *subDirs) + File.separator + fileName
        val tmp = FileTools.createFileIfNotExist(path)
        FileDoc.fromFile(tmp)
    }
}

fun FileDoc.createFolderIfNotExist(
    vararg subDirs: String
): FileDoc {
    return if (uri.isContentUri()) {
        val documentFile = asDocumentFile()!!
        val tmp = DocumentUtils.createFolderIfNotExist(documentFile, *subDirs)!!
        FileDoc.fromDocumentFile(tmp)
    } else {
        val path = FileTools.getPath(uri.path!!, *subDirs)
        val tmp = FileTools.createFolderIfNotExist(path)
        FileDoc.fromFile(tmp)
    }
}

fun FileDoc.openInputStream(): Result<InputStream> {
    return uri.inputStream(appCtx)
}

fun FileDoc.openOutputStream(): Result<OutputStream> {
    return uri.outputStream(appCtx)
}

fun FileDoc.openReadPfd(): Result<ParcelFileDescriptor> {
    return uri.toReadPfd(appCtx)
}

fun FileDoc.openWritePfd(): Result<ParcelFileDescriptor> {
    return uri.toWritePfd(appCtx)
}

fun FileDoc.exists(
    fileName: String,
    vararg subDirs: String
): Boolean {
    return if (uri.isContentUri()) {
        DocumentUtils.exists(asDocumentFile()!!, fileName, *subDirs)
    } else {
        val path = FileTools.getPath(uri.path!!, *subDirs) + File.separator + fileName
        FileTools.exist(path)
    }
}

fun FileDoc.exists(): Boolean {
    return if (uri.isContentUri()) {
        asDocumentFile()!!.exists()
    } else {
        FileTools.exist(uri.path!!)
    }
}

fun FileDoc.writeText(text: String) {
    if (uri.isContentUri()) {
        uri.writeText(appCtx, text)
    } else {
        File(uri.path!!).writeText(text)
    }
}

fun FileDoc.delete() {
    asFile()?.let {
        FileTools.delete(it, true)
    }
    asDocumentFile()?.delete()
}

fun FileDoc.checkWrite(): Boolean? {
    if (!isDir) {
        throw RuntimeException("只能检查目�?)
    }
    asFile()?.let {
        return it.checkWrite()
    }
    return asDocumentFile()?.checkWrite()
}

/**
 * DocumentFile �?listFiles() 非常的慢,尽量不要使用
 */
fun DocumentFile.listFileDocs(filter: FileDocFilter? = null): ArrayList<FileDoc>? {
    return FileDoc.fromDocumentFile(this).list(filter)
}

@Throws(Exception::class)
fun DocumentFile.openInputStream(): InputStream? {
    return appCtx.contentResolver.openInputStream(uri)
}

@Throws(Exception::class)
fun DocumentFile.openOutputStream(): OutputStream? {
    return appCtx.contentResolver.openOutputStream(uri)
}

@Throws(Exception::class)
fun DocumentFile.writeText(context: Context, data: String, charset: Charset = Charsets.UTF_8) {
    uri.writeText(context, data, charset)
}

@Throws(Exception::class)
fun DocumentFile.writeBytes(context: Context, data: ByteArray) {
    uri.writeBytes(context, data)
}

@Throws(Exception::class)
fun DocumentFile.readText(context: Context): String {
    return String(readBytes(context))
}

@Throws(Exception::class)
fun DocumentFile.readBytes(context: Context): ByteArray {
    return context.contentResolver.openInputStream(uri)?.let {
        val len: Int = it.available()
        val buffer = ByteArray(len)
        it.read(buffer)
        it.close()
        return buffer
    } ?: throw RuntimeException("打开文件失败\n${uri}")
}

fun DocumentFile.checkWrite(): Boolean {
    return try {
        val filename = currentTimeMillis.toString()
        createFile(FileTools.getMimeType(filename), filename)?.let {
            it.openOutputStream()?.let { out ->
                out.use { }
                it.delete()
                return true
            }
        }
        false
    } catch (e: Exception) {
        false
    }
}
