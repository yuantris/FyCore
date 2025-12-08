package io.core.engine.img.transformation

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/**
 * 高效的模糊变换实现
 * 使用RenderScript实现高质量的模糊效果
 */
class BlurTransformation(
    private val context: Context,
    private val radius: Int = 15
) : BitmapTransformation() {
    
    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        // 创建可修改的副本
        val config = toTransform.config ?: Bitmap.Config.ARGB_8888
        val bitmap = pool.get(toTransform.width, toTransform.height, config)
        
        // 使用RenderScript实现模糊
        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, toTransform)
        val output = Allocation.createFromBitmap(rs, bitmap)
        
        // 创建模糊脚本
        val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blurScript.setInput(input)
        
        // 设置模糊半径（0-25之间）
        blurScript.setRadius(radius.coerceIn(0..25).toFloat())
        
        // 执行模糊操作
        blurScript.forEach(output)
        
        // 将结果复制回Bitmap
        output.copyTo(bitmap)
        
        // 释放资源
        input.destroy()
        output.destroy()
        blurScript.destroy()
        rs.destroy()
        
        return bitmap
    }
    
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("blur_$radius".toByteArray())
    }
    
    override fun equals(other: Any?): Boolean {
        if (other is BlurTransformation) {
            return radius == other.radius
        }
        return false
    }
    
    override fun hashCode(): Int {
        return radius
    }
}