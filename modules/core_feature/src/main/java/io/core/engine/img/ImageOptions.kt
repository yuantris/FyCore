package io.core.engine.img

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange

/**
 * 图片加载监听器
 */
interface ImageLoaderListener {
    /**
     * 加载成功回调
     */
    fun onSuccess(resource: Any?) {}
    
    /**
     * 加载失败回调
     */
    fun onError(throwable: Throwable?) {}
    
    /**
     * 加载开始回调
     */
    fun onStart() {}
    
    /**
     * 加载完成回调（无论成功失败）
     */
    fun onComplete() {}
}

/**
 * 统一的图片配置类
 * 包含所有通用的加载参数
 */
/**
 * 全局默认配置
 */
object ImageOptionsDefaults {
    var placeholderResId: Int = 0
    var errorResId: Int = 0
    var isCrossFade: Boolean = true
    var crossFadeDuration: Int = 300
    var priority: ImageOptions.LoadPriority = ImageOptions.LoadPriority.NORMAL
    var scaleType: ImageOptions.ScaleType = ImageOptions.ScaleType.CENTER_CROP
}


/**
 * 自定义请求配置接口
 * 允许直接访问底层图片库的请求构建器
 */
interface CustomRequestConfig<T> {
    /**
     * 配置底层请求构建器
     * @param requestBuilder 底层图片库的请求构建器实例
     */
    fun configure(requestBuilder: T)
}

/**
 * 统一的图片配置类
 * 包含所有通用的加载参数
 */
data class ImageOptions(
    var url: Any? = null,
    var imageView: ImageView? = null,
    @param:DrawableRes var placeholderResId: Int = ImageOptionsDefaults.placeholderResId,
    @param:DrawableRes var errorResId: Int = ImageOptionsDefaults.errorResId,
    var placeholderDrawable: Drawable? = null,
    var errorDrawable: Drawable? = null,
    var isCircle: Boolean = false,
    var cornerRadius: Int = 0, // dp
    var blurRadius: Int = 15, // 模糊半径，默认15
    var isGrayscale: Boolean = false, // 是否灰度
    var isBlur: Boolean = false, // 是否模糊
    var skipMemoryCache: Boolean = false,
    var skipDiskCache: Boolean = false,
    var isCrossFade: Boolean = ImageOptionsDefaults.isCrossFade,
    var crossFadeDuration: Int = ImageOptionsDefaults.crossFadeDuration, // 交叉淡入时长，单位ms
    var thumbnail: Float = 0f, // 缩略图比例，0-1之间
    var priority: LoadPriority = ImageOptionsDefaults.priority,
    var resizeWidth: Int = 0,
    var resizeHeight: Int = 0,
    var scaleType: ScaleType = ImageOptionsDefaults.scaleType,
    var listener: ImageLoaderListener? = null,
    var customConfig: CustomRequestConfig<*>? = null, // 自定义请求配置
) {
    /**
     * 图片缩放类型
     */
    enum class ScaleType {
        CENTER,
        CENTER_CROP,
        CENTER_INSIDE,
        FIT_CENTER,
        FIT_START,
        FIT_END,
        FIT_XY
    }


    /**
     * 加载优先级
     */
    enum class LoadPriority {
        LOW,
        NORMAL,
        HIGH
    }
    
    // 专门为 Java 提供的 Builder，虽然 Kotlin 有命名参数，但 Builder 对 Java 更习惯
    class Builder {
        private val options = ImageOptions()

        fun url(url: Any?) = apply { options.url = url }
        fun into(imageView: ImageView) = apply { options.imageView = imageView }
        fun placeholder(@DrawableRes resId: Int) = apply { options.placeholderResId = resId }
        fun error(@DrawableRes resId: Int) = apply { options.errorResId = resId }
        fun placeholder(drawable: Drawable) = apply { options.placeholderDrawable = drawable }
        fun error(drawable: Drawable) = apply { options.errorDrawable = drawable }
        fun asCircle() = apply { options.isCircle = true }
        fun corner(radiusDp: Int) = apply { options.cornerRadius = radiusDp }
        fun blur(radius: Int) = apply { options.isBlur = true; options.blurRadius = radius }
        fun grayscale() = apply { options.isGrayscale = true }
        fun skipMemoryCache() = apply { options.skipMemoryCache = true }
        fun skipDiskCache() = apply { options.skipDiskCache = true }
        fun crossFade(duration: Int = 300) = apply { options.isCrossFade = true; options.crossFadeDuration = duration }
        fun thumbnail(@FloatRange(from = 0.0, to = 1.0) scale: Float) = apply { options.thumbnail = scale }
        fun priority(priority: LoadPriority) = apply { options.priority = priority }
        fun resize(width: Int, height: Int) = apply { options.resizeWidth = width; options.resizeHeight = height }
        fun scaleType(scaleType: ScaleType) = apply { options.scaleType = scaleType }
        fun listener(listener: ImageLoaderListener?) = apply { options.listener = listener }
        /**
         * 设置自定义请求配置
         * 允许直接访问底层图片库的请求构建器
         */
        fun <T> customConfig(config: CustomRequestConfig<T>) = apply { options.customConfig = config as CustomRequestConfig<*> }
        
        fun build(): ImageOptions = options
    }
}