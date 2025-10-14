package io.core.constant

import android.annotation.SuppressLint
import io.core.common.util.log.LogPure
import java.util.concurrent.ConcurrentHashMap

/**
 * 系统属性缓存管理器
 * 解决频繁反射调用的性能问题
 */
object SystemPropertyCache {
    
    private val cache = ConcurrentHashMap<String, String>()
    private val reflectionCache = ConcurrentHashMap<String, Any>()
    
    /**
     * 获取系统属性（带缓存）
     */
    fun getProperty(key: String, default: String = ""): String {
        return cache.getOrPut(key) {
            getSystemPropertyInternal(key, default)
        }
    }
    
    /**
     * 检查系统属性是否存在
     */
    fun hasProperty(key: String): Boolean {
        return getProperty(key).isNotEmpty()
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        cache.clear()
        reflectionCache.clear()
    }
    
    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Int = cache.size
    
    /**
     * 内部方法：通过反射获取系统属性
     */
    @SuppressLint("PrivateApi")
    private fun getSystemPropertyInternal(key: String, default: String): String {
        return try {
            // 缓存反射方法，避免重复获取
            val method = reflectionCache.getOrPut("getMethod") {
                Class.forName("android.os.SystemProperties")
                    .getMethod("get", String::class.java)
            } as java.lang.reflect.Method
            
            method.invoke(null, key) as? String ?: default
        } catch (e: Exception) {
            LogPure.w("SystemPropertyCache") { "Failed to get system property: $key, error: ${e.message}" }
            default
        }
    }
}
