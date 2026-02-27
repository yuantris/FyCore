package com.core.fy.android.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaRefreshHelper {

    /**
     * 刷新指定文件或目录的媒体库记�?
     * @param context 上下文对�?
     * @param filePaths 需要刷新的文件路径列表
     */
    suspend fun refreshMediaStore(
        context: Context,
        filePaths: List<String>
    ) = withContext(Dispatchers.IO) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+ 使用 MediaStore API
                filePaths.forEach { path ->
                    refreshUsingMediaStoreAPI(context, File(path))
                }
            }
            else -> {
                // 旧版本使用传�?MediaScanner
                scanWithMediaScanner(context, filePaths)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun refreshUsingMediaStoreAPI(context: Context, file: File) {
        try {
            if (!file.exists()) return

            val resolver = context.contentResolver
            val collection = getMediaCollectionUri(file)
            val values = createContentValues(file)

            // 尝试更新现有记录
            val updated = updateExistingRecord(resolver, collection, file, values)
            
            if (!updated) {
                // 插入新记�?
                insertNewRecord(resolver, collection, values)
            }
        } catch (e: Exception) {
            // 处理异常
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getMediaCollectionUri(file: File): Uri {
        return when (file.getMediaType()) {
            MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createContentValues(file: File): ContentValues {
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.RELATIVE_PATH, getRelativePath(file))
            put(MediaStore.MediaColumns.SIZE, file.length())
            put(MediaStore.MediaColumns.DATE_MODIFIED, file.lastModified() / 1000)
            put(MediaStore.MediaColumns.MIME_TYPE, file.path.getMimeType())
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateExistingRecord(
        resolver: ContentResolver,
        collection: Uri,
        file: File,
        values: ContentValues
    ): Boolean {
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND " +
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        
        val selectionArgs = arrayOf(
            getRelativePath(file),
            file.name
        )

        return resolver.update(
            collection,
            values,
            selection,
            selectionArgs
        ) > 0
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertNewRecord(
        resolver: ContentResolver,
        collection: Uri,
        values: ContentValues
    ) {
        try {
            resolver.insert(collection, values)?.also { uri ->
                // 更新 IS_PENDING 状态（如果适用�?
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        } catch (e: Exception) {
            // 处理插入失败
        }
    }

    private suspend fun scanWithMediaScanner(
        context: Context,
        filePaths: List<String>
    ) = suspendCoroutine<Unit> { continuation ->
        val mimeTypes = filePaths.map { it.getMimeType() }.toTypedArray()
        
        MediaScannerConnection.scanFile(
            context.applicationContext,
            filePaths.toTypedArray(),
            mimeTypes
        ) { _, _ ->
            continuation.resume(Unit)
        }
    }

    // region Helper Extensions
    private fun String.getMimeType(): String {
        val extension = substringAfterLast('.', "")
        return MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension) ?: "*/*"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getRelativePath(file: File): String {
        val externalDir = Environment.getExternalStorageDirectory().path
        return file.parent?.removePrefix(externalDir)?.trimStart('/') ?: ""
    }

    private fun File.getMediaType(): MediaType {
        return when {
            path.contains(Environment.DIRECTORY_DCIM, true) -> MediaType.IMAGE
            path.contains(Environment.DIRECTORY_MOVIES, true) -> MediaType.VIDEO
            path.contains(Environment.DIRECTORY_MUSIC, true) -> MediaType.AUDIO
            else -> MediaType.OTHER
        }
    }

    private enum class MediaType { IMAGE, VIDEO, AUDIO, OTHER }
    // endregion
}