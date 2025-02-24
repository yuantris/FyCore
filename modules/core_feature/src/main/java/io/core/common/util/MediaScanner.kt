package io.core.common.util

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Looper
import android.provider.MediaStore
import com.hjq.permissions.Permission
import io.core.appCtx
import io.core.common.util.extensions.cool.hasReadStoragePermission
import io.core.common.util.extensions.cool.isGranted
import io.core.common.util.log.LogPure
import io.core.common.util.tools.buildMainHandler

/**
 * 媒体库扫描工具
 * 该工具类提供了查询媒体库文件的功能，支持按文件类型、过滤条件和排序顺序查询文件信息。
 * @author FFGreatKing
 */
class MediaScanner {

    companion object {

        private const val CACHE_EXPIRY_MS = 5 * 60 * 1000 // 5分钟缓存有效期

        private val PROJECTION = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE
        )

        private val EXCLUDED_PATTERNS = listOf(
            "^/storage/emulated/\\d+/Android/.*",
            "^/storage/emulated/\\d+/.*cache.*",
            "^/storage/emulated/\\d+/\\..*",
            "thumbnails"
        ).map { Regex(it, RegexOption.IGNORE_CASE) }

        // 缓存状态跟踪
        private var cachedResults: List<FileInfo> = emptyList()
        private var lastQueryParams: Triple<Set<FileType>, String, Long>? = null
        private var contentObserver: ContentObserver? = null


        /**
         * 带缓存的媒体库查询
         * @param forceRefresh 是否强制刷新缓存
         */
        @JvmStatic
        @JvmOverloads
        fun queryFiles(
            types: Set<FileType>,
            addFilter: ((FileInfo) -> Boolean)? = null,
            sortOrder: String = SQL.timeAddedDESC,
            forceRefresh: Boolean = false
        ): List<FileInfo> {
            if (!appCtx.hasReadStoragePermission()) {
                throw IllegalStateException("No permission to read external storage")
            }

            val currentParams = Triple(types, sortOrder, System.currentTimeMillis())

            return when {
                forceRefresh -> refreshAndGet(currentParams, addFilter)
                isCacheValid(currentParams) -> {
                    LogPure.d { "MediaScanner---- Using cached results" }
                    processResults(cachedResults, addFilter)
                }

                else -> refreshAndGet(currentParams, addFilter)
            }
        }

        /**
         * 手动清除缓存
         */
        @JvmStatic
        fun clearCache() {
            cachedResults = emptyList()
            lastQueryParams = null
            LogPure.v { "MediaScanner---- Cache cleared" }
        }

        /**
         * 注册内容观察者自动刷新缓存
         */
        @JvmStatic
        fun registerContentObserver() {
            if (contentObserver == null) {
                contentObserver = object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        if (!selfChange) {
                            LogPure.v { "MediaScanner---- Content changed, invalidating cache" }
                            clearCache()
                        }
                    }
                }

                // 注册所有相关URI的监听
                val uris = FileType.values().map { it.contentUri }.toSet()
                uris.forEach { uri ->
                    appCtx.contentResolver.registerContentObserver(
                        uri,
                        true,
                        contentObserver!!
                    )
                }
                LogPure.d { "MediaScanner---- Content observer registered" }
            }
        }

        /**
         * 注销内容观察者
         */
        @JvmStatic
        fun unregisterContentObserver() {
            contentObserver?.let {
                appCtx.contentResolver.unregisterContentObserver(it)
                contentObserver = null
                LogPure.d { "MediaScanner---- Content observer unregistered" }
            }
        }

        private fun refreshAndGet(
            params: Triple<Set<FileType>, String, Long>,
            filter: ((FileInfo) -> Boolean)?
        ): List<FileInfo> {
            val (types, sortOrder, timestamp) = params

            val results = mutableListOf<FileInfo>().apply {
                types.groupBy { it.contentUri }.forEach { (uri, fileTypes) ->
                    queryMediaStore(uri, fileTypes, sortOrder)?.let { addAll(it) }
                }
            }

            cachedResults = processResults(results, null) // 基础缓存不过滤用户条件
            lastQueryParams = params.copy(third = System.currentTimeMillis())

            LogPure.d { "MediaScanner---- Cache updated (${cachedResults.size} items)" }
            return processResults(cachedResults, filter)
        }

        private fun queryMediaStore(
            uri: Uri,
            fileTypes: List<FileType>,
            sortOrder: String
        ): List<FileInfo>? {
            val mimeTypes = fileTypes.flatMap { it.mimeTypes }.distinct()
            val extensions = fileTypes.flatMap { it.extensions }.distinct()

            return appCtx.contentResolver.query(
                uri,
                PROJECTION,
                buildSelection(mimeTypes, extensions),
                buildSelectionArgs(mimeTypes, extensions)?.toTypedArray(),
                sortOrder
            )?.use { cursor ->
                val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                generateSequence { if (cursor.moveToNext()) cursor else null }
                    .map {
                        FileInfo(
                            path = it.getString(pathIndex) ?: return@map null,
                            size = it.getLong(sizeIndex),
                            dateAdded = it.getLong(dateIndex),
                            mimeType = it.getString(mimeIndex)
                        )
                    }
                    .filterNotNull()
                    .toList()
            }
        }

        private fun isCacheValid(currentParams: Triple<Set<FileType>, String, Long>): Boolean {
            return lastQueryParams?.let { (cachedTypes, cachedSort, cachedTime) ->
                currentParams.first == cachedTypes &&
                        currentParams.second == cachedSort &&
                        (currentParams.third - cachedTime) < CACHE_EXPIRY_MS
            } ?: false
        }

        private fun processResults(
            results: List<FileInfo>,
            addFilter: ((FileInfo) -> Boolean)?
        ): List<FileInfo> {
            return results
                .distinctBy { it.path }
                .filter { file ->
                    val defaultPass = defaultFilter(file)
                    val additionalPass = addFilter?.invoke(file) ?: true
                    defaultPass && additionalPass
                }
                .also {
                    LogPure.v { "MediaScanner---- Final results: ${it.size} items after filtering" }
                }
        }

        private fun defaultFilter(file: FileInfo): Boolean {
            return file.size > 0 && EXCLUDED_PATTERNS.none { it.containsMatchIn(file.path) }
        }

        private fun buildSelection(mimeTypes: List<String>, extensions: List<String>): String? {
            val conditions = mutableListOf<String>()

            if (mimeTypes.isNotEmpty()) {
                conditions.add("${MediaStore.MediaColumns.MIME_TYPE} IN (${mimeTypes.joinToString(", ") { "?" }})")
            }

            if (extensions.isNotEmpty()) {
                conditions.add(extensions.joinToString(" OR ") { "${MediaStore.MediaColumns.DATA} LIKE ?" })
            }

            return if (conditions.isNotEmpty()) conditions.joinToString(" OR ") else null
        }

        private fun buildSelectionArgs(
            mimeTypes: List<String>,
            extensions: List<String>
        ): List<String>? {
            val args = mutableListOf<String>()
            args.addAll(mimeTypes)
            args.addAll(extensions.map { "%.$it" })
            return args.ifEmpty { null }
        }

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