package io.core.common.helper

import com.google.gson.JsonParser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.SoftReference

/**
 * ██╗  ██╗███████╗██╗   ██╗    ┌──────────┐
 * ╚██╗██╔╝██╔════╝╚██╗ ██╔╝    │ 加载进度 │▰▰▰▰▰▰▰▰◯ 87%
 *  ╚███╔╝ █████╗   ╚████╔╝     └──────────┘
 *  ██╔██╗ ██╔══╝    ╚██╔╝      ╱╲▲△△△△△△△△
 * ██╔╝ ██╗██╗        ██║       ▉ ▏正在渲染配置矩阵...
 * ╚═╝  ╚═╝╚═╝        ╚═╝       ╲╱▼▽▽▽▽▽▽▽▽
 * 注释的艺术，正在生成......
 * 模块加载阶段 ████████████ 100%
 * 最后编译阶段 ████████░░░░ 65% (按 F12 解锁彩蛋)
 * --------------------------------------------
 * Json解析工具类，用于解析和操作JSON数据。
 * @param jsonElement JSON数据。
 * @param autoParse 是否自动解析字符串内容，默认为true。
 * @author [Yuantris]
 * 2025/2/13 13:49
 */
class JsonUltra private constructor(
    private val jsonElement: JsonElement,
    private val autoParse: Boolean = true
) {
    // region 核心解析功能
    operator fun get(path: String): JsonUltra? {
        // 新增路径缓存优化
        val cachedPath = pathCache.getOrPut(path) { parseCalibratedPath(path) }
        var current: JsonElement = jsonElement

        for (segment in cachedPath) {
            current = current.autoParseStringContent()
            val parsedSegment = parseSegment(segment)
            current = when {
                parsedSegment.isArray -> handleArray(current, parsedSegment)
                else -> handleObject(current, parsedSegment)
            } ?: return null
        }
        return JsonUltra(current, autoParse)
    }

    /**
     * 获取当前节点，并确保节点存在，否则抛出异常
     * 用于Java环境调用者已知存在此节点的情况下使用，如{code,message,data}结构直接取code
     */
    fun getNotNull(path: String): JsonUltra {
        val cachedPath = pathCache.getOrPut(path) { parseCalibratedPath(path) }
        var current: JsonElement = jsonElement
        val processedPath = mutableListOf<String>()

        for (segment in cachedPath) {
            current = current.autoParseStringContent()
            val parsedSegment = parseSegment(segment)
            current = when {
                parsedSegment.isArray -> handleArray(current, parsedSegment)
                else -> handleObject(current, parsedSegment)
            } ?: throw IllegalArgumentException(
                "路径 '$path' 不存在\n" +
                        "失败位置：'${parsedSegment.key}' (段: ${processedPath.joinToString(".") + "." + segment})\n" +
                        "可能原因：\n" +
                        "1. 实际JSON结构缺少该字段\n" +
                        "2. 节点类型不匹配（尝试访问对象但实际是数组）\n" +
                        "可用路径：${getAllPaths().take(5).joinToString()}"
            )
            processedPath.add(segment)
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

    // 统一类型转换模板
    private inline fun <reified T> asType(
        typeName: String,
        crossinline primitiveGetter: JsonPrimitive.() -> T
    ): T {
        return when (val element = jsonElement.autoParseStringContent()) {
            is JsonPrimitive -> try {
                element.primitiveGetter()
            } catch (e: Exception) {
                throw conversionException(typeName, element, e)
            }

            else -> throw typeMismatchException(typeName, element)
        }
    }

    // 生成统一的转换异常
    private fun conversionException(
        typeName: String,
        element: JsonElement,
        cause: Exception
    ): IllegalArgumentException {
        val currentValue = when (element) {
            is JsonPrimitive -> element.content
            else -> element.toString()
        }

        return IllegalArgumentException(
            """
        JSON值转换失败：${cause.message}
        目标类型：$typeName
        当前值：$currentValue
        可用路径：${getAllPaths().take(5).joinToString()}
        解决方案：
        1. 使用get()获取具体子节点
        2. 使用asMap()/asList()处理复杂结构
        3. 使用convertJsonString()获取完整JSON
        """.trimIndent()
        )
    }

    // region 安全类型转换方法
    /**
     * 获取当前节点的字符串值
     * @throws IllegalArgumentException 当节点不是基本类型或转换失败时抛出，包含详细类型和路径信息
     */
    fun asString(): String {
        return when (val element = jsonElement.autoParseStringContent()) {
            is JsonPrimitive -> asType("String") { content }
            is JsonObject, is JsonArray -> Json.encodeToString(jsonElement)
            else -> throw typeMismatchException("String", element)
        }
    }

    /**
     * 安全获取字符串值
     * @return 字符串值或null（当节点不存在或类型不匹配时）
     */
    fun asStringOrNull() = runCatching { asString() }.getOrNull()

    /**
     * 获取整型值
     * @throws IllegalArgumentException 当节点不是数值类型或转换失败时抛出
     */
    fun asInt(): Int = asType("Int") { int }

    /**
     * 安全获取整型值
     * @return 整型值或null（当节点不存在或类型不匹配时）
     */
    fun asIntOrNull() = runCatching { asInt() }.getOrNull()

    /**
     * 获取布尔值
     * @throws IllegalArgumentException 当节点不是布尔类型时抛出
     */
    fun asBoolean(): Boolean = asType("Boolean") { boolean }

    /**
     * 安全获取布尔值
     * @return 布尔值或null（当节点不存在或类型不匹配时）
     */
    fun asBooleanOrNull() = runCatching { asBoolean() }.getOrNull()

    /**
     * 获取双精度浮点值
     * @throws IllegalArgumentException 当节点不是数值类型时抛出
     */
    fun asDouble(): Double = asType("Double") { double }

    /**
     * 安全获取双精度浮点值
     * @return 双精度值或null（当节点不存在或类型不匹配时）
     */
    fun asDoubleOrNull() = runCatching { asDouble() }.getOrNull()

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

    /**
     * 获取键值对
     * @throws IllegalArgumentException 当当前节点不是对象时抛出
     */
    fun asMap(): Map<String, Any> = when (val element = jsonElement.autoParseStringContent()) {
        is JsonObject -> element.mapValues { (_, v) -> v.toAny() }
        else -> throw typeMismatchException("JsonObject", element)
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

    /**
     * 转换为JSON字符串
     */
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

    // TODO: 2025/3/14 16:59 新增如下默认解析方法
    /** 将所有没有被单引号包裹的key，增加单引号包裹，降低输入Path错误率*/
    private fun calibrationPath(path: String): String {
        val element = JsonParser.parseString(Json.encodeToString(jsonElement))
        val allKeys: Set<String> = getAllKeysCached(element)

        var modifiedPath = path
        allKeys.sortedByDescending { it.length }.forEach { key ->
            modifiedPath = modifiedPath.replace(
                Regex("(?<!')${Regex.escape(key)}(?!')"),
                "'$key'"
            )
        }
        return modifiedPath
    }

    private fun getAllKeysCached(element: com.google.gson.JsonElement): Set<String> {
        val currentJson = Json.encodeToString(jsonElement)
        val currentHash = currentJson.hashCode()

        return keysCache?.get()?.takeIf { it.jsonHash == currentHash }?.keys
            ?: run {
                val newKeys = getAllKeys(element)
                keysCache = SoftReference(KeyCache(currentHash, newKeys))
                newKeys
            }
    }

    /** 嵌套获取Json（Gson方式）所有的key */
    private fun getAllKeys(jsonElement: com.google.gson.JsonElement): Set<String> {
        val keys = mutableSetOf<String>()
        when (jsonElement) {
            is com.google.gson.JsonObject -> {
                jsonElement.keySet().forEach { key ->
                    keys.add(key)
                    keys.addAll(getAllKeys(jsonElement.get(key)))
                }
            }

            is com.google.gson.JsonArray -> {
                jsonElement.forEach { element ->
                    keys.addAll(getAllKeys(element))
                }
            }
            // JsonPrimitive 和 JsonNull 不处理
        }
        return keys
    }

    private data class KeyCache(
        val jsonHash: Int,
        val keys: Set<String>
    )

    // 使用软引用缓存，防止内存泄漏
    private var keysCache: SoftReference<KeyCache>? = null
    // 新增路径缓存（使用软引用避免内存泄漏）
    private val pathCache = mutableMapOf<String, List<String>>()
        .withDefault { parseCalibratedPath(it) }

    /**
     * 合并路径校准与解析步骤
     * @return 预解析的路径段列表
     */
    private fun parseCalibratedPath(rawPath: String): List<String> {
        val calibrated = calibrationPath(rawPath)
        return parsePath(calibrated).also {
            validatePathSegments(it)  // 新增路径校验
        }
    }

    /**
     * 新增路径段校验逻辑
     */
    private fun validatePathSegments(segments: List<String>) {
        if (segments.any { it.contains("''") || it.contains("\"\"") }) {
            throw IllegalArgumentException("路径包含无效的空引号：${segments.joinToString(".")}")
        }
    }

    private fun parsePath(path: String): List<String> {
        return path.split(Regex("""(?<!\\)\.(?=(?:[^"']*["'][^"']*["'])*[^"']*$)""")) // 新增转义点号支持
            .flatMap { segment ->
                val processed = mutableListOf<String>()
                var current = segment.replace("\\\\.", ".") // 处理转义点号

                while (current.isNotEmpty()) {
                    when {
                        current.startsWith('[') -> handleArrayNotation(current, processed)
                        current.startsWith('\'') || current.startsWith('"') -> handleQuotedSegment(current, processed)
                        else -> handlePlainSegment(current, processed)
                    }.let { current = it }
                }
                processed
            }
            .filter { it.isNotEmpty() }
    }

    // 新增三种路径段处理策略
    private fun handleArrayNotation(current: String, processed: MutableList<String>): String {
        val endIndex = current.indexOfFirst { it == ']' }.takeIf { it != -1 } ?: return ""
        processed.add(current.substring(0, endIndex + 1))
        return current.substring(endIndex + 1)
    }

    private fun handleQuotedSegment(current: String, processed: MutableList<String>): String {
        val quote = current[0]
        val endIndex = current.indexOf(quote, 1).takeIf { it != -1 } ?: return ""
        processed.add(current.substring(0, endIndex + 1))
        return current.substring(endIndex + 1)
    }

    private fun handlePlainSegment(current: String, processed: MutableList<String>): String {
        val nextArray = current.indexOf('[')
        return if (nextArray == -1) {
            processed.add(current)
            ""
        } else {
            processed.add(current.substring(0, nextArray))
            current.substring(nextArray)
        }
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