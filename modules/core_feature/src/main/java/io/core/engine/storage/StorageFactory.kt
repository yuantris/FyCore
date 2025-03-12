package io.core.engine.storage

import com.tencent.mmkv.MMKV
import io.core.appCtx
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

// 统一存储管理类
object StorageFactory {
    private var engine: StorageEngine? = null
    private var currentType: StorageType = StorageType.SHARED_PREFS
    val keyDefaultMap = mutableMapOf<String, Pair<String, KClass<*>>>()

    @JvmStatic
    fun initialize(config: StorageConfig) {
        currentType = config.type
        engine = when (config.type) {
            StorageType.SHARED_PREFS -> SharedPreferencesEngine(
                appCtx.getSharedPreferences(
                    config.name,
                    config.mode
                )
            )

            StorageType.MMKV -> {
                MMKV.initialize(appCtx)
                MMKVEngine(MMKV.mmkvWithID(config.name, config.mmkvMode))
            }
        }
        config.validateClass?.let { validateKeys(it) }
    }

    fun getEngine() = engine ?: throw IllegalStateException("Storage not initialized")

    fun isInit(): Boolean {
        return engine != null
    }

    private fun validateKeys(clazz: Class<*>) {
        clazz.declaredFields.forEach { field ->
            field.getAnnotation(StorageKey::class.java)?.let { annotation ->
                require(field.type == String::class.java) {
                    "Key ${field.name} must be String type"
                }
                val key = field.get(null) as String // 获取注解字段的实际值作为key
                val fieldClass = clazz.kotlin.memberProperties
                    .first { it.name == field.name }
                    .returnType.classifier as KClass<*>

                keyDefaultMap[key] = annotation.defaultValue to fieldClass
            }
        }
    }

}