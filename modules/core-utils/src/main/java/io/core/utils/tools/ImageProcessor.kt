@file:Suppress("unused")

package io.core.utils.tools

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.DrawableRes
import io.core.base.appCtx
import io.core.common.helper.valid.NullCheck
import io.core.utils.CoreUtil
import io.core.utils.extensions.cool.hasWriteStoragePermission
import io.core.utils.extensions.cool.isEmpty
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import androidx.core.graphics.scale

/**
 * 统一的图片处理工具类
 * 提供图片解码、保存、转换等功能
 */
@Suppress("WeakerAccess", "MemberVisibilityCanBePrivate")
object ImageProcessor {

    private const val TAG = "ImageProcessor"
    private const val DEFAULT_QUALITY = 100
    private const val DEFAULT_COMPRESS_QUALITY = 90

    /**
     * 创建解码构建�?
     */
    @JvmStatic
    fun decode(): DecodeBuilder = DecodeBuilder()

    /**
     * 创建保存构建�?
     */
    @JvmStatic
    fun save(): SaveBuilder = SaveBuilder()

    /**
     * 创建转换构建�?
     */
    @JvmStatic
    fun transform(): TransformBuilder = TransformBuilder()

    /**
     * 验证Bitmap是否有效
     */
    private fun validateBitmap(bitmap: Bitmap?): Boolean {
        if (bitmap == null) {
            Log.e(TAG, "bitmap is null.")
            return false
        }
        if (bitmap.isEmpty()) {
            Log.e(TAG, "bitmap is empty.")
            return false
        }
        if (bitmap.isRecycled) {
            Log.e(TAG, "bitmap is recycled.")
            return false
        }
        return true
    }

    /**
     * 计算采样�?
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int?,
        reqHeight: Int?
    ): Int {
        val wRatio = reqWidth?.let { options.outWidth / it } ?: -1
        val hRatio = reqHeight?.let { options.outHeight / it } ?: -1
        return when {
            wRatio > 1 && hRatio > 1 -> max(wRatio, hRatio)
            wRatio > 1 -> wRatio
            hRatio > 1 -> hRatio
            else -> 1
        }
    }

    /**
     * 计算初始采样�?
     */
    private fun computeInitialSampleSize(
        options: BitmapFactory.Options,
        minSideLength: Int,
        maxNumOfPixels: Int
    ): Int {
        val w = options.outWidth.toDouble()
        val h = options.outHeight.toDouble()

        val lowerBound = when (maxNumOfPixels) {
            -1 -> 1
            else -> ceil(sqrt(w * h / maxNumOfPixels)).toInt()
        }

        val upperBound = when (minSideLength) {
            -1 -> 128
            else -> min(
                floor(w / minSideLength),
                floor(h / minSideLength)
            ).toInt()
        }

        if (upperBound < lowerBound) {
            return lowerBound
        }

        return when {
            maxNumOfPixels == -1 && minSideLength == -1 -> 1
            minSideLength == -1 -> lowerBound
            else -> upperBound
        }
    }

    /**
     * 计算采样率（兼容旧方法）
     */
    private fun computeSampleSize(
        options: BitmapFactory.Options,
        minSideLength: Int,
        maxNumOfPixels: Int
    ): Int {
        val initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels)
        var roundedSize: Int
        if (initialSize <= 8) {
            roundedSize = 1
            while (roundedSize < initialSize) {
                roundedSize = roundedSize shl 1
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8
        }
        return roundedSize
    }

    /**
     * 图片解码构建�?
     */
    class DecodeBuilder {
        private var sourcePath: String? = null
        private var sourceResId: Int? = null
        private var sourceAssets: String? = null
        private var reqWidth: Int? = null
        private var reqHeight: Int? = null
        private var config: Bitmap.Config = Bitmap.Config.ARGB_8888
        private var isLowMemory: Boolean = false

        /**
         * 从文件路径解�?
         */
        fun fromFile(path: String): DecodeBuilder {
            this.sourcePath = path
            return this
        }

        /**
         * 从资源ID解码
         */
        fun fromResource(@DrawableRes resId: Int): DecodeBuilder {
            this.sourceResId = resId
            return this
        }

        /**
         * 从Assets解码
         */
        fun fromAssets(fileName: String): DecodeBuilder {
            this.sourceAssets = fileName
            return this
        }

        /**
         * 设置目标尺寸
         */
        fun withSize(width: Int, height: Int? = null): DecodeBuilder {
            this.reqWidth = width
            this.reqHeight = height
            return this
        }

        /**
         * 设置Bitmap配置
         */
        fun withConfig(config: Bitmap.Config): DecodeBuilder {
            this.config = config
            return this
        }

        /**
         * 使用低内存模�?
         */
        fun withLowMemory(): DecodeBuilder {
            this.isLowMemory = true
            this.config = Bitmap.Config.RGB_565
            return this
        }

        /**
         * 构建Bitmap
         */
        fun build(): Bitmap? {
            return try {
                when {
                    sourcePath != null -> decodeFromFile()
                    sourceResId != null -> decodeFromResource()
                    sourceAssets != null -> decodeFromAssets()
                    else -> {
                        Log.e(TAG, "No source specified for decoding")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding bitmap", e)
                null
            }
        }

        private fun decodeFromFile(): Bitmap? {
            val path = sourcePath ?: return null
            val fis = FileInputStream(path)
            return fis.use {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFileDescriptor(fis.fd, null, options)

                if (reqWidth != null || reqHeight != null) {
                    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                } else {
                    options.inSampleSize = computeSampleSize(options, -1, 128 * 128)
                }

                options.inJustDecodeBounds = false
                options.inPreferredConfig = config
                BitmapFactory.decodeFileDescriptor(fis.fd, null, options)
            }
        }

        private fun decodeFromResource(): Bitmap? {
            val resId = sourceResId ?: return null
            val options = BitmapFactory.Options()
            options.inPreferredConfig = config

            if (reqWidth != null && reqHeight != null) {
                options.inJustDecodeBounds = true
                BitmapFactory.decodeResource(appCtx.resources, resId, options)
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
            }

            return BitmapFactory.decodeResource(appCtx.resources, resId, options)
        }

        private fun decodeFromAssets(): Bitmap? {
            val fileName = sourceAssets ?: return null
            var inputStream = appCtx.assets.open(fileName)
            return inputStream.use {
                val options = BitmapFactory.Options()
                
                if (reqWidth != null && reqHeight != null) {
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeStream(inputStream, null, options)
                    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                    inputStream = appCtx.assets.open(fileName)
                    options.inJustDecodeBounds = false
                }
                
                options.inPreferredConfig = config
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        }
    }

    /**
     * 图片保存构建�?
     */
    class SaveBuilder {
        private var bitmap: Bitmap? = null
        private var targetFile: File? = null
        private var albumDirName: String? = null
        private var format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
        private var quality: Int = DEFAULT_QUALITY
        private var shouldRecycle: Boolean = false
        private var saveToAlbum: Boolean = false

        /**
         * 设置要保存的Bitmap
         */
        fun bitmap(bitmap: Bitmap?): SaveBuilder {
            this.bitmap = bitmap
            return this
        }

        /**
         * 保存到指定文�?
         */
        fun toFile(file: File): SaveBuilder {
            this.targetFile = file
            this.saveToAlbum = false
            return this
        }

        /**
         * 保存到相�?
         */
        fun toAlbum(dirName: String? = null): SaveBuilder {
            this.albumDirName = dirName
            this.saveToAlbum = true
            return this
        }

        /**
         * 设置压缩格式
         */
        fun withFormat(format: Bitmap.CompressFormat): SaveBuilder {
            this.format = format
            return this
        }

        /**
         * 设置压缩质量
         */
        fun withQuality(quality: Int): SaveBuilder {
            this.quality = quality
            return this
        }

        /**
         * 设置是否回收Bitmap
         */
        fun withRecycle(recycle: Boolean): SaveBuilder {
            this.shouldRecycle = recycle
            return this
        }

        /**
         * 执行保存操作
         */
        fun execute(): Any? {
            return if (saveToAlbum) {
                executeAlbumSave()
            } else {
                executeFileSave()
            }
        }

        private fun executeFileSave(): Boolean {
            val bmp = bitmap
            val file = targetFile
            
            if (!validateBitmap(bmp) || file == null) {
                return false
            }

            var os: OutputStream? = null
            var ret = false
            try {
                os = BufferedOutputStream(FileOutputStream(file))
                ret = bmp!!.compress(format, quality, os)
                if (shouldRecycle && !bmp.isRecycled) bmp.recycle()
            } catch (e: IOException) {
                Log.e(TAG, "Error saving bitmap to file", e)
            } finally {
                try {
                    os?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing output stream", e)
                }
            }
            return ret
        }

        private fun executeAlbumSave(): File? {
            val bmp = bitmap
            if (!validateBitmap(bmp)) return null

            val safeDirName = if (NullCheck.isEmpty(albumDirName)) appCtx.packageName else albumDirName!!
            val suffix = if (Bitmap.CompressFormat.JPEG == format) "JPG" else format.name
            val fileName = System.currentTimeMillis().toString() + "_" + quality + "." + suffix

            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                saveToAlbumLegacy(bmp!!, safeDirName, fileName)
            } else {
                saveToAlbumModern(bmp!!, safeDirName, fileName)
            }
        }

        private fun saveToAlbumLegacy(bmp: Bitmap, dirName: String, fileName: String): File? {
            if (!appCtx.hasWriteStoragePermission()) return null

            val picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val destFile = File(picDir, "$dirName/$fileName")
            
            if (!saveToFile(bmp, destFile)) {
                return null
            }
            
            CoreUtil.Files.refreshMediaLibrary(destFile)
            return destFile
        }

        private fun saveToAlbumModern(bmp: Bitmap, dirName: String, fileName: String): File? {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_DCIM + File.separator + dirName
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val contentUri = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.INTERNAL_CONTENT_URI
            }

            val uri: Uri = appCtx.contentResolver.insert(contentUri, contentValues) ?: return null

            try {
                appCtx.contentResolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream == null) return null
                    bmp.compress(format, quality, outputStream)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                appCtx.contentResolver.update(uri, contentValues, null, null)

                if (shouldRecycle && !bmp.isRecycled) bmp.recycle()
                return UriTools.uri2File(uri)
            } catch (e: Exception) {
                appCtx.contentResolver.delete(uri, null, null)
                Log.e(TAG, "Error saving bitmap to album", e)
                return null
            }
        }

        private fun saveToFile(bmp: Bitmap, file: File): Boolean {
            var os: OutputStream? = null
            var ret = false
            try {
                file.parentFile?.mkdirs()
                os = BufferedOutputStream(FileOutputStream(file))
                ret = bmp.compress(format, quality, os)
                if (shouldRecycle && !bmp.isRecycled) bmp.recycle()
            } catch (e: IOException) {
                Log.e(TAG, "Error saving bitmap to file", e)
            } finally {
                try {
                    os?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing output stream", e)
                }
            }
            return ret
        }
    }

    /**
     * 图片转换构建�?
     */
    class TransformBuilder {
        private var bitmap: Bitmap? = null

        /**
         * 设置要转换的Bitmap
         */
        fun bitmap(bitmap: Bitmap?): TransformBuilder {
            this.bitmap = bitmap
            return this
        }

        /**
         * 转换为InputStream
         */
        fun toInputStream(quality: Int = DEFAULT_COMPRESS_QUALITY): InputStream? {
            val bmp = bitmap
            if (!validateBitmap(bmp)) return null

            val bos = ByteArrayOutputStream()
            bmp!!.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            return ByteArrayInputStream(bos.toByteArray()).also { 
                try {
                    bos.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing ByteArrayOutputStream", e)
                }
            }
        }

        /**
         * 转换为字节数�?
         */
        fun toByteArray(
            format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
            quality: Int = DEFAULT_COMPRESS_QUALITY
        ): ByteArray? {
            val bmp = bitmap
            if (!validateBitmap(bmp)) return null

            val bos = ByteArrayOutputStream()
            return try {
                bmp!!.compress(format, quality, bos)
                bos.toByteArray()
            } catch (e: Exception) {
                Log.e(TAG, "Error converting bitmap to byte array", e)
                null
            } finally {
                try {
                    bos.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing ByteArrayOutputStream", e)
                }
            }
        }

        /**
         * 创建缩放后的Bitmap
         */
        fun createScaled(width: Int, height: Int, filter: Boolean = true): Bitmap? {
            val bmp = bitmap
            if (!validateBitmap(bmp)) return null

            return try {
                bmp!!.scale(width, height, filter)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating scaled bitmap", e)
                null
            }
        }
    }
}