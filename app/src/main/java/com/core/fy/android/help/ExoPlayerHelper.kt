package com.core.fy.android.help

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import io.core.appCtx
import io.core.common.util.extensions.cool.GSON
import com.google.gson.reflect.TypeToken
import java.io.File


@Suppress("unused")
@SuppressLint("UnsafeOptInUsageError")
object ExoPlayerHelper {

    private const val SPLIT_TAG = "\uD83D\uDEA7"

    private val mapType by lazy {
        object : TypeToken<Map<String, String>>() {}.type
    }

    fun createMediaItem(url: String, headers: Map<String, String>): MediaItem {
        val formatUrl = url + SPLIT_TAG + GSON.toJson(headers, mapType)
        return MediaItem.Builder().setUri(formatUrl).build()
    }

    fun createHttpExoPlayer(context: Context): ExoPlayer {
        return ExoPlayer.Builder(context).build()
    }


    /**
     * Exoplayer 内置的缓存
     */
    private val cache: Cache by lazy {
        val databaseProvider = StandaloneDatabaseProvider(appCtx)
        return@lazy SimpleCache(
            //Exoplayer的缓存路径
            File(appCtx.externalCacheDir, "exoplayer"),
            //100M的缓存
            LeastRecentlyUsedCacheEvictor((100 * 1024 * 1024).toLong()),
            //记录缓存的数据库
            databaseProvider
        )
    }

    /**
     * 通过kotlin扩展函数+反射实现CacheDataSource.Factory设置默认请求头
     * 需要添加混淆规则 -keepclassmembers class com.google.android.exoplayer2.upstream.cache.CacheDataSource$Factory{upstreamDataSourceFactory;}
     * @param headers
     * @return
     */
//    private fun CacheDataSource.Factory.setDefaultRequestProperties(headers: Map<String, String> = mapOf()): CacheDataSource.Factory {
//        val declaredField = this.javaClass.getDeclaredField("upstreamDataSourceFactory")
//        declaredField.isAccessible = true
//        val df = declaredField[this] as DataSource.Factory
//        if (df is OkHttpDataSource.Factory) {
//            df.setDefaultRequestProperties(headers)
//        }
//        return this
//    }

}