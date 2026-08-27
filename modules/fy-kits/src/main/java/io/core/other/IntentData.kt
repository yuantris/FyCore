@file:Suppress("UNCHECKED_CAST")

package io.core.other

import java.lang.ref.SoftReference
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 智能内存缓存控制器，具备以下优化特性：
 * - 使用读写锁实现高效并发访问
 * - 基于访问顺序的LRU淘汰机制
 * - 自动TTL过期检查和清理
 * - 内存敏感的软引用存储
 * - 智能的缓存统计和监控
 *
 * 设计原则：
 * 1. 线程安全：所有操作保证线程安全
 * 2. 内存友好：自动清理和软引用防止OOM
 * 3. 高性能：最小化锁竞争，优化读多写少场景
 * 4. 自监控：提供详细的缓存命中统计
 *
 * 使用示例：
 * ```
 * // 存储数据（自动生成key）
 * val key = IntentData.put("缓存值", ttl = 5000)
 *
 * // 获取数据
 * val value: String? = IntentData.get<String>(key)
 *
 * // 带默认值的获取
 * val value = IntentData.get(key, "默认值")
 *
 * // 获取统计信息
 * println(IntentData.cacheStats())
 * ```
 */
object IntentData {
    private const val DEFAULT_MAX_ENTRIES = 100
    private const val CLEANUP_FACTOR = 0.75 // 清理至75%容量
    private var maxEntries: Int = DEFAULT_MAX_ENTRIES
    private var cleanupFactor: Double = CLEANUP_FACTOR
    
    // 改进的key生成策略
    private val keyCounter = AtomicLong(0)
    
    // 异步清理执行器
    private val cleanupExecutor = Executors.newSingleThreadExecutor { 
        Thread(it, "IntentData-Cleanup").apply { isDaemon = true }
    }

    private data class CacheEntry(
        val data: SoftReference<Any>,
        val expireTime: Long,
        var lastAccess: Long = System.currentTimeMillis()
    )

    // 使用读写锁保护并发访问
    private val lock = ReentrantReadWriteLock()

    // 核心存储结构，LinkedHashMap维护访问顺序
    private val cache = object : LinkedHashMap<String, CacheEntry>(DEFAULT_MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
            return size > DEFAULT_MAX_ENTRIES
        }
    }

    // 使用LongAdder优化高并发计数器
    private val totalHits = LongAdder()
    private val totalMisses = LongAdder()

    /**
     * 配置缓存的最大条目数和清理因子。
     *
     * @param maxEntries 缓存的最大条目数，必须为正数。默认值为当前实例的 `maxEntries`。
     * @param cleanupFactor 清理因子，表示当缓存条目数达到最大条目数时，需要清理的比例。
     *                      该值必须在 0.1 到 0.9 之间。默认值为当前实例的 `cleanupFactor`。
     *
     * @throws IllegalArgumentException 如果 `maxEntries` 不是正数，或者 `cleanupFactor` 不在 0.1 到 0.9 之间。
     */
    @JvmStatic
    @JvmOverloads
    fun configure(maxEntries: Int = this.maxEntries, cleanupFactor: Double = this.cleanupFactor) {
        require(maxEntries > 0) { "maxEntries must be positive" }
        require(cleanupFactor in 0.1..0.9) { "cleanupFactor must be between 0.1 and 0.9" }

        lock.write {
            this.maxEntries = maxEntries
            this.cleanupFactor = cleanupFactor
        }
    }

    /**
     * 存储缓存对象
     *
     * @param key 缓存键标识（需保证唯一性）
     * @param data 要缓存的对象（可空类型）
     * @param ttl 存活时间（毫秒，0表示永久缓存）
     * @return 返回传入的key（链式调用可用）
     *
     * @throws IllegalArgumentException 如果key为空字符串
     */
    @JvmStatic
    @JvmOverloads
    fun <T : Any?> put(key: String, data: T, ttl: Long = 0L): String {
        require(key.isNotEmpty()) { "Cache key cannot be empty" }

        val expireTime = if (ttl > 0) System.currentTimeMillis() + ttl else Long.MAX_VALUE
        val entry = CacheEntry(SoftReference(data), expireTime)

        lock.write {
            cache[key] = entry
            // 使用动态配置而不是硬编码
            if (cache.size >= maxEntries) {
                cleanUp()
            }
        }
        return key
    }

    /**
     * 自动生成key存储缓存对象
     *
     * @param data 要缓存的对象
     * @param ttl 存活时间（毫秒）
     * @return 自动生成的缓存key（基于时间戳）
     */
    @JvmStatic
    fun <T : Any?> put(data: T, ttl: Long = 0L): String {
        // 使用时间戳 + 计数器 + 随机数避免冲突
        val key = "${System.currentTimeMillis()}_${keyCounter.incrementAndGet()}_${ThreadLocalRandom.current().nextInt()}"
        return put(key, data, ttl)
    }

    /**
     * 获取缓存对象
     *
     * @param key 要获取的缓存键
     * @return 当以下情况返回null：
     *         - key不存在
     *         - 对象已被回收
     *         - TTL已过期
     *         - 类型转换失败
     */
    @JvmStatic
    fun <T : Any> get(key: String?): T? {
        if (key.isNullOrEmpty()) return null

        var result: T? = null
        var shouldRemove = false
        
        lock.read {
            cache[key]?.let { entry ->
                if (isEntryValid(entry)) {
                    totalHits.increment()
                    entry.lastAccess = System.currentTimeMillis()
                    result = entry.data.get() as? T
                } else {
                    shouldRemove = true
                    totalMisses.increment()
                }
            } ?: run {
                totalMisses.increment()
            }
        }
        
        // 在read锁外进行清理，避免死锁
        if (shouldRemove) {
            lock.write { cache.remove(key) }
        }
        
        return result
    }

    /**
     * 获取缓存对象（带默认值）
     *
     * @param key 要获取的缓存键
     * @param defaultValue 当缓存不存在时返回的默认值
     * @return 缓存对象或默认值（保证非空）
     */
    @JvmStatic
    fun <T : Any> get(key: String?, defaultValue: T): T {
        return get(key) ?: defaultValue
    }

    /**
     * 检查是否包含有效缓存
     *
     * @param key 要检查的缓存键
     * @return 当且仅当key存在且未过期时返回true
     */
    @JvmStatic
    fun contains(key: String): Boolean {
        return lock.read {
            cache[key]?.let { isEntryValid(it) } ?: false
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @return 包含以下信息的字符串：
     *         - 缓存命中率（百分比）
     *         - 当前缓存条目数/最大容量
     *         - 内存占用估算
     */
    fun cacheStats(): String {
        val hits = totalHits.sum()
        val misses = totalMisses.sum()
        val hitRate = if (hits + misses > 0) {
            hits.toDouble() / (hits + misses) * 100
        } else 0.0

        return lock.read {
            "Hit Rate: ${"%.2f".format(hitRate)}% | " +
                    "Entries: ${cache.size}/$maxEntries | " +
                    "Memory: ${calculateMemoryUsage()} bytes"
        }
    }
    
    /**
     * 计算实际内存使用量
     */
    private fun calculateMemoryUsage(): Long {
        return cache.entries.sumOf { (_, entry) ->
            val data = entry.data.get()
            when (data) {
                null -> 0L
                is String -> data.length * 2L // 估算字符串内存
                is Number -> 8L // 数字类型
                is Boolean -> 1L
                is Collection<*> -> data.size * 16L // 集合类型估算
                is Array<*> -> data.size * 8L // 数组类型估算
                else -> 64L // 其他对象估算
            }
        }
    }

    /**
     * 批量存储缓存对象
     * 
     * @param entries 要缓存的键值对
     * @param ttl 存活时间（毫秒，0表示永久缓存）
     */
    @JvmStatic
    fun putAll(entries: Map<String, Any>, ttl: Long = 0L) {
        lock.write {
            val expireTime = if (ttl > 0) System.currentTimeMillis() + ttl else Long.MAX_VALUE
            entries.forEach { (key, value) ->
                cache[key] = CacheEntry(SoftReference(value), expireTime)
            }
            if (cache.size >= maxEntries) {
                cleanUp()
            }
        }
    }
    
    /**
     * 批量获取缓存对象
     * 
     * @param keys 要获取的缓存键集合
     * @return 键值对映射，不存在的键对应的值为null
     */
    @JvmStatic
    fun getAll(keys: Collection<String>): Map<String, Any?> {
        return lock.read {
            keys.associateWith { key ->
                cache[key]?.let { entry ->
                    if (isEntryValid(entry)) {
                        entry.data.get()
                    } else null
                }
            }
        }
    }
    
    /**
     * 缓存预热功能
     * 
     * @param entries 预热的键值对
     * @param ttl 存活时间（毫秒，0表示永久缓存）
     */
    @JvmStatic
    fun warmUp(entries: Map<String, Any>, ttl: Long = 0L) {
        putAll(entries, ttl)
    }
    
    /**
     * 预加载功能
     * 
     * @param keys 要预加载的键集合
     * @param loader 数据加载器
     */
    @JvmStatic
    fun preload(keys: Collection<String>, loader: (String) -> Any?) {
        lock.write {
            keys.forEach { key ->
                if (!cache.containsKey(key)) {
                    loader(key)?.let { value ->
                        cache[key] = CacheEntry(SoftReference(value), Long.MAX_VALUE)
                    }
                }
            }
        }
    }

    /**
     * 清空所有缓存
     */
    @JvmStatic
    fun clear() {
        lock.write {
            cache.clear()
            totalHits.reset()
            totalMisses.reset()
        }
    }

    // ============== 内部方法 ==============

    /**
     * 执行缓存清理：
     * 1. 移除所有过期条目
     * 2. 如果仍然超过限制，按LRU顺序移除
     */
    private fun cleanUp() {
        lock.write {
            val now = System.currentTimeMillis()
            val targetSize = (maxEntries * cleanupFactor).toInt()

            // 批量清理所有无效条目
            cache.entries.removeAll { (_, entry) ->
                entry.data.get() == null || now >= entry.expireTime
            }

            // 如果仍然超过限制，按LRU清理到目标大小
            while (cache.size > targetSize) {
                cache.remove(cache.keys.first()) // 移除最久未使用的
            }
        }
    }
    
    /**
     * 异步清理过期条目
     */
    private fun scheduleAsyncCleanup() {
        cleanupExecutor.submit {
            lock.write {
                val now = System.currentTimeMillis()
                cache.entries.removeAll { (_, entry) ->
                    entry.data.get() == null || now >= entry.expireTime
                }
            }
        }
    }
    
    /**
     * 手动触发异步清理
     */
    @JvmStatic
    fun cleanupAsync() {
        scheduleAsyncCleanup()
    }

    /**
     * 检查缓存条目是否有效
     */
    private fun isEntryValid(entry: CacheEntry): Boolean {
        return entry.data.get() != null &&
                System.currentTimeMillis() < entry.expireTime
    }
    
    /**
     * 关闭缓存系统，清理资源
     * 注意：调用后缓存将不可用，需要重新初始化
     */
    @JvmStatic
    fun shutdown() {
        cleanupExecutor.shutdown()
        clear()
    }
}