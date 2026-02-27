package com.core.fy.android.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

sealed class PathSegment {
    data class Key(val value: String) : PathSegment()
    data class Index(val value: Int) : PathSegment()
}

class SafeJson private constructor(private val root: Any) {
    private val lock = ReentrantReadWriteLock()

    companion object {
        @JvmStatic
        fun parse(jsonString: String): SafeJson {
            return try {
                val trimmed = jsonString.trim()
                when {
                    trimmed.startsWith("{") -> SafeJson(JSONObject(trimmed))
                    trimmed.startsWith("[") -> SafeJson(JSONArray(trimmed))
                    else -> throw JSONException("Invalid JSON format")
                }
            } catch (e: JSONException) {
                throw JsonParseException("Failed to parse JSON", e)
            }
        }

        private fun parsePath(path: String): List<PathSegment> {
            val regex = """(?<!\\)\.""".toRegex()
            return path.split(regex).map { segment ->
                val unescaped = segment.replace("\\\\.", ".")
                    .replace("\\\\", "\\")
                
                when {
                    unescaped.matches("""\[\d+]""".toRegex()) -> {
                        val index = unescaped.substring(1, unescaped.length - 1).toInt()
                        PathSegment.Index(index)
                    }
                    else -> PathSegment.Key(unescaped)
                }
            }
        }
    }

    fun getString(path: String): String? = lock.read {
        navigate(path)?.let { value ->
            when (value) {
                is String -> value
                is Number -> value.toString()
                is Boolean -> value.toString()
                is JSONObject -> value.toString()
                is JSONArray -> value.toString()
                else -> null
            }
        }
    }

    fun getInt(path: String): Int? = lock.read {
        getNumber(path)?.toInt()
    }

    fun getLong(path: String): Long? = lock.read {
        getNumber(path)?.toLong()
    }

    fun getBigInteger(path: String): BigInteger? = lock.read {
        getString(path)?.let { BigInteger(it) }
    }

    fun getBigDecimal(path: String): BigDecimal? = lock.read {
        getString(path)?.let { BigDecimal(it) }
    }

    fun getObject(path: String): SafeJson? = lock.read {
        navigate(path)?.let { value ->
            when (value) {
                is JSONObject -> SafeJson(value)
                is JSONArray -> SafeJson(value)
                is String -> try { // 新增字符串解析逻辑
                    when {
                        value.trimStart().startsWith("{") -> SafeJson(JSONObject(value))
                        value.trimStart().startsWith("[") -> SafeJson(JSONArray(value))
                        else -> null
                    }
                } catch (e: Exception) {
                    null
                }
                else -> null
            }
        }
    }


    fun getJsonArray(path: String): List<SafeJson>? = lock.read {
        (navigate(path) as? JSONArray)?.let { array ->
            List(array.length()) { index ->
                SafeJson(array.get(index))
            }
        }
    }

    fun containsKey(path: String): Boolean = lock.read {
        navigate(path) != null
    }


    private fun getNumber(path: String): Number? {
        return navigate(path)?.let { value ->
            when (value) {
                is Number -> value
                is String -> try {
                    value.toDouble()
                } catch (e: NumberFormatException) {
                    null
                }
                else -> null
            }
        }
    }

    private fun navigate(path: String): Any? {
        val segments = parsePath(path)
        var current: Any? = root

        for (segment in segments) {
            current = when (current) {
                is JSONObject -> handleObjectSegment(current, segment)
                is JSONArray -> handleArraySegment(current, segment)
                else -> return null
            } ?: return null
        }
        return current
    }

    private fun handleObjectSegment(obj: JSONObject, segment: PathSegment): Any? {
        return when (segment) {
            is PathSegment.Key -> try {
                obj.get(segment.value)
            } catch (e: JSONException) {
                null
            }
            is PathSegment.Index -> null
        }
    }

    private fun handleArraySegment(arr: JSONArray, segment: PathSegment): Any? {
        return when (segment) {
            is PathSegment.Index -> try {
                arr.get(segment.value)
            } catch (e: JSONException) {
                null
            }
            is PathSegment.Key -> null
        }
    }
}

// Custom Exceptions
class JsonParseException(message: String, cause: Throwable?) : Exception(message, cause)
class JsonPathException(message: String) : Exception(message)

/**
 * val json = """
 * {
 *     "user": {
 *         "name": "John",
 *         "age": 30,
 *         "contacts": [
 *             {"type": "email", "value": "john@example.com"},
 *             {"type": "phone", "value": "+123456789"}
 *         ],
 *         "balance": "12345678901234567890.12345"
 *     }
 * }
 * """.trimIndent()
 *
 * val safeJson = SafeJson.parse(json)
 *
 * // 获取简单�?
 * val name = safeJson.getString("user.name") // "John"
 * val age = safeJson.getInt("user.age") // 30
 *
 * // 处理大数�?
 * val balance = safeJson.getBigDecimal("user.balance") // BigDecimal("12345678901234567890.12345")
 *
 * // 处理数组
 * val firstContact = safeJson.getJsonArray("user.contacts")?.first()
 * val contactType = firstContact?.getString("type") // "email"
 *
 * // 处理特殊字符路径
 * val specialKeyJson = SafeJson.parse("""{"a.b": {"c": "value"}}""")
 * val value = specialKeyJson.getString("a\\.b.c") // "value"
 */

/**
 *     val json = """
 * {
 *     "user": {
 *         "name": "John",
 *         "age": 30,
 *         "hobbies": ["Reading", {"name": "Gaming", "type": "Video"}],
 *         "active": true
 *     }
 * }
 * """.trimIndent()
 *  val safeJson = SafeJson.parse(json).getString("user.hobbies.[0]")
 *  safeJson?.let {
 *      ADUtil.logd("zyAdCore_", it)
 *  }
 */
