package io.core.utils.extensions.cool

import android.net.Uri
import io.core.utils.CoreUtil
import io.core.utils.FileDoc
import io.core.utils.FileDocFilter
import io.core.utils.extensions.currentTimeMillis
import io.core.utils.tools.FileTools
import io.core.utils.tools.UriTools
import java.io.File
import java.io.FileOutputStream

fun File.refreshMediaLibrary() {
    CoreUtil.Files.refreshMediaLibrary(file = this)
}

fun File.getUri(): Uri {
    return UriTools.file2Uri(this)
}

fun File.getFile(vararg subDirFiles: String): File {
    val path = FileTools.getPath(this, *subDirFiles)
    return File(path)
}

fun String.getFileName(): String {
    if (!isFilePath()) throw IllegalArgumentException("$this is not a file path")
    return FileTools.getName(this)
}

fun File.getNameNoExtension(): String {
    return FileTools.getNameExcludeExtension(this.absolutePath)
}

fun String.getFileNameNoExtension(): String {
    if (!isFilePath()) throw IllegalArgumentException("$this is not a file path")
    return FileTools.getNameExcludeExtension(this)
}

fun String.isFilePath(): Boolean {
    return try {
        File(this).canonicalPath // 尝试解析路径（自动处理非法字符和格式�?
        !contains('\u0000')     // 额外排除空字�?
    } catch (e: Exception) {
        false
    }
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
