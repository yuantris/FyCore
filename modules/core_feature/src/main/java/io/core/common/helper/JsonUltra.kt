package io.core.common.helper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

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

    // region 安全类型转换方法
    /**
     * 获取当前节点的字符串值
     * @throws IllegalArgumentException 当节点不是基本类型或转换失败时抛出，包含详细类型和路径信息
     */
    fun asString(): String {
        if (jsonElement is JsonObject) {
            throw IllegalArgumentException("当前为JSON对象，请使用convertJsonString()")
        }
        return getPrimitiveValue("String") { content }
    }

    /**
     * 安全获取字符串值
     * @return 字符串值或null（当节点不存在或类型不匹配时）
     */
    fun asStringOrNull() = (jsonElement as? JsonPrimitive)?.contentOrNull

    /**
     * 获取整型值
     * @throws IllegalArgumentException 当节点不是数值类型或转换失败时抛出
     */
    fun asInt(): Int = getPrimitiveValue("Int") { int }

    /**
     * 安全获取整型值
     * @return 整型值或null（当节点不存在或类型不匹配时）
     */
    fun asIntOrNull() = (jsonElement as? JsonPrimitive)?.intOrNull

    /**
     * 获取布尔值
     * @throws IllegalArgumentException 当节点不是布尔类型时抛出
     */
    fun asBoolean(): Boolean = getPrimitiveValue("Boolean") { boolean }

    /**
     * 安全获取布尔值
     * @return 布尔值或null（当节点不存在或类型不匹配时）
     */
    fun asBooleanOrNull() = (jsonElement as? JsonPrimitive)?.booleanOrNull

    /**
     * 获取双精度浮点值
     * @throws IllegalArgumentException 当节点不是数值类型时抛出
     */
    fun asDouble(): Double = getPrimitiveValue("Double") { double }

    /**
     * 安全获取双精度浮点值
     * @return 双精度值或null（当节点不存在或类型不匹配时）
     */
    fun asDoubleOrNull() = (jsonElement as? JsonPrimitive)?.doubleOrNull

    /**
     * 通用列表转换方法
     * @param converter 元素转换逻辑
     * @throws IllegalArgumentException 当当前节点不是数组时抛出
     */
    fun <T> asList(converter: (JsonUltra) -> T): List<T> {
        return when (val element = jsonElement.autoParseStringContent()) {
            is JsonArray -> element.map { JsonUltra(it).let(converter) }
            else -> throw typeMismatchException("JsonArray", element)
        }
    }

    //    fun asMap(): Map<String, Any> = when (val element = jsonElement.autoParseStringContent()) {
