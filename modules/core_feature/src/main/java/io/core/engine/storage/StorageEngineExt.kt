package io.core.engine.storage

import io.core.common.util.extensions.cool.GSON

val storage = StorageFactory.getEngine()

// 类型推断扩展函数
inline fun <reified T : Any> StorageEngine.put(key: String, value: T?) {
    putData(key, value, T::class)
}

inline fun <reified T : Any> StorageEngine.get(key: String, default: T): T {
    return getData(key, T::class, default)
}

inline fun <reified T : Any> StorageEngine.getWithAnnotation(key: String): T {
    val (defaultValue, _) = StorageFactory.keyDefaultMap[key]
        ?: throw IllegalArgumentException("Key $key not registered with @StorageKey")

    return when (T::class) {
        String::class -> getData(key, T::class, defaultValue as T)
        Int::class -> getData(key, T::class, defaultValue.toIntOrNull() as T? ?: 0 as T)
        Boolean::class -> getData(key, T::class, defaultValue.toBooleanStrict() as T)
        Float::class -> getData(key, T::class, defaultValue.toFloatOrNull() as T? ?: 0f as T)
        Long::class -> getData(key, T::class, defaultValue.toLongOrNull() as T? ?: 0L as T)
        else -> throw IllegalArgumentException("Unsupported type")
    }
}

// 扩展存储能力
inline fun <reified T> StorageEngine.putObject(key: String, value: T) {
    val json = GSON.toJson(value)
    put(key, json)
}

inline fun <reified T> StorageEngine.getObject(key: String): T? {
    val json = get(key, "")
    return GSON.fromJson(json, T::class.java)
}