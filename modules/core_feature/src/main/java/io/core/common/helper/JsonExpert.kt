@file:Suppress("UNCHECKED_CAST")

package io.core.common.helper

import kotlinx.serialization.json.*
import kotlinx.serialization.json.JsonObject

class JsonExpert private constructor(private val jsonElement: JsonElement) {
    // region 核心解析功能
    operator fun get(path: String): JsonExpert? {
        val segments = parsePath(path)
        var current: JsonElement = jsonElement

        for (segment in segments) {
            current = current.autoParseStringContent() // 每次处理前都尝试解析
            current = when {
                segment.isArrayIndex() -> handleArray(current, segment)
                else -> handleObject(current, segment)
            } ?: return null
        }
        return JsonExpert(current)
    }

    private fun JsonElement.autoParseStringContent(): JsonElement {
        return when (this) {
            is JsonPrimitive -> if (isString) {
                try {
                    Json.parseToJsonElement(content).autoParseStringContent() // 递归解析
                } catch (e: Exception) {
                    this
                }
            } else {
                this
            }
            is JsonObject -> buildJsonObject {
                this@autoParseStringContent.forEach { (key, value) ->
                    put(key, value.autoParseStringContent()) // 递归处理对象字段
                }
            }
            is JsonArray -> buildJsonArray {
                this@autoParseStringContent.forEach {
                    add(it.autoParseStringContent()) // 递归处理数组元素
                }
            }
            else -> this
        }
    }

    fun asString() = jsonElement.jsonPrimitive.content
    fun asInt() = jsonElement.jsonPrimitive.int
    fun asBoolean() = jsonElement.jsonPrimitive.boolean
    fun asDouble() = jsonElement.jsonPrimitive.double
    fun <T> asList(converter: (JsonExpert) -> T) = jsonElement.jsonArray.map { JsonExpert(it).let(converter) }
    // endregion

    // region 构建功能
    companion object Builder {
        fun build(block: JsonObjectBuilder.() -> Unit): String {
            return JsonObjectBuilder().apply(block).toJsonString()
        }

        fun parse(jsonString: String): JsonExpert {
            return JsonExpert(Json.parseToJsonElement(jsonString))
        }
    }

    class JsonObjectBuilder {
        private val content = mutableMapOf<String, JsonElement>()

        infix fun String.of(value: Any?) {
            content[this] = when (value) {
                null -> JsonNull
                is Boolean -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is JsonObjectBuilder -> value.build()
                is Iterable<*> -> JsonArray(value.map { it.toJsonElement() })
                is Map<*, *> -> JsonArrayBuilder().convertMapToJsonObject(value) // 添加 Map 处理
                else -> convertValue(value)
            }
        }

        infix fun String.obj(block: JsonObjectBuilder.() -> Unit) {
            content[this] = JsonObjectBuilder().apply(block).build()
        }

        infix fun String.array(block: JsonArrayBuilder.() -> Unit) {
            content[this] = JsonArrayBuilder().apply(block).build()
        }

        fun build() = JsonObject(content)
        fun toJsonString() = build().toString()

        private fun Any?.toJsonElement(): JsonElement {
            return when (this) {
                null -> JsonNull
                is Boolean -> JsonPrimitive(this)
                is Number -> JsonPrimitive(this)
                is String -> JsonPrimitive(this)
                is JsonElement -> this
                else -> Json.parseToJsonElement(toString())
            }
        }

        private fun convertValue(value: Any?): JsonElement {
            return when (value) {
                is Boolean -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is Iterable<*> -> JsonArray(value.map { convertValue(it) })
                else -> Json.parseToJsonElement(value.toString())
            }
        }
    }

    class JsonArrayBuilder {
        private val content = mutableListOf<JsonElement>()

        operator fun plus(value: Any?) {
            content += when (value) {
                null -> JsonNull
                is Boolean -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is Map<*, *> -> convertMapToJsonObject(value)
                is JsonObjectBuilder -> value.build()
                is JsonArrayBuilder -> value.build()
                else -> Json.parseToJsonElement(value.toString())
            }
        }

        fun convertMapToJsonObject(map: Map<*, *>): JsonObject {
            return JsonObjectBuilder().apply {
                map.forEach { (k, v) ->
                    // 使用自定义的 to1 操作符
                    k?.toString()?.let { key ->
                        key of v
                    }
                }
            }.build()
        }

        fun build() = JsonArray(content)
    }
    // endregion

    // region 私有工具方法
    private fun parsePath(path: String): List<String> {
        return path.split('.')
            .flatMap { it.split(Regex("""(?<=\])|(?=\[)""")) }
            .filter { it.isNotEmpty() }
            .map { it.replace(Regex("""^['"]|['"]$"""), "") } // 支持带引号的key
    }


    private fun String.isArrayIndex(): Boolean {
        return matches(Regex("""\w+\[\d+]"""))
    }

    private fun handleArray(element: JsonElement, segment: String): JsonElement? {
        val (key, index) = Regex("""(\w+)\[(\d+)]""").find(segment)?.destructured ?: return null
        return (element as? JsonObject)?.get(key)?.jsonArray?.getOrNull(index.toInt())
    }

    private fun handleObject(element: JsonElement, segment: String): JsonElement? {
        return (element as? JsonObject)?.get(segment)
    }

    // endregion
}