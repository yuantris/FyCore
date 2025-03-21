@file:Suppress("unused")

package io.core.common.util.extensions.cool

import android.net.Uri
import androidx.core.content.FileProvider
import io.core.appCtx
import io.core.common.util.CoreUtil
import io.core.common.util.FileDoc
import io.core.common.util.FileDocFilter
import io.core.common.util.extensions.authority
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.tools.FileTools
import io.core.common.util.tools.UriTools
import java.io.File
import java.io.FileOutputStream

fun File.refreshMediaLibrary() {
    CoreUtil.Files.refreshMediaLibrary(file = this)
}

fun File.getUri(): Uri {
    return toUri()
}

fun File.toUri(): Uri {
    return UriTools.file2Uri(this)
}

fun File.getFile(vararg subDirFiles: String): File {
    val path = FileTools.getPath(this, *subDirFiles)
    return File(path)
}

fun File.exists(vararg subDirFiles: String): Boolean {
    return getFile(*subDirFiles).exists()
}

@Throws(Exception::class)
fun File.listFileDocs(filter: FileDocFilter? = null): ArrayList<FileDoc> {
    val docList = arrayListOf<FileDoc>()
    listFiles()?.forEach {
        val item = FileDoc(
            it.name,
            it.isDirectory,
            it.length(),
            it.lastModified(),
            Uri.fromFile(it)
        )
        if (filter == null || filter.invoke(item)) {
            docList.add(item)
        }
    }
    return docList
}

fun File.createFileIfNotExist(): File {
    if (!exists()) {
        parentFile?.createFolderIfNotExist()
        createNewFile()
    }
    return this
}

fun File.createFileReplace(): File {
    if (!exists()) {
        parent?.let {
            File(it).mkdirs()
        }
        createNewFile()
    } else {
        delete()
        createNewFile()
    }
    return this
}

fun File.createFolderIfNotExist(): File {
    if (!exists()) {
        mkdirs()
    }
    return this
}

fun File.createFolderReplace(): File {
    if (exists()) {
        FileTools.delete(this, true)
    }
    mkdirs()
    return this
}

fun File.checkWrite(): Boolean {
    return try {
        val filename = currentTimeMillis.toString()
        val file = FileTools.createFileIfNotExist(this, filename)
        file.outputStream().use { }
        file.delete()
        true
    } catch (e: Exception) {
        false
    }
}

fun File.outputStream(append: Boolean = false): FileOutputStream {
    return FileOutputStream(this, append)
}
