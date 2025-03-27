@file:Suppress("UNCHECKED_CAST")

package io.core.other

import io.core.common.util.extensions.currentTimeMillis
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

/**
 * 基于内存的智能缓存控制器，具备以下特性：
 * - 使用软引用存储对象避免内存泄漏
 * - LRU访问顺序淘汰机制
 * - 支持TTL过期时间
 * - 线程安全的并发访问
 * - 自动缓存清理策略
 */
object IntentData {
    private const val MAX_ENTRIES = 100
    private const val CLEAN_PERCENTAGE = 0.25f
    private val lock = ReentrantLock()

    /**
     * 存储缓存数据的核心容器，使用：
     * - SoftReference 实现内存敏感缓存
     * - ConcurrentHashMap 保证线程安全
     */
    private val bigData = ConcurrentHashMap<String, SoftReference<Any>>()
    private val accessOrder = ConcurrentLinkedQueue<String>()
    private val expireTimes = ConcurrentHashMap<String, Long>()

    private var totalHits = 0L
    private var totalMisses = 0L

    /**
     * 添加缓存对象（带手动指定key）
     * @param key 缓存键（需保证唯一性）
     * @param data 缓存对象（可空类型）
     * @param ttl 存活时间（毫秒，0表示永久）
     */
    @JvmStatic
    @Synchronized
    @JvmOverloads
    fun <T : Any?> put(key: String, data: T, ttl: Long = 0L): String {
        val expireTime = if (ttl > 0) System.currentTimeMillis() + ttl else 0L
        bigData[key] = SoftReference(data)
        expireTimes[key] = expireTime
        recordAccess(key)
        autoClean()
        return key
    }

    /**
     * 添加缓存对象（自动生成时间戳key）
     * @return 自动生成的缓存键（基于当前时间戳）
     */
    @JvmStatic
    @Synchronized
    fun <T : Any?> put(data: T): String {
        val key = currentTimeMillis.toString()
        put(key, data)
        return key
    }

    /**
     * 获取缓存对象（线程安全带过期检查）
     * @return 当以下情况返回null：
     * - key不存在
     * - 对象已被回收
     * - TTL已过期
     * - 类型转换失败
     */
    @JvmStatic
    @Synchronized
    fun <T : Any> get(key: String?): T? {
        if (key == null) return null

        return when {
            !expireTimes.containsKey(key) -> {
                totalMisses++
                null
            }

            isExpired(key) -> {
                expireTimes.remove(key)
                bigData.remove(key)
                totalMisses++
                null
            }

            else -> {
                totalHits++
                bigData[key]?.get()?.let {
                    recordAccess(key)
                    it as? T
                }
            }
        }
    }

    /**
     * 获取缓存对象（线程安全带默认值）
     * @param defaultValue 当缓存不存在或失效时返回的默认值
     * @return 当以下情况返回defaultValue：
     * - key不存在
     * - 对象已被回收
     * - TTL已过期
     * - 类型转换失败
     * 否则返回非空缓存对象
     */
    @JvmStatic
    @Synchronized
    fun <T : Any> get(key: String?, defaultValue: T): T {
        return get(key) ?: defaultValue
    }

    /**
     * 生成缓存统计报告，包含：
     * - 缓存命中率百分比
     * - 当前缓存条目数 / 最大容量
     */
    fun cacheStats(): String =
        "Hit Rate: ${totalHits.toFloat() / (totalHits + totalMisses) * 100}% | " +
                "Entries: ${bigData.size}/$MAX_ENTRIES"

    fun contains(key: String): Boolean = expireTimes.containsKey(key) && !isExpired(key)

    @JvmStatic
    @Synchronized
    fun clear() {
        bigData.clear()
        expireTimes.clear()
        accessOrder.clear()
    }

    private fun recordAccess(key: String) {
        accessOrder.remove(key)
        accessOrder.add(key)
    }

    /**
     * 执行缓存清理策略：
     * 1. 当缓存超过最大容量时触发
     * 2. 清理25%最久未访问的条目
     * 3. 使用重入锁保证清理原子性
     */
    private fun autoClean() {
        if (bigData.size <= MAX_ENTRIES) return

        lock.withLock {
            val targetSize = (MAX_ENTRIES * (1 - CLEAN_PERCENTAGE)).toInt()
            val toRemove = accessOrder.take(min(accessOrder.size, MAX_ENTRIES - targetSize))

            toRemove.forEach { key ->
                bigData.remove(key)
                expireTimes.remove(key)
            }
            accessOrder.removeAll(toRemove.toSet())
        }
    }

    private fun isExpired(key: String): Boolean {
        val expire = expireTimes[key] ?: return true
        return expire > 0 && System.currentTimeMillis() > expire
    }
}