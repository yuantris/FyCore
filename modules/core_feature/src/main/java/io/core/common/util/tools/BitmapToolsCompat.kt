@file:Suppress("unused")

package io.core.common.util.tools

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import java.io.IOException
import java.io.InputStream

/**
 * BitmapTools 兼容性类
 * 保持原有API的兼容性，内部委托给 ImageProcessor
 */
@Suppress("WeakerAccess", "MemberVisibilityCanBePrivate")
object BitmapToolsCompat {

    /**
     * 从path中获取图片信息
     */
    @JvmStatic
    @Throws(IOException::class)
    fun decodeBitmap(path: String, width: Int, height: Int? = null): Bitmap? {
        return ImageProcessor.decode()
            .fromFile(path)
            .withSize(width, height)
            .build()
    }

    /**
     * 从path中获取Bitmap图片
     */
    @JvmStatic
    @Throws(IOException::class)
    fun decodeBitmap(path: String): Bitmap? {
        return ImageProcessor.decode()
            .fromFile(path)
            .build()
    }

    /**
     * 以最省内存的方式读取本地资源的图片
     */
    @JvmStatic
    fun decodeBitmapWithLowMemory(@DrawableRes resId: Int): Bitmap? {
        return ImageProcessor.decode()
            .fromResource(resId)
            .withLowMemory()
            .build()
    }

    /**
     * 以标准内存配置读取本地资源图片
     */
    @JvmStatic
    fun decodeBitmap(@DrawableRes resId: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
        return ImageProcessor.decode()
            .fromResource(resId)
            .withConfig(config)
            .build()
    }

    /**
     * 从资源解码指定尺寸的Bitmap
     */
    @JvmStatic
    fun decodeBitmap(@DrawableRes resId: Int, width: Int, height: Int): Bitmap? {
        return ImageProcessor.decode()
            .fromResource(resId)
            .withSize(width, height)
            .build()
    }

    /**
     * 从Assets解码Bitmap
     */
    @JvmStatic
    @Throws(IOException::class)
    fun decodeAssetsBitmap(fileNameInAssets: String, width: Int, height: Int): Bitmap? {
        return ImageProcessor.decode()
            .fromAssets(fileNameInAssets)
            .withSize(width, height)
            .build()
    }

    /**
     * 将Bitmap转换成InputStream
     */
    @JvmStatic
    fun toInputStream(bitmap: Bitmap): InputStream? {
        return ImageProcessor.transform()
            .bitmap(bitmap)
            .toInputStream()
    }
}