package io.core.engine.img

import android.content.Context
import android.widget.ImageView

object ImageLoader {
    // 默认策略，可以在 Application 中初始化时替换
    private var strategy: ILoaderStrategy = GlideStrategy() 

    // 提供给 Application 初始化时替换策略（例如换成 Coil）
    fun setStrategy(newStrategy: ILoaderStrategy) {
        strategy = newStrategy
    }

    // 核心加载方法
    fun load(options: ImageOptions) {
        val view = options.imageView ?: return
        strategy.loadImage(view.context, options)
    }
    
    /**
     * 支持扩展函数配置的加载方法
     */
    fun load(imageView: ImageView, source: Any?, config: ImageOptions.() -> Unit = {}) {
        val options = ImageOptions().apply {
            url = source
            this.imageView = imageView
            config()
        }
        load(options)
    }
    
    // 预加载图片
    fun preload(options: ImageOptions) {
        strategy.preloadImage(options.imageView?.context ?: return, options)
    }
    
    // 预加载图片，指定Context
    fun preload(context: Context, options: ImageOptions) {
        strategy.preloadImage(context, options)
    }
    
    // 清除ImageView的图片加载请求
    fun clear(imageView: ImageView) {
        strategy.clear(imageView)
    }
    
    // 清除内存缓存
    fun clearMemoryCache(context: Context) {
        strategy.clearMemoryCache(context)
    }
    
    // 清除磁盘缓存
    fun clearDiskCache(context: Context) {
        strategy.clearDiskCache(context)
    }
    
    // 暂停所有加载请求
    fun pauseRequests(context: Context) {
        strategy.pauseRequests(context)
    }
    
    // 恢复所有加载请求
    fun resumeRequests(context: Context) {
        strategy.resumeRequests(context)
    }

    // --- 兼容 Java 的静态入口 ---
    @JvmStatic
    fun with(context: Context): JavaBuilder {
        return JavaBuilder(context)
    }

    // Java 使用的链式调用辅助类
    class JavaBuilder(private val context: Context) {
        private val builder = ImageOptions.Builder()

        fun load(url: Any?) = apply { builder.url(url) }
        fun placeholder(resId: Int) = apply { builder.placeholder(resId) }
        fun error(resId: Int) = apply { builder.error(resId) }
        fun circle() = apply { builder.asCircle() }
        fun corner(radiusDp: Int) = apply { builder.corner(radiusDp) }
        fun blur(radius: Int) = apply { builder.blur(radius) }
        fun grayscale() = apply { builder.grayscale() }
        fun skipMemoryCache() = apply { builder.skipMemoryCache() }
        fun skipDiskCache() = apply { builder.skipDiskCache() }
        fun crossFade() = apply { builder.crossFade() }
        fun crossFade(duration: Int) = apply { builder.crossFade(duration) }
        fun thumbnail(scale: Float) = apply { builder.thumbnail(scale) }
        
        fun into(imageView: ImageView) {
            builder.into(imageView)
            ImageLoader.load(builder.build())
        }
        
        fun preload() {
            ImageLoader.preload(context, builder.build())
        }
    }
}