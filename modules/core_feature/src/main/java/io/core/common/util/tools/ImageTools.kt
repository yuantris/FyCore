package io.core.common.util.tools

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import io.core.appCtx
import io.core.common.helper.valid.NullCheck
import io.core.common.util.CoreUtil
import io.core.common.util.extensions.cool.hasWriteStoragePermission
import io.core.common.util.extensions.cool.isEmpty
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

object ImageTools {

    @JvmStatic
    @JvmOverloads
    fun save(
        src: Bitmap?,
        file: File,
        format: Bitmap.CompressFormat,
        quality: Int = 100,
        recycle: Boolean = false
    ): Boolean {
        if (null == src) {
            Log.e(ImageTools.javaClass.simpleName, "bitmap is null.")
            return false
        }
        if (src.isEmpty()) {
            Log.e(ImageTools.javaClass.simpleName, "bitmap is empty.")
            return false
        }
        if (src.isRecycled) {
            Log.e(ImageTools.javaClass.simpleName, "bitmap is recycled.")
            return false
        }
        var os: OutputStream? = null
        var ret = false
        try {
            os = BufferedOutputStream(FileOutputStream(file))
            ret = src.compress(format, quality, os)
            if (recycle && !src.isRecycled) src.recycle()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                os?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return ret
    }

    @JvmStatic
    @JvmOverloads
    fun save2Album(
        src: Bitmap?,
        format: Bitmap.CompressFormat,
        dirName: String? = null,
        quality: Int = 100,
        recycle: Boolean = false
    ): File? {
        if (null == src) return null
        val safeDirName = if (NullCheck.isEmpty(dirName)) appCtx.packageName else dirName!!
        val suffix = if (Bitmap.CompressFormat.JPEG == format) "JPG" else format.name
        val fileName = System.currentTimeMillis().toString() + "_" + quality + "." + suffix
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!appCtx.hasWriteStoragePermission()) return null

            val picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val destFile = File(picDir, "$safeDirName/$fileName")
            if (!save(src, destFile, format, quality, recycle)) {
                return null
            }
            CoreUtil.Files.refreshMediaLibrary(destFile)
            return destFile
        } else {
            val contentValues = ContentValues()
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/*")
            val contentUri =
                if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
                }
            contentValues.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM + File.separator + safeDirName
            )
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1)
            val uri: Uri = appCtx.contentResolver
                .insert(contentUri, contentValues)
                ?: return null
            try {
                appCtx.contentResolver.openOutputStream(uri).use {
                    if (it == null) return null
                    src.compress(format, quality, it)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                appCtx.contentResolver.update(uri, contentValues, null, null)

                return UriTools.uri2File(uri)
            } catch (e: Exception) {
                appCtx.contentResolver.delete(uri, null, null)
                e.printStackTrace()
                return null
            }
        }
    }
}
