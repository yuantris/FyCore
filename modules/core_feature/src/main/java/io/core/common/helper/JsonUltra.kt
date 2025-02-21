package io.core.common.helper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.json.JsonObject

class JsonUltra private constructor(
    private val jsonElement: JsonElement,
    private val autoParse: Boolean = true
) {
    // region 核心解析功能
    operator fun get(path: String): JsonUltra? {
        val segments = parsePath(path)
        var current: JsonElement = jsonElement

        for (segment in segments) {
            current = current.autoParseStringContent()
            val parsedSegment = parseSegment(segment)
            current = when {
                parsedSegment.isArray -> handleArray(current, parsedSegment)
                else -> handleObject(current, parsedSegment)
            } ?: return null
        }
        return JsonUltra(current, autoParse)
    }

    private fun JsonElement.autoParseStringContent(): JsonElement {
        if (!autoParse) return this
        return when (this) {
            is JsonPrimitive -> if (isString) {
                try {
                    Json.parseToJsonElement(content).autoParseStringContent()
                } catch (e: Exception) {
                    this
                }
            } else {
                this
            }

            is JsonObject -> buildJsonObject {
                this@autoParseStringContent.forEach { (key, value) ->
                    put(key, value.autoParseStringContent())
                }
            }

            is JsonArray -> buildJsonArray {
                this@autoParseStringContent.forEach {
                    add(it.autoParseStringContent())
                }
            }

            else -> this
        }
    }

    fun getJsonString() = Json.encodeToString(jsonElement)
    // 安全类型转换方法
    fun asString() = jsonElement.jsonPrimitive.content
    fun asStringOrNull() = jsonElement.jsonPrimitive.contentOrNull
    fun asInt() = jsonElement.jsonPrimitive.int
    fun asIntOrNull() = runCatching { jsonElement.jsonPrimitive.int }.getOrNull()
    fun asBoolean() = jsonElement.jsonPrimitive.boolean
    fun asBooleanOrNull() = runCatching { jsonElement.jsonPrimitive.boolean }.getOrNull()
    fun asDouble() = jsonElement.jsonPrimitive.double
    fun asDoubleOrNull() = runCatching { jsonElement.jsonPrimitive.double }.getOrNull()
    fun <T> asList(converter: (JsonUltra) -> T) =
        jsonElement.jsonArray.map { JsonUltra(it).let(converter) }
    // endregion

    // region 构建功能
    companion object Builder {
        @JvmStatic
        fun build(block: JsonObjectBuilder.() -> Unit): String {
            return JsonObjectBuilder().apply(block).toJsonString()
        }

        @JvmStatic
        fun parse(jsonString: String, autoParse: Boolean = true): JsonUltra {
            return JsonUltra(Json.parseToJsonElement(jsonString), autoParse)
        }
    }

    class JsonObjectBuilder {
        private val content = mutableMapOf<String, JsonElement>()

        infix fun String.with(value: Any?) {
            content[this] = value.toJsonElement()
        }

        infix fun String.obj(block: JsonObjectBuilder.() -> Unit) {
            content[this] = JsonObjectBuilder().apply(block).build()
        }

        infix fun String.array(block: JsonArrayBuilder.() -> Unit) {
            content[this] = JsonArrayBuilder().apply(block).build()
        }

        fun build() = JsonObject(content)
        fun toJsonString() = build().toString()

        private fun Any?.toJsonElement(): JsonElement = when (this) {
            null -> JsonNull
            is Boolean -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is String -> JsonPrimitive(this)
            is JsonElement -> this
            is Map<*, *> -> JsonObjectBuilder().apply {
                this@toJsonElement.forEach { (k, v) ->
                    k?.toString()?.let { key -> key with v }
                }
            }.build()

            is Iterable<*> -> JsonArray(map { it.toJsonElement() })
            else -> Json.parseToJsonElement(toString())
        }
    }

    class JsonArrayBuilder {
        private val content = mutableListOf<JsonElement>()

        operator fun plusAssign(value: Any?) {
            content += value.toJsonElement()
        }

        fun obj(block: JsonObjectBuilder.() -> Unit) {
            this += JsonObjectBuilder().apply(block).build()
        }

        fun array(block: JsonArrayBuilder.() -> Unit) {
            this += JsonArrayBuilder().apply(block).build()
        }

        fun build() = JsonArray(content)

        private fun Any?.toJsonElement(): JsonElement = when (this) {
            null -> JsonNull
            is Boolean -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is String -> JsonPrimitive(this)
            is JsonElement -> this
            is Map<*, *> -> JsonObjectBuilder().apply {  // 关键修改点
                this@toJsonElement.forEach { (k, v) ->
                    k?.toString()?.let { key -> key with v }
                }
            }.build()

            is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
            else -> Json.parseToJsonElement(toString())
        }
    }
    // endregion

    // region 私有工具方法
    private data class ParsedSegment(
        val key: String,
        val index: Int?,
        val isArray: Boolean
    )

    private fun parsePath(path: String): List<String> {
        return path.split(Regex("""\.(?=(?:[^"']*["'][^"']*["'])*[^"']*$)"""))
            .flatMap { segment ->
                val parts = mutableListOf<String>()
                var current = segment
                while (current.isNotEmpty()) {
                    when {
                        current.startsWith('[') -> {
                            val endIndex = current.indexOf(']').takeIf { it != -1 } ?: break
                            parts.add(current.substring(0, endIndex + 1))
                            current = current.substring(endIndex + 1)
                        }

                        current[0] == '"' || current[0] == '\'' -> {
                            val quote = current[0]
                            val endIndex = current.indexOf(quote, 1)
                            if (endIndex == -1) {
                                parts.add(current)
                                break
                            }
                            parts.add(current.substring(0, endIndex + 1))
                            current = current.substring(endIndex + 1)
                        }

                        else -> {
                            val nextArray = current.indexOf('[')
                            if (nextArray == -1) {
                                parts.add(current)
                                break
                            }
                            parts.add(current.substring(0, nextArray))
                            current = current.substring(nextArray)
                        }
                    }
                }
                parts
            }
            .filter { it.isNotEmpty() }
    }


    private fun parseSegment(segment: String): ParsedSegment {
        return when {
            segment.startsWith('[') -> {
                val index = segment.substring(1, segment.length - 1).toIntOrNull() ?: -1
                ParsedSegment("", index, true)
            }

            segment.contains('[') -> {
                val key = segment.substringBefore('[')
                val indexStr = segment.substringAfter('[').substringBefore(']')
                val index = indexStr.toIntOrNull() ?: -1
                ParsedSegment(key.removeQuotes(), index, true)
            }

            else -> ParsedSegment(segment.removeQuotes(), null, false)
        }
    }

    private fun String.removeQuotes(): String = replace(Regex("^['\"]|['\"]$"), "")

    private fun handleArray(element: JsonElement, segment: ParsedSegment): JsonElement? {
        return when {
            // 处理纯数组语法 (例如 "[0]")
            segment.key.isEmpty() && element is JsonArray -> {
                element.getOrNull(segment.index ?: return null)
            }

            // 处理对象中的数组 (例如 "books[0]")
            else -> (element as? JsonObject)
                ?.get(segment.key)
                ?.takeIf { it is JsonArray }
                ?.let { (it as JsonArray).getOrNull(segment.index ?: return null) }
        }
    }

    private fun handleObject(element: JsonElement, segment: ParsedSegment): JsonElement? {
        return (element as? JsonObject)?.get(segment.key)
    }
    // endregion

    // region 扩展函数
    fun JsonElement.toExpert(autoParse: Boolean = true): JsonUltra =
        JsonUltra(this, autoParse)
    // endregion
}