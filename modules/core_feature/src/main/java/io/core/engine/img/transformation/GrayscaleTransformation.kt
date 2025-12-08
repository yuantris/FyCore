package io.core.engine.img.transformation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/**
 * 灰度变换实现
 * 将彩色图片转换为灰度图片
 */
class GrayscaleTransformation : BitmapTransformation() {
    
    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        // 创建可修改的副本
        val config = toTransform.config ?: Bitmap.Config.ARGB_8888
        val bitmap = pool.get(toTransform.width, toTransform.height, config)
        
        // 创建画布和画笔
        val canvas = Canvas(bitmap)
        val paint = Paint()
        
        // 创建灰度颜色矩阵
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f) // 饱和度为0表示灰度
        
        // 设置颜色滤镜
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        
        // 绘制灰度图片
        canvas.drawBitmap(toTransform, 0f, 0f, paint)
        
        return bitmap
    }
    
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("grayscale".toByteArray())
    }
    
    override fun equals(other: Any?): Boolean {
        return other is GrayscaleTransformation
    }
    
    override fun hashCode(): Int {
        return "grayscale".hashCode()
    }
}