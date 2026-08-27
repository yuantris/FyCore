@file:Suppress("unused")

package io.core.common.util.tools

import android.graphics.Bitmap
import java.io.File

/**
 * ImageTools 兼容性类
 * 保持原有API的兼容性，内部委托给 ImageProcessor
 */
object ImageToolsCompat {

    /**
     * 保存Bitmap到文件
     */
    @JvmStatic
    @JvmOverloads
    fun save(
        src: Bitmap?,
        file: File,
        format: Bitmap.CompressFormat,
        quality: Int = 100,
        recycle: Boolean = false
    ): Boolean {
        return ImageProcessor.save()
            .bitmap(src)
            .toFile(file)
            .withFormat(format)
            .withQuality(quality)
            .withRecycle(recycle)
            .execute() as? Boolean == true
    }

    /**
     * 保存Bitmap到相册
     */
    @JvmStatic
    @JvmOverloads
    fun save2Album(
        src: Bitmap?,
        format: Bitmap.CompressFormat,
        dirName: String? = null,
        quality: Int = 100,
        recycle: Boolean = false
    ): File? {
        return ImageProcessor.save()
            .bitmap(src)
            .toAlbum(dirName)
            .withFormat(format)
            .withQuality(quality)
            .withRecycle(recycle)
            .execute() as? File
    }
}