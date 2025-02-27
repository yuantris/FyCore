package io.core.common.util.tools

import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.annotation.IntDef
import io.core.appCtx
import io.core.common.util.extensions.cool.ConvertUtils
import io.core.common.util.extensions.cool.cnCompare
import io.core.common.util.extensions.cool.externalCache
import io.core.common.util.extensions.cool.printOnDebug
import io.core.common.util.extensions.currentTimeMillis
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.regex.Pattern

object FileUtils {

    /**
     * 创建指定路径的文件（若不存在），自动创建所有必需的父目录
     *
     * @param root 基础目录路径，作为文件路径的根节点
     * @param subDirFiles 可变参数，表示从根目录开始的子目录层级结构及最终文件名。
     *                    例如：["dir1", "dir2", "file.txt"]
     * @return 已存在或新创建的文件对象
     *
     * @throws IOException 当文件创建失败或路径中的某个元素是已存在的非目录文件时抛出
     */
    fun createFileIfNotExist(root: File, vararg subDirFiles: String): File {
        val filePath = getPath(root, *subDirFiles)
        return createFileIfNotExist(filePath)
    }

    fun createFolderIfNotExist(root: File, vararg subDirs: String): File {
        val filePath = getPath(root, *subDirs)
        return createFolderIfNotExist(filePath)
    }

    fun createFolderIfNotExist(filePath: String): File {
        val file = File(filePath)
        //如果文件夹不存在，就创建它
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    @Synchronized
    fun createFileIfNotExist(filePath: String): File {
        val file = File(filePath)
        try {
            if (!file.exists()) {
                //创建父类文件夹
                file.parent?.let {
                    createFolderIfNotExist(it)
                }
                //创建文件
                file.createNewFile()
            }
        } catch (e: IOException) {
            e.printOnDebug()
        }
        return file
    }

    /**
     * 创建文件，如果文件已存在则删除并重新创建
     *
     * @param filePath 文件路径
     * @return 返回创建的文件对象
     */
    fun createFileWithReplace(filePath: String): File {
        val file = File(filePath)
        if (!file.exists()) {
            //创建父类文件夹
            file.parent?.let {
                createFolderIfNotExist(it)
            }
            //创建文件
            file.createNewFile()
        } else {
            file.delete()
            file.createNewFile()
        }
        return file
    }

    /**
     * 生成完整的文件路径
     *
     * 该函数从一个根路径开始，根据可变数量的子目录文件名参数，构建一个完整的文件路径
     * 它确保了路径的正确性，即每个子目录之间用正确的文件分隔符隔开，并且不会在路径末尾重复添加分隔符
     *
     * @param rootPath 根路径，路径构建的起点
     * @param subDirFiles 可变数量的子目录文件名，用于构建最终路径
     * @return 返回构建完成的完整文件路径
     */
    fun getPath(rootPath: String, vararg subDirFiles: String): String {
        val path = StringBuilder(rootPath)
        subDirFiles.forEach {
            if (it.isNotEmpty()) {
                if (!path.endsWith(File.separator)) {
                    path.append(File.separator)
                }
                path.append(it)
            }
        }
        return path.toString()
    }

    /**
     * 根据根目录和子目录文件名生成完整路径字符串
     *
     * 该函数接受一个根目录文件对象和一个可变长度的子目录文件名数组，
     * 并构建起一个完整的文件路径字符串它通过在根目录的绝对路径之后，
     * 依次添加每个子目录文件名，使用系统文件分隔符连接
     *
     * @param root 根目录的文件对象，是构建路径的起点
     * @param subDirFiles 可变长度的子目录文件名数组，表示路径中的子目录或文件名
     * @return 返回构建的完整路径字符串
     */
    fun getPath(root: File, vararg subDirFiles: String): String {
        val path = StringBuilder(root.absolutePath)
        subDirFiles.forEach {
            if (it.isNotEmpty()) {
                path.append(File.separator).append(it)
            }
        }
        return path.toString()
    }

    fun getCachePath(): String {
        return appCtx.externalCache.absolutePath
    }

    fun getSdCardPath(): String {
        var sdCardDirectory = Environment.getExternalStorageDirectory().absolutePath
        try {
            sdCardDirectory = File(sdCardDirectory).canonicalPath
        } catch (e: IOException) {
            e.printOnDebug()
        }
        return sdCardDirectory
    }

    const val BY_NAME_ASC = 0
    const val BY_NAME_DESC = 1
    const val BY_TIME_ASC = 2
    const val BY_TIME_DESC = 3
    const val BY_SIZE_ASC = 4
    const val BY_SIZE_DESC = 5
    const val BY_EXTENSION_ASC = 6
    const val BY_EXTENSION_DESC = 7

    @IntDef(value = [BY_NAME_ASC, BY_NAME_DESC, BY_TIME_ASC, BY_TIME_DESC, BY_SIZE_ASC, BY_SIZE_DESC, BY_EXTENSION_ASC, BY_EXTENSION_DESC])
    @Retention(AnnotationRetention.SOURCE)
    annotation class SortType

    /**
     * 将目录分隔符统一为平台默认的分隔符，并为目录结尾添加分隔符
     */
    fun separator(path: String): String {
        var path1 = path
        val separator = File.separator
        path1 = path1.replace("\\", separator)
        if (!path1.endsWith(separator)) {
            path1 += separator
        }
        return path1
    }

    /**
     * 关闭一个可关闭的资源，忽略任何由此产生的IOException。
     * 这个方法主要用于简化资源的关闭操作，避免在关闭资源时处理异常。
     *
     * @param c 可关闭的资源，如文件流或网络连接。如果为null，则方法直接返回。
     */
    fun closeSilently(c: Closeable?) {
        if (c == null) {
            return
        }
        try {
            c.close()
        } catch (ignored: IOException) {
        }

    }

    /**
     * 列出指定目录下的所有子目录
     */
    @JvmOverloads
    fun listDirs(
        startDirPath: String,
        excludeDirs: Array<String>? = null, @SortType sortType: Int = BY_NAME_ASC
    ): Array<File> {
        var excludeDirs1 = excludeDirs
        val dirList = ArrayList<File>()
        val startDir = File(startDirPath)
        if (!startDir.isDirectory) {
            return arrayOf()
        }
        val dirs = startDir.listFiles(FileFilter { f ->
            if (f == null) {
                return@FileFilter false
            }
            f.isDirectory
        }) ?: return arrayOf()
        if (excludeDirs1 == null) {
            excludeDirs1 = arrayOf()
        }
        for (dir in dirs) {
            val file = dir.absoluteFile
            if (!excludeDirs1.contentDeepToString().contains(file.name)) {
                dirList.add(file)
            }
        }
        when (sortType) {
            BY_NAME_ASC -> Collections.sort(dirList, SortByName())
            BY_NAME_DESC -> {
                Collections.sort(dirList, SortByName())
                dirList.reverse()
            }

            BY_TIME_ASC -> Collections.sort(dirList, SortByTime())
            BY_TIME_DESC -> {
                Collections.sort(dirList, SortByTime())
                dirList.reverse()
            }

            BY_SIZE_ASC -> Collections.sort(dirList, SortBySize())
            BY_SIZE_DESC -> {
                Collections.sort(dirList, SortBySize())
                dirList.reverse()
            }

            BY_EXTENSION_ASC -> Collections.sort(dirList, SortByExtension())
            BY_EXTENSION_DESC -> {
                Collections.sort(dirList, SortByExtension())
                dirList.reverse()
            }
        }
        return dirList.toTypedArray()
    }

    /**
     * 列出指定目录下的所有子目录及所有文件
     */
    @JvmOverloads
    fun listDirsAndFiles(
        startDirPath: String,
        allowExtensions: Array<String>? = null
    ): Array<File>? {
        val dirs: Array<File>?
        val files: Array<File>? = if (allowExtensions == null) {
            listFiles(startDirPath)
        } else {
            listFiles(startDirPath, allowExtensions)
        }
        dirs = listDirs(startDirPath)
        if (files == null) {
            return null
        }
        return dirs + files
    }

    /**
     * 列出指定目录下的所有文件
     */
    @JvmOverloads
    fun listFiles(
        startDirPath: String,
        filterPattern: Pattern? = null, @SortType sortType: Int = BY_NAME_ASC
    ): Array<File> {
        val fileList = ArrayList<File>()
        val f = File(startDirPath)
        if (!f.isDirectory) {
            return arrayOf()
        }
        val files = f.listFiles(FileFilter { file ->
            if (file == null) {
                return@FileFilter false
            }
            if (file.isDirectory) {
                return@FileFilter false
            }

            filterPattern?.matcher(file.name)?.find() ?: true
        })
            ?: return arrayOf()
        for (file in files) {
            fileList.add(file.absoluteFile)
        }
        when (sortType) {
            BY_NAME_ASC -> Collections.sort(fileList, SortByName())
            BY_NAME_DESC -> {
                Collections.sort(fileList, SortByName())
                fileList.reverse()
            }

            BY_TIME_ASC -> Collections.sort(fileList, SortByTime())
            BY_TIME_DESC -> {
                Collections.sort(fileList, SortByTime())
                fileList.reverse()
            }

            BY_SIZE_ASC -> Collections.sort(fileList, SortBySize())
            BY_SIZE_DESC -> {
                Collections.sort(fileList, SortBySize())
                fileList.reverse()
            }

            BY_EXTENSION_ASC -> Collections.sort(fileList, SortByExtension())
            BY_EXTENSION_DESC -> {
                Collections.sort(fileList, SortByExtension())
                fileList.reverse()
            }
        }
        return fileList.toTypedArray()
    }

    /**
     * 列出指定目录下的所有文件
     */
    fun listFiles(startDirPath: String, allowExtensions: Array<String>?): Array<File>? {
        val file = File(startDirPath)
        return file.listFiles { _, name ->
            //返回当前目录所有以某些扩展名结尾的文件
            val extension = getExtension(name)
            allowExtensions?.contentDeepToString()?.contains(extension) == true
                    || allowExtensions == null
        }
    }

    /**
     * 列出指定目录下的所有文件
     */
    fun listFiles(startDirPath: String, allowExtension: String?): Array<File>? {
        return if (allowExtension == null)
            listFiles(startDirPath, allowExtension = null)
        else
            listFiles(startDirPath, arrayOf(allowExtension))
    }

    /**
     * 判断文件或目录是否存在
     */
    fun exist(path: String): Boolean {
        val file = File(path)
        return file.exists()
    }

    /**
     * 删除文件或目录
     */
    @JvmOverloads
    fun delete(file: File, deleteRootDir: Boolean = false): Boolean {
        var result = false
        if (file.isFile) {
            //是文件
            result = deleteResolveEBUSY(file)
        } else {
            //是目录
            val files = file.listFiles() ?: return false
            if (files.isEmpty()) {
                result = deleteRootDir && deleteResolveEBUSY(file)
            } else {
                for (f in files) {
                    delete(f, deleteRootDir)
                    result = deleteResolveEBUSY(f)
                }
            }
            if (deleteRootDir) {
                result = deleteResolveEBUSY(file)
            }
        }
        return result
    }

    /**
     * bug: open failed: EBUSY (Device or resource busy)
     * fix: http://stackoverflow.com/questions/11539657/open-failed-ebusy-device-or-resource-busy
     */
    private fun deleteResolveEBUSY(file: File): Boolean {
        // Before you delete a Directory or File: rename it!
        val to = File(file.absolutePath + currentTimeMillis)

        file.renameTo(to)
        return to.delete()
    }

    /**
     * 删除文件或目录
     */
    @JvmOverloads
    fun delete(path: String, deleteRootDir: Boolean = true): Boolean {
        val file = File(path)

        return if (file.exists()) {
            delete(file, deleteRootDir)
        } else false
    }

    /**
     * 复制文件为另一个文件，或复制某目录下的所有文件及目录到另一个目录下
     */
    fun copy(src: String, tar: String): Boolean {
        val srcFile = File(src)
        return srcFile.exists() && copy(srcFile, File(tar))
    }

    /**
     * 复制文件或目录
     */
    fun copy(src: File, tar: File): Boolean {
        try {
            if (src.isFile) {
                val inputStream = FileInputStream(src)
                val outputStream = FileOutputStream(tar)
                inputStream.use {
                    outputStream.use {
                        inputStream.copyTo(outputStream)
                        outputStream.flush()
                    }
                }
            } else if (src.isDirectory) {
                tar.mkdirs()
                src.listFiles()?.forEach { file ->
                    copy(file.absoluteFile, File(tar.absoluteFile, file.name))
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }

    }

    /**
     * 移动文件或目录
     */
    fun move(src: String, tar: String): Boolean {
        return move(File(src), File(tar))
    }

    /**
     * 移动文件或目录
     */
    fun move(src: File, tar: File): Boolean {
        return rename(src, tar)
    }

    /**
     * 文件重命名
     */
    fun rename(oldPath: String, newPath: String): Boolean {
        return rename(File(oldPath), File(newPath))
    }

    /**
     * 文件重命名
     */
    fun rename(src: File, tar: File): Boolean {
        return src.renameTo(tar)
    }

    /**
     * 读取 assets 目录下的文件内容
     * @param fileName 文件名或相对路径（例如：`"data/config.json"`）
     * @return 文件内容字符串
     * @throws IOException 文件不存在或读取失败时抛出
     */
    @Throws(IOException::class)
    fun readFromAssets(fileName: String): String {
        return appCtx.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    /**
     * 读取文本文件, 失败将返回空串
     */
    @JvmOverloads
    fun readText(filepath: String, charset: String = "utf-8"): String {
        try {
            val data = readBytes(filepath)
            if (data != null) {
                return String(data, Charset.forName(charset)).trim { it <= ' ' }
            }
        } catch (ignored: UnsupportedEncodingException) {
        }

        return ""
    }

    /**
     * 读取文件内容, 失败将返回空串
     */
    fun readBytes(filepath: String): ByteArray? {
        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(filepath)
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (true) {
                val len = fis.read(buffer, 0, buffer.size)
                if (len == -1) {
                    break
                } else {
                    outputStream.write(buffer, 0, len)
                }
            }
            val data = outputStream.toByteArray()
            outputStream.close()
            return data
        } catch (e: IOException) {
            return null
        } finally {
            closeSilently(fis)
        }
    }

    /**
     * 保存文本内容
     */
    @JvmOverloads
    fun writeText(filepath: String, content: String, charset: String = "utf-8"): Boolean {
        return try {
            writeBytes(filepath, content.toByteArray(charset(charset)))
        } catch (e: UnsupportedEncodingException) {
            false
        }

    }

    /**
     * 保存文件内容
     */
    fun writeBytes(filepath: String, data: ByteArray): Boolean {
        val file = File(filepath)
        var fos: FileOutputStream? = null
        return try {
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
            fos = FileOutputStream(filepath)
            fos.write(data)
            true
        } catch (e: IOException) {
            false
        } finally {
            closeSilently(fos)
        }
    }

    /**
     * 保存文件内容
     */
    fun writeInputStream(filepath: String, data: InputStream): Boolean {
        val file = File(filepath)
        return writeInputStream(file, data)
    }

    /**
     * 保存文件内容
     */
    fun writeInputStream(file: File, data: InputStream): Boolean {
        return try {
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
            data.use {
                FileOutputStream(file).use { fos ->
                    data.copyTo(fos)
                    fos.flush()
                }
            }
            true
        } catch (e: IOException) {
            false
        }
    }

    /**
     * 追加文本内容
     */
    fun appendText(path: String, content: String): Boolean {
        val file = File(path)
        var writer: FileWriter? = null
        return try {
            if (!file.exists()) {
                file.createNewFile()
            }
            writer = FileWriter(file, true)
            writer.write(content)
            true
        } catch (e: IOException) {
            false
        } finally {
            closeSilently(writer)
        }
    }

    /**
     * 获取文件大小
     */
    fun getLength(path: String): Long {
        val file = File(path)
        return if (!file.isFile || !file.exists()) {
            0
        } else file.length()
    }

    /**
     * 获取文件或网址的名称（包括后缀）
     */
    fun getName(path: String?): String {
        if (path == null) {
            return ""
        }
        val pos = path.lastIndexOf(File.separator)
        return if (0 <= pos) {
            path.substring(pos + 1)
        } else {
            path
        }
    }

    /**
     * 获取文件名（不包括扩展名）
     */
    fun getNameExcludeExtension(path: String): String {
        return try {
            var fileName = File(path).name
            val lastIndexOf = fileName.lastIndexOf(".")
            if (lastIndexOf != -1) {
                fileName = fileName.substring(0, lastIndexOf)
            }
            fileName
        } catch (e: Exception) {
            ""
        }

    }

    /**
     * 获取格式化后的文件大小
     */
    fun getSize(path: String): String {
        val fileSize = getLength(path)
        return ConvertUtils.formatFileSize(fileSize)
    }

    /**
     * 获取文件后缀,不包括“.”
     */
    fun getExtension(pathOrUrl: String): String {
        val dotPos = pathOrUrl.lastIndexOf('.')
        return if (0 <= dotPos) {
            pathOrUrl.substring(dotPos + 1)
        } else {
            "ext"
        }
    }

    /**
     * 获取文件的MIME类型
     */
    fun getMimeType(pathOrUrl: String): String {
        val ext = getExtension(pathOrUrl)
        val map = MimeTypeMap.getSingleton()
        return map.getMimeTypeFromExtension(ext) ?: "*/*"
    }

    /**
     * 获取格式化后的文件/目录创建或最后修改时间
     */
    @JvmOverloads
    fun getDateTime(path: String, format: String = "yyyy年MM月dd日HH:mm"): String {
        val file = File(path)
        return getDateTime(file, format)
    }

    /**
     * 获取格式化后的文件/目录创建或最后修改时间
     */
    fun getDateTime(file: File, format: String): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = file.lastModified()
        return SimpleDateFormat(format, Locale.PRC).format(cal.time)
    }

    /**
     * 比较两个文件的最后修改时间
     */
    fun compareLastModified(path1: String, path2: String): Int {
        val stamp1 = File(path1).lastModified()
        val stamp2 = File(path2).lastModified()
        return when {
            stamp1 > stamp2 -> 1
            stamp1 < stamp2 -> -1
            else -> 0
        }
    }

    /**
     * 创建多级别的目录
     */
    fun makeDirs(path: String): Boolean {
        return makeDirs(File(path))
    }

    /**
     * 创建多级别的目录
     */
    fun makeDirs(file: File): Boolean {
        return file.mkdirs()
    }

    class SortByExtension : Comparator<File> {

        override fun compare(f1: File?, f2: File?): Int {
            return if (f1 == null || f2 == null) {
                if (f1 == null) -1 else 1
            } else {
                if (f1.isDirectory && f2.isFile) {
                    -1
                } else if (f1.isFile && f2.isDirectory) {
                    1
                } else {
                    f1.name.compareTo(f2.name, ignoreCase = true)
                }
            }
        }

    }

    class SortByName : Comparator<File> {
        private var caseSensitive: Boolean = false

        constructor(caseSensitive: Boolean) {
            this.caseSensitive = caseSensitive
        }

        constructor() {
            this.caseSensitive = false
        }

        override fun compare(f1: File?, f2: File?): Int {
            if (f1 == null || f2 == null) {
                return if (f1 == null) {
                    -1
                } else {
                    1
                }
            } else {
                return if (f1.isDirectory && f2.isFile) {
                    -1
                } else if (f1.isFile && f2.isDirectory) {
                    1
                } else {
                    val s1 = f1.name
                    val s2 = f2.name
                    if (caseSensitive) {
                        s1.cnCompare(s2)
                    } else {
                        s1.compareTo(s2, ignoreCase = true)
                    }
                }
            }
        }

    }

    class SortBySize : Comparator<File> {

        override fun compare(f1: File?, f2: File?): Int {
            return if (f1 == null || f2 == null) {
                if (f1 == null) {
                    -1
                } else {
                    1
                }
            } else {
                if (f1.isDirectory && f2.isFile) {
                    -1
                } else if (f1.isFile && f2.isDirectory) {
                    1
                } else {
                    if (f1.length() < f2.length()) {
                        -1
                    } else {
                        1
                    }
                }
            }
        }

    }

    class SortByTime : Comparator<File> {

        override fun compare(f1: File?, f2: File?): Int {
            return if (f1 == null || f2 == null) {
                if (f1 == null) {
                    -1
                } else {
                    1
                }
            } else {
                if (f1.isDirectory && f2.isFile) {
                    -1
                } else if (f1.isFile && f2.isDirectory) {
                    1
                } else {
                    if (f1.lastModified() > f2.lastModified()) {
                        -1
                    } else {
                        1
                    }
                }
            }
        }

    }
}
