package io.core.utils.constant

import io.core.utils.constant.detector.DeviceInfo
import io.core.utils.log.LogPure
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 设备信息缓存管理�?
 * 提供线程安全的缓存机�?
 */
object DeviceInfoCache {
    
    private val cache = ConcurrentHashMap<String, DeviceInfo>()
    private val lock = ReentrantReadWriteLock()
    
    /**
     * 获取缓存的设备信�?
     */
    fun get(key: String): DeviceInfo? {
        return lock.read {
            cache[key]
        }
    }
    
    /**
     * 缓存设备信息
     */
    fun put(key: String, info: DeviceInfo) {
        lock.write {
            cache[key] = info
            LogPure.d("DeviceInfoCache") { "Cached device info for key: $key" }
        }
    }
    
    /**
     * 获取或计算设备信�?
     */
    fun getOrCompute(key: String, compute: () -> DeviceInfo): DeviceInfo {
        return get(key) ?: run {
            val info = compute()
            put(key, info)
            info
        }
    }
    
    /**
     * 清除缓存
     */
    fun clear() {
        lock.write {
            cache.clear()
            LogPure.d("DeviceInfoCache") { "Device info cache cleared" }
        }
    }
    
    /**
     * 获取缓存大小
     */
    fun size(): Int {
        return lock.read { cache.size }
    }
    
    /**
     * 检查是否包含指定键
     */
    fun containsKey(key: String): Boolean {
        return lock.read { cache.containsKey(key) }
    }
    
    /**
     * 生成缓存�?
     */
    fun generateKey(manufacturer: String, brand: String, model: String): String {
        return "${manufacturer}_${brand}_${model}".lowercase()
    }
}