//        is JsonObject -> element.mapValues { (_, v) -> v.toAny() }
//        else -> throw typeMismatchException("JsonObject", element)
//    }
    fun asMap(): Map<String, JsonUltra> {
        return when (val element = jsonElement.autoParseStringContent()) {
            is JsonObject -> element.mapValues { JsonUltra(it.value) }
            else -> throw typeMismatchException("JsonObject", element)
        }
    }

    private fun JsonElement.toAny(): Any = when (this) {
        is JsonPrimitive -> when {
            isString -> content
            booleanOrNull != null -> boolean
            intOrNull != null -> int
            doubleOrNull != null -> double
            else -> content
        }

        is JsonArray -> map { it.toAny() }
        is JsonObject -> mapValues { (_, v) -> v.toAny() }
        else -> this
    }

    /**
     * 获取所有路径
     */
    fun getAllPaths(): List<String> = buildList {
        fun traverse(path: String, element: JsonElement) {
            when (element) {
                is JsonObject -> element.forEach { (k, v) ->
                    val newPath = if (path.isEmpty()) k else "$path.$k"
                    add(newPath)
                    traverse(newPath, v)
                }

                is JsonArray -> element.forEachIndexed { i, e ->
                    val newPath = "$path[$i]"
                    add(newPath)
                    traverse(newPath, e)
                }

                else -> if (path.isNotEmpty()) add(path)
            }
        }
        traverse("", jsonElement.autoParseStringContent())
    }.distinct()

    fun convertJsonString() = Json.encodeToString(jsonElement)
    // endregion

    // region 构建功能
    companion object Builder {


        private val jsonFormatter by lazy {
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                isLenient = true
            }
        }

        @JvmStatic
        fun build(block: JsonObjectBuilder.() -> Unit): String {
            return JsonObjectBuilder().apply(block).toJsonString()
        }

        @JvmStatic
        @JvmOverloads
        fun parse(jsonString: String, autoParse: Boolean = true): JsonUltra {
            return runCatching {
                JsonUltra(jsonFormatter.parseToJsonElement(jsonString), autoParse)
            }.getOrElse { e ->
                throw IllegalArgumentException(
                    """
                JSON解析失败：${e.message}
                原始内容：${jsonString.take(200)}${if (jsonString.length > 200) "..." else ""}
                建议：使用JsonUltra.format()预处理字符串
            """.trimIndent()
                )
            }
        }


        /**
         * 批量格式化JSON字符串列表
         */
        @JvmStatic
        fun formatAll(inputs: List<String>): Map<String, String> {
            return inputs.associateWith { format(it) }
        }


        /**
         * Json字符串格式化，将Json字符串（多次序列化后的）转换为标准的Json格式
         * @param input 待格式化的JSON字符串
         * @param compact 是否压缩格式
         * @return 格式化后的JSON字符串
         */
        @JvmStatic
        @JvmOverloads
        fun format(input: String, compact: Boolean = false): String {
            return try {
                val parsed = JSONObject(input)
                serializeProcessedValue(parsed.processValue(), compact)
            } catch (e: JSONException) {
                try {
                    val parsed = JSONArray(input)
                    serializeProcessedValue(parsed.processValue(), compact)
                } catch (e: JSONException) {
                    input.cleanString()
                }
            }
        }

        private fun serializeProcessedValue(value: Any, compress: Boolean): String {
            val jsonStr = when (value) {
                is JSONObject -> if (compress) value.toString() else value.toString(2)
                is JSONArray -> if (compress) value.toString() else value.toString(2)
                else -> value.toString()
            }
            /**
             * 替换转义的斜杠为普通斜杠
             * 如输出为：
             * "imageUrlPrefix": "https:\/\/zycdn.ss.bscstorage.com\/wallpaper\/"
             */
            return jsonStr.replace("\\/", "/")
        }

        private fun String.cleanString(): String {
            var current = this.trim()
            var previous = ""
            while (current != previous) {
                previous = current
                current = current.replace("\\\\", "\\")
                    .replace("^\"|\"$".toRegex(), "")
                    .trim()
            }
            return current
        }

        private fun Any.processValue(): Any {
            return when (this) {
                is String -> {
                    val cleaned = this.cleanString()
                    try {
                        val json = JSONObject(cleaned)
                        json.processValue()
                    } catch (e: JSONException) {
                        try {
                            val arr = JSONArray(cleaned)
                            arr.processValue()
                        } catch (e: JSONException) {
                            cleaned
                        }
                    }
                }

                is JSONObject -> {
                    val newObj = JSONObject()
                    for (key in this.keys()) {
                        newObj.put(key, this.get(key).processValue())
                    }
                    newObj
                }

                is JSONArray -> {
                    val newArr = JSONArray()
                    for (i in 0 until this.length()) {
                        newArr.put(this[i].processValue())
                    }
                    newArr
                }

                else -> this
            }
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

    private inline fun <T> getPrimitiveValue(
        typeName: String,
        converter: JsonPrimitive.() -> T
    ): T {
        return when (val element = jsonElement.autoParseStringContent()) {
            is JsonPrimitive -> try {
                element.converter()
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "JSON值转换失败：${e.message}\n" +
                            "目标类型：$typeName\n" +
                            "当前值：${element.content}\n" +
                            "可用路径：${getAllPaths().take(5).joinToString()}"
                )
            }

            else -> throw typeMismatchException("JsonPrimitive", element)
        }
    }

    private fun typeMismatchException(expected: String, actual: JsonElement): Exception {
        return IllegalArgumentException(
            """
        类型不匹配！期望：$expected，实际：${actual::class.simpleName}
        解决方案：
        1. 检查路径是否正确：${getAllPaths().take(5)}
        2. 使用get()定位到正确节点
        3. 再使用asXXX()方法进行类型转换
        
        完整路径列表：${getAllPaths()}        """.trimIndent()
        )
    }

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