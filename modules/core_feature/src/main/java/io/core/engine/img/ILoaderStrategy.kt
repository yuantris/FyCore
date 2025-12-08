package io.core.engine.img

import android.content.Context
import android.widget.ImageView

interface ILoaderStrategy {
    /**
     * 加载图片到ImageView
     */
    fun loadImage(context: Context, options: ImageOptions)
    
    /**
     * 预加载图片
     */
    fun preloadImage(context: Context, options: ImageOptions)
    
    /**
     * 清除ImageView的图片加载请求
     */
    fun clear(imageView: ImageView)
    
    /**
     * 清除内存缓存
     */
    fun clearMemoryCache(context: Context)
    
    /**
     * 清除磁盘缓存
     */
    fun clearDiskCache(context: Context)
    
    /**
     * 暂停所有加载请求
     */
    fun pauseRequests(context: Context)
    
    /**
     * 恢复所有加载请求
     */
    fun resumeRequests(context: Context)
}