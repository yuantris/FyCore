package io.core.engine.img

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import io.core.engine.img.transformation.BlurTransformation
import io.core.engine.img.transformation.GrayscaleTransformation

class GlideStrategy : ILoaderStrategy {
    override fun loadImage(context: Context, options: ImageOptions) {
        val requestManager = Glide.with(context)
        val requestBuilder = requestManager.load(options.url)

        // 构建请求选项
        val requestOptions = buildRequestOptions(context, options)
        
        // 应用缩略图
        if (options.thumbnail > 0f) {
            requestBuilder.thumbnail(options.thumbnail)
        }
        
        options.imageView?.let { imageView ->
            val target = requestBuilder.apply(requestOptions)
            
            // 添加加载监听
            applyLoadListeners(target, options)
            
            // 开始加载
            options.listener?.onStart()
            target.into(imageView)
        }
    }
    
    /**
     * 构建请求选项
     */
    private fun buildRequestOptions(context: Context, options: ImageOptions): RequestOptions {
        val requestOptions = RequestOptions()

        // 占位图和错误图
        applyPlaceholderAndError(options, requestOptions)
        
        // 缓存控制
        applyCacheControl(options, requestOptions)
        
        // 优先级
        applyPriority(options, requestOptions)
        
        // 尺寸调整
        applyResize(options, requestOptions)
        
        // 变换处理
        applyTransformations(context, options, requestOptions)
        
        return requestOptions
    }
    
    /**
     * 应用占位图和错误图
     */
    private fun applyPlaceholderAndError(options: ImageOptions, requestOptions: RequestOptions) {
        if (options.placeholderResId != 0) {
            requestOptions.placeholder(options.placeholderResId)
        } else if (options.placeholderDrawable != null) {
            requestOptions.placeholder(options.placeholderDrawable)
        }
        if (options.errorResId != 0) {
            requestOptions.error(options.errorResId)
        } else if (options.errorDrawable != null) {
            requestOptions.error(options.errorDrawable)
        }
    }
    
    /**
     * 应用缓存控制
     */
    private fun applyCacheControl(options: ImageOptions, requestOptions: RequestOptions) {
        requestOptions.skipMemoryCache(options.skipMemoryCache)
        requestOptions.diskCacheStrategy(if (options.skipDiskCache) DiskCacheStrategy.NONE else DiskCacheStrategy.AUTOMATIC)
    }
    
    /**
     * 应用优先级
     */
    private fun applyPriority(options: ImageOptions, requestOptions: RequestOptions) {
        when (options.priority) {
            ImageOptions.LoadPriority.LOW -> requestOptions.priority(com.bumptech.glide.Priority.LOW)
            ImageOptions.LoadPriority.HIGH -> requestOptions.priority(com.bumptech.glide.Priority.HIGH)
            else -> requestOptions.priority(com.bumptech.glide.Priority.NORMAL)
        }
    }
    
    /**
     * 应用尺寸调整
     */
    private fun applyResize(options: ImageOptions, requestOptions: RequestOptions) {
        if (options.resizeWidth > 0 && options.resizeHeight > 0) {
            requestOptions.override(options.resizeWidth, options.resizeHeight)
        }
    }
    
    /**
     * 应用变换效果
     */
    private fun applyTransformations(context: Context, options: ImageOptions, requestOptions: RequestOptions) {
        val transformations = mutableListOf<BitmapTransformation>()
        
        // 圆形裁剪优先于scaleType
        if (options.isCircle) {
            transformations.add(CircleCrop())
        } else {
            // 根据scaleType添加对应的变换
            when (options.scaleType) {
                ImageOptions.ScaleType.CENTER_CROP -> transformations.add(CenterCrop())
                ImageOptions.ScaleType.FIT_CENTER -> transformations.add(FitCenter())
                ImageOptions.ScaleType.CENTER -> transformations.add(CenterCrop())
                ImageOptions.ScaleType.CENTER_INSIDE -> transformations.add(CenterCrop())
                ImageOptions.ScaleType.FIT_START -> transformations.add(FitCenter())
                ImageOptions.ScaleType.FIT_END -> transformations.add(FitCenter())
                ImageOptions.ScaleType.FIT_XY -> transformations.add(FitCenter())
            }
            
            // 圆角裁剪
            if (options.cornerRadius > 0) {
                transformations.add(RoundedCorners(dip2px(context, options.cornerRadius.toFloat())))
            }
        }
        
        // 灰度效果
        if (options.isGrayscale) {
            transformations.add(GrayscaleTransformation())
        }
        
        // 模糊效果
        if (options.isBlur) {
            transformations.add(BlurTransformation(context, options.blurRadius))
        }
        
        // 应用所有变换
        if (transformations.isNotEmpty()) {
            requestOptions.transform(*transformations.toTypedArray())
        }
    }
    
    /**
     * 应用加载监听
     */
    private fun applyLoadListeners(
        requestBuilder: com.bumptech.glide.RequestBuilder<Drawable>,
        options: ImageOptions
    ) {
        options.listener?.let { listener ->
            requestBuilder.listener(object : com.bumptech.glide.request.RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    listener.onError(e)
                    listener.onComplete()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    listener.onSuccess(resource)
                    listener.onComplete()
                    return false
                }
            })
        }
    }

    override fun preloadImage(context: Context, options: ImageOptions) {
        val requestManager = Glide.with(context)
        val requestBuilder = requestManager.load(options.url)
        
        val requestOptions = RequestOptions()
        // 应用基本配置
        if (options.placeholderResId != 0) {
            requestOptions.placeholder(options.placeholderResId)
        }
        if (options.errorResId != 0) {
            requestOptions.error(options.errorResId)
        }
        
        requestBuilder.apply(requestOptions).preload()
    }

    override fun clear(imageView: ImageView) {
        Glide.with(imageView.context).clear(imageView)
    }

    override fun clearMemoryCache(context: Context) {
        Glide.get(context).clearMemory()
    }

    override fun clearDiskCache(context: Context) {
        Glide.get(context).clearDiskCache()
    }

    override fun pauseRequests(context: Context) {
        Glide.with(context).pauseRequests()
    }

    override fun resumeRequests(context: Context) {
        Glide.with(context).resumeRequests()
    }

    private fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
}