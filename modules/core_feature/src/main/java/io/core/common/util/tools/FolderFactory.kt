package io.core.common.util.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

object FolderFactory {
    private val folders = mutableMapOf<String, Folder>()

    fun create(name: String, parentPath: String): Folder {
        val folder = Folder(File(parentPath, name).absolutePath)
        folders[name] = folder
        return folder
    }

    fun get(name: String): Folder? = folders[name]
}

class Folder(private val rootPath: String) {

    init {
        File(rootPath).mkdirs()
    }

    // 获取当前目录下所有文件（不含子目录）
    suspend fun listFiles(): List<File> = withContext(Dispatchers.IO) {
        File(rootPath).listFiles()?.filter { it.isFile } ?: emptyList()
    }

    // 递归获取所有文件（包含子目录）
    suspend fun listAllFiles(): List<File> = withContext(Dispatchers.IO) {
        fun walkDir(dir: File): List<File> {
            val files = dir.listFiles() ?: return emptyList()
            return files.filter { it.isFile } + files.filter { it.isDirectory }
                .flatMap { walkDir(it) }
        }
        walkDir(File(rootPath))
    }

    // 检查文件是否存在
    fun fileExists(filename: String): Boolean {
        return File(rootPath, filename).exists()
    }

    // 删除指定文件
    suspend fun deleteFile(filename: String) = withContext(Dispatchers.IO) {
        File(rootPath, filename).delete()
    }

    // 计算文件夹总大小（递归）
    suspend fun folderSize(): Long = withContext(Dispatchers.IO) {
        fun calculateSize(file: File): Long {
            if (file.isFile) return file.length()
            return file.listFiles()?.sumOf { calculateSize(it) } ?: 0L
        }
        calculateSize(File(rootPath))
    }

    // 清空文件夹
    suspend fun clear() = withContext(Dispatchers.IO) {
        File(rootPath).deleteRecursively()
        File(rootPath).mkdirs()
    }

    // 复制文件到其他目录（保留文件名）
    suspend fun copyTo(filename: String, targetFolder: Folder) = withContext(Dispatchers.IO) {
        val source = File(rootPath, filename)
        source.inputStream().use { input ->
            targetFolder.writeFile(input, filename)
        }
    }

    // 移动文件到其他目录（保留文件名）
    suspend fun moveTo(filename: String, targetFolder: Folder) = withContext(Dispatchers.IO) {
        copyTo(filename, targetFolder)
        deleteFile(filename)
    }

    // 协程版文件操作（自动切换IO线程）
    suspend fun writeFile(filename: String, content: String) = withContext(Dispatchers.IO) {
        File(rootPath, filename).writeText(content)
    }

    suspend fun writeFile(inputStream: InputStream, filename: String) =
        withContext(Dispatchers.IO) {
            File(rootPath, filename).outputStream().use { output ->
                inputStream.copyTo(output)
            }
        }

    suspend fun readFile(filename: String): String = withContext(Dispatchers.IO) {
        File(rootPath, filename).readText()
    }

    // Bitmap异步操作
    suspend fun writeBitmap(
        filename: String,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ) = withContext(Dispatchers.IO) {
        File(rootPath, filename).outputStream().use {
            bitmap.compress(format, 90, it)
        }
    }

    suspend fun getBitmap(filename: String): Bitmap? = withContext(Dispatchers.IO) {
        BitmapFactory.decodeFile(File(rootPath, filename).absolutePath)
    }

    // 可选：带进度的文件写入（适合大文件）
    suspend fun writeFileWithProgress(
        inputStream: InputStream,
        filename: String,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val file = File(rootPath, filename)
        val totalBytes = inputStream.available().toFloat()
        var copiedBytes = 0

        file.outputStream().use { output ->
            val buffer = ByteArray(8 * 1024)
            var bytes = inputStream.read(buffer)

            while (bytes >= 0) {
                output.write(buffer, 0, bytes)
                copiedBytes += bytes
                val progress = (copiedBytes / totalBytes * 100).toInt()
                withContext(Dispatchers.Main) { onProgress(progress) }
                bytes = inputStream.read(buffer)
            }
        }
    }
}