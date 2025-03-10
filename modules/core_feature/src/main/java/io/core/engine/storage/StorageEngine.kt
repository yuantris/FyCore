package io.core.engine.storage

import kotlin.reflect.KClass

// 存储引擎接口（使用泛型）
interface StorageEngine {
    // 通用存取方法
    fun <T : Any> putData(key: String, value: T?, type: KClass<out T>)
    fun <T : Any> getData(key: String, type: KClass<T>, default: T): T
    
    // 其他操作
    fun remove(key: String)
    fun clear()
    fun contains(key: String): Boolean
    fun getAllKeys(): Set<String>
}