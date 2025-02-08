package io.core.common.util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.telephony.mbms.FileInfo
import io.core.common.util.ext.logE
import io.core.common.util.log.LogPure
import io.core.common.util.log.logW
import io.core.constant.FileType
import java.util.Locale.filter

class MediaScanner(private val context: Context) {

    fun queryMediaFiles(
        types: Set<FileType>,
        addFilter: (FileInfo) -> Boolean = { true },
        sortOrder: String = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
    ): List<FileInfo> {
        val groupedTypes = types.groupBy { it.contentUri }
        val results = mutableListOf<FileInfo>()

        groupedTypes.forEach { (uri, fileTypes) ->
            val mimeTypes = fileTypes.flatMap { it.mimeTypes }.distinct()
            val extensions = fileTypes.flatMap { it.extensions }.distinct()

            val selection = buildSelection(mimeTypes, extensions)
            val selectionArgs = buildSelectionArgs(mimeTypes, extensions)

            context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.MediaColumns.DATA,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_ADDED,
                    MediaStore.MediaColumns.MIME_TYPE
                ),
                selection,
                selectionArgs?.toTypedArray(),
                sortOrder
            )?.use { cursor ->
                val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val mimeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(pathIndex)
                    val size = cursor.getLong(sizeIndex)
                    val date = cursor.getLong(dateIndex)
                    val mime = cursor.getString(mimeIndex)

                    if (!path.isNullOrEmpty()) {
                        results.add(FileInfo(path, size, date, mime))
                    }
                }
            }
        }
        val sortedByDescending = results.distinctBy { it.path }
            .filter { file ->
                val defaultShouldKeep = defaultFilter(file)
                val additionalShouldKeep = addFilter(file)
                val shouldKeep = defaultShouldKeep && additionalShouldKeep
                if (!shouldKeep) {
                    ("Filtered out: ${file.path}").logW()
                }
                shouldKeep
            }
            .sortedByDescending { it.dateAdded }
        return sortedByDescending
    }

    private fun buildSelection(mimeTypes: List<String>, extensions: List<String>): String? {
        val conditions = mutableListOf<String>()
        
        if (mimeTypes.isNotEmpty()) {
            conditions.add("${MediaStore.MediaColumns.MIME_TYPE} IN (${mimeTypes.map { "?" }.joinToString(", ")})")
        }
        
        if (extensions.isNotEmpty()) {
            conditions.add(extensions.joinToString(" OR ") { "${MediaStore.MediaColumns.DATA} LIKE ?" })
        }

        return if (conditions.isNotEmpty()) conditions.joinToString(" OR ") else null
    }

    private fun buildSelectionArgs(mimeTypes: List<String>, extensions: List<String>): List<String>? {
        val args = mutableListOf<String>()
        args.addAll(mimeTypes)
        args.addAll(extensions.map { "%.$it" })
        return args.ifEmpty { null }
    }

    private fun defaultFilter(file: FileInfo): Boolean {
        val excludedPaths = listOf("/Android/", "/cache/", "/.", "thumbnails")
        return file.size > 0 && excludedPaths.none { file.path.contains(it, true) }
    }

    data class FileInfo(
        val path: String,
        val size: Long,
        val dateAdded: Long,
        val mimeType: String?
    )

    enum class FileType(
        val mimeTypes: List<String>,
        val extensions: List<String>,
        val contentUri: Uri
    ) {
        // 图片类型
        JPG(
            listOf("image/jpeg"),
            listOf("jpg", "jpeg"),
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ),
        PNG(
            listOf("image/png"),
            listOf("png"),
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ),
        GIF(
            listOf("image/gif"),
            listOf("gif"),
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ),
        BMP(
            listOf("image/bmp"),
            listOf("bmp"),
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ),
        WEBP(
            listOf("image/webp"),
            listOf("webp"),
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ),
        TIFF(
            listOf("image/tiff"),
            listOf("tiff", "tif"),
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ),

        // 音频类型
        MP3(
            listOf("audio/mpeg"),
            listOf("mp3"),
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        ),
        WAV(
            listOf("audio/wav", "audio/x-wav"),
            listOf("wav"),
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        ),
        OGG(
            listOf("audio/ogg"),
            listOf("ogg"),
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        ),
        FLAC(
            listOf("audio/flac"),
            listOf("flac"),
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        ),
        M4A(
            listOf("audio/mp4", "audio/m4a"),
            listOf("m4a"),
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        ),

        // 视频类型
        MP4(
            listOf("video/mp4"),
            listOf("mp4"),
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ),
        AVI(
            listOf("video/x-msvideo"),
            listOf("avi"),
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ),
        MKV(
            listOf("video/x-matroska"),
            listOf("mkv"),
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ),
        MOV(
            listOf("video/quicktime"),
            listOf("mov"),
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ),

        // 文本文档
        TXT(
            listOf("text/plain"),
            listOf("txt"),
            MediaStore.Files.getContentUri("external")
        ),
        CSV(
            listOf("text/csv"),
            listOf("csv"),
            MediaStore.Files.getContentUri("external")
        ),
        HTML(
            listOf("text/html"),
            listOf("html", "htm"),
            MediaStore.Files.getContentUri("external")
        ),
        XML(
            listOf("application/xml", "text/xml"),
            listOf("xml"),
            MediaStore.Files.getContentUri("external")
        ),
        JSON(
            listOf("application/json"),
            listOf("json"),
            MediaStore.Files.getContentUri("external")
        ),

        // Office 文档
        DOC(
            listOf("application/msword"),
            listOf("doc"),
            MediaStore.Files.getContentUri("external")
        ),
        DOCX(
            listOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            listOf("docx"),
            MediaStore.Files.getContentUri("external")
        ),
        XLSX(
            listOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            listOf("xlsx"),
            MediaStore.Files.getContentUri("external")
        ),
        PPTX(
            listOf("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            listOf("pptx"),
            MediaStore.Files.getContentUri("external")
        ),
        RTF(
            listOf("application/rtf"),
            listOf("rtf"),
            MediaStore.Files.getContentUri("external")
        ),
        PDF(
            listOf("application/pdf"),
            listOf("pdf"),
            MediaStore.Files.getContentUri("external")
        ),

        // 压缩包和其他格式
        ZIP(
            listOf("application/zip"),
            listOf("zip"),
            MediaStore.Files.getContentUri("external")
        ),
        RAR(
            listOf("application/x-rar-compressed"),
            listOf("rar"),
            MediaStore.Files.getContentUri("external")
        ),
        SEVEN_ZIP(
            listOf("application/x-7z-compressed"),
            listOf("7z"),
            MediaStore.Files.getContentUri("external")
        ),
        EPUB(
            listOf("application/epub+zip"),
            listOf("epub"),
            MediaStore.Files.getContentUri("external")
        ),
        APK(
            listOf("application/vnd.android.package-archive"),
            listOf("apk"),
            MediaStore.Files.getContentUri("external")
        )
    }
}