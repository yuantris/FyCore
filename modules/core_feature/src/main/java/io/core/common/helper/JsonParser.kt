package io.core.common.helper

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.let
import kotlin.ranges.until
import kotlin.text.startsWith
import kotlin.text.trim

/**
 * 轻量级JSON解析器工具类
 * 提供基础的JSON字符串解析和数据获取功能
 */
class JsonParser(private val jsonString: String) {

    var jsonObject: JSONObject? = null
    private var jsonArray: JSONArray? = null
    private var isArray: Boolean = false

    init {
        parseJson()
    }

    /**
     * 解析JSON字符串
     */
    private fun parseJson() {
        try {
            val trimmed = jsonString.trim()
            when {
                trimmed.startsWith("{") -> {
                    jsonObject = JSONObject(jsonString)
                    isArray = false
                }

                trimmed.startsWith("[") -> {
                    jsonArray = JSONArray(jsonString)
                    isArray = true
                }

                else -> {
                    throw JSONException("Invalid JSON format")
                }
            }
        } catch (e: JSONException) {
            throw kotlin.IllegalArgumentException("Invalid JSON string: ${e.message}")
        }
    }

    /**
     * 泛型方法：根据key获取指定类型的value
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> get(key: String): T? {
        return try {
            when (T::class) {
                String::class -> jsonObject?.getString(key) as? T
                Int::class -> jsonObject?.getInt(key) as? T
                Long::class -> jsonObject?.getLong(key) as? T
                Double::class -> jsonObject?.getDouble(key) as? T
                Boolean::class -> jsonObject?.getBoolean(key) as? T
                JsonParser::class -> {
                    val obj = jsonObject?.getJSONObject(key)
                    obj?.let { JsonParser(it.toString()) } as? T
                }

                else -> null
            }
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * 泛型方法：根据key获取指定类型的value，如果不存在返回默认值
     */
    inline fun <reified T> get(key: String, defaultValue: T): T {
        return get<T>(key) ?: defaultValue
    }

    /**
     * 根据key获取JSONObject类型的value
     */
    fun getJsonObject(key: String): JsonParser? {
        return try {
            val obj = jsonObject?.getJSONObject(key)
            obj?.let { JsonParser(it.toString()) }
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * 根据key获取JSONArray类型的value
     */
    fun getJsonArray(key: String): List<JsonParser>? {
        return try {
            val array = jsonObject?.getJSONArray(key)
            array?.let {
                (0 until it.length()).map { index ->
                    JsonParser(it.get(index).toString())
                }
            }
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * 泛型方法：根据key获取指定类型的数组
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getArray(key: String): List<T>? {
        return try {
            val array = jsonObject?.getJSONArray(key)
            array?.let {
                (0 until it.length()).map { index ->
                    when (T::class) {
                        String::class -> it.getString(index) as T
                        Int::class -> it.getInt(index) as T
                        Long::class -> it.getLong(index) as T
                        Double::class -> it.getDouble(index) as T
                        Boolean::class -> it.getBoolean(index) as T
                        JsonParser::class -> JsonParser(it.get(index).toString()) as T
                        else -> throw kotlin.IllegalArgumentException("Unsupported type: ${T::class}")
                    }
                }
            }
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * 泛型方法：根据key获取指定类型的数组，如果不存在返回默认值
     */
    inline fun <reified T> getArray(key: String, defaultValue: List<T>): List<T> {
        return getArray<T>(key) ?: defaultValue
    }


    /**
     * 如果当前JSON是数组，根据索引获取元素
     */
    fun getArrayItem(index: Int): JsonParser? {
        return try {
            if (isArray && jsonArray != null && index >= 0 && index < jsonArray!!.length()) {
                JsonParser(jsonArray!!.get(index).toString())
            } else {
                null
            }
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * 获取数组长度（仅当JSON为数组时有效）
     */
    fun getArrayLength(): Int {
        return if (isArray) jsonArray?.length() ?: 0 else 0
    }

    /**
     * 检查是否包含指定的key
     */
    fun has(key: String): Boolean {
        return jsonObject?.has(key) ?: false
    }

    /**
     * 获取所有的key
     */
    fun getKeys(): Set<String> {
        return try {
            val keys = mutableSetOf<String>()
            jsonObject?.keys()?.forEach { keys.add(it) }
            keys
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * 判断当前JSON是否为数组
     */
    fun isJsonArray(): Boolean = isArray

    /**
     * 判断当前JSON是否为对象
     */
    fun isJsonObject(): Boolean = !isArray

    /**
     * 获取原始JSON字符串
     */
    fun getRawJson(): String = jsonString

    /**
     * 格式化输出JSON字符串
     */
    fun toPrettyString(indent: Int = 2): String {
        return try {
            when {
                isArray -> jsonArray?.toString(indent) ?: jsonString
                else -> jsonObject?.toString(indent) ?: jsonString
            }
        } catch (e: JSONException) {
            jsonString
        }
    }

    companion object {
        /**
         * 静态方法：快速解析JSON字符串
         */
        @JvmStatic
        fun parse(jsonString: String): JsonParser {
            return JsonParser(jsonString)
        }

        @JvmStatic
        fun parseOrNull(jsonString: String): JsonParser? {
            return try {
                JsonParser(jsonString)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * 静态方法：验证JSON字符串是否有效
         */
        @JvmStatic
        fun isValidJson(jsonString: String): Boolean {
            return try {
                val trimmed = jsonString.trim()
                when {
                    trimmed.startsWith("{") -> {
                        JSONObject(jsonString)
                        true
                    }

                    trimmed.startsWith("[") -> {
                        JSONArray(jsonString)
                        true
                    }

                    else -> false
                }
            } catch (e: JSONException) {
                false
            }
        }
    }
}