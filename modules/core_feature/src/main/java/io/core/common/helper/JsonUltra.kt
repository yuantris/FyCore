package io.core.common.helper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

/**
 * ██╗  ██╗███████╗██╗   ██╗    ╔═════════════════╗
 * ╚██╗██╔╝██╔════╝╚██╗ ██╔╝  ♥ → X F Y  ♡  Z J Q ← ♥
 *  ╚███╔╝ █████╗   ╚████╔╝     ╚═╦═══════════════╝
 *  ██╔██╗ ██╔══╝    ╚██╔╝         ▀█▄
 * ██╔╝ ██╗██╗        ██║         ▄█▀
 * ╚═╝  ╚═╝╚═╝        ╚═╝       █▌ 丘比特协议 v11.11
 * --------------------------------------------
 * Json解析工具类，用于解析和操作JSON数据。
 * 优化版本：改进内存管理、性能和并发安全性
 * @param jsonElement JSON数据。
 * @param autoParse 是否自动解析字符串内容，默认为true。
 * @author [Yuantris]
 * 2025/2/13 13:49 - 2025/8/10 21:00 (优化版)
 */
class JsonUltra private constructor(
    private val jsonElement: JsonElement,
    private val autoParse: Boolean = true
) : AutoCloseable {

    // region 性能监控
    companion object Stats {
        private val parseTimeStats = AtomicLong()
        private val cacheHitStats = AtomicLong()
        private val cacheMissStats = AtomicLong()
        
        fun getStats(): Map<String, Long> = mapOf(
            "totalParseTime" to parseTimeStats.get(),
            "cacheHits" to cacheHitStats.get(),
            "cacheMisses" to cacheMissStats.get()
        )
        
        fun resetStats() {
            parseTimeStats.set(0)
            cacheHitStats.set(0)
            cacheMissStats.set(0)
        }
    }
    // endregion

    // region 缓存管理器
    private class CacheManager {
        private val pathCache = ConcurrentHashMap<String, List<String>>()
        private val keysCache = ConcurrentHashMap<Int, Set<String>>()
        private val maxCacheSize = 1000
        
        fun getCachedPath(path: String, computer: () -> List<String>): List<String> {
            return pathCache[path]?.also { 
                cacheHitStats.incrementAndGet() 
            } ?: run {
                cacheMissStats.incrementAndGet()
                val result = computer()
                if (pathCache.size < maxCacheSize) {
                    pathCache[path] = result
                }
                result
            }
        }
        
        fun getCachedKeys(hash: Int, computer: () -> Set<String>): Set<String> {
            return keysCache[hash] ?: run {
                val result = computer()
                if (keysCache.size < maxCacheSize) {
                    keysCache[hash] = result
                }
                result
            }
        }
        
        fun clear() {
            pathCache.clear()
            keysCache.clear()
        }
        
        fun size(): Pair<Int, Int> = pathCache.size to keysCache.size
    }
    
    private val cacheManager = CacheManager()
    // endregion

    // region 路径解析器
    private class PathParser {
        companion object {
            // 预编译正则表达式
            private val DOT_SPLIT_PATTERN = Pattern.compile("""(?<!\\)\.(?=(?:[^"']*["'][^"']*["'])*[^"']*$)""")
            private val QUOTE_PATTERN = Pattern.compile("""^['"](.*?)['"]$""")
            private val ARRAY_PATTERN = Pattern.compile("""^(.*?)\[(\d+)]$""")
            
            private const val MAX_RECURSION_DEPTH = 50
        }
        
        fun parsePath(path: String): List<String> {
            return DOT_SPLIT_PATTERN.split(path)
                .flatMap { segment ->
                    val processed = mutableListOf<String>()
                    var current = segment.replace("\\\\.", ".")
                    
                    while (current.isNotEmpty()) {
                        current = when {
                            current.startsWith('[') -> handleArrayNotation(current, processed)
                            current.startsWith('\'') || current.startsWith('"') -> handleQuotedSegment(current, processed)
                            else -> handlePlainSegment(current, processed)
                        }
                    }
                    processed
                }
                .filter { it.isNotEmpty() }
        }
        
        private fun handleArrayNotation(current: String, processed: MutableList<String>): String {
            val endIndex = current.indexOf(']').takeIf { it != -1 } ?: return ""
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
    }
    
    private val pathParser = PathParser()
    // endregion

    // region 自定义异常
    sealed class JsonUltraException(message: String, cause: Throwable? = null) : Exception(message, cause) {
        class PathNotFoundException(path: String, availablePaths: List<String>) : JsonUltraException(
            "路径 '$path' 不存在。可用路径: ${availablePaths.take(5).joinToString()}"
        )
        
        class TypeMismatchException(expected: String, actual: String, availablePaths: List<String>) : JsonUltraException(
            "类型不匹配！期望: $expected，实际: $actual。可用路径: ${availablePaths.take(3).joinToString()}"
        )
        
        class ConversionException(typeName: String, value: String, cause: Throwable) : JsonUltraException(
            "JSON值转换失败: $typeName，当前值: $value", cause
        )
    }
    // endregion

    // region 核心解析功能
    operator fun get(path: String): JsonUltra? {
        val startTime = System.nanoTime()
        try {
            val segments = cacheManager.getCachedPath(path) { 
                parseCalibratedPath(path) 
            }
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
        } finally {
            parseTimeStats.addAndGet(System.nanoTime() - startTime)
        }
    }

    fun getNotNull(path: String): JsonUltra {
        val startTime = System.nanoTime()
        try {
            val segments = cacheManager.getCachedPath(path) { 
                parseCalibratedPath(path) 
            }
            var current: JsonElement = jsonElement
            val processedPath = mutableListOf<String>()

            for (segment in segments) {
                current = current.autoParseStringContent()
                val parsedSegment = parseSegment(segment)
                current = when {
                    parsedSegment.isArray -> handleArray(current, parsedSegment)
                    else -> handleObject(current, parsedSegment)
                } ?: throw JsonUltraException.PathNotFoundException(path, getAllPaths())
                processedPath.add(segment)
            }
            return JsonUltra(current, autoParse)
        } finally {
            parseTimeStats.addAndGet(System.nanoTime() - startTime)
        }
    }

    private fun JsonElement.autoParseStringContent(depth: Int = 0): JsonElement {
        if (!autoParse || depth > PathParser.MAX_RECURSION_DEPTH) return this
        
        return when (this) {
            is JsonPrimitive -> if (isString) {
                try {
                    Json.parseToJsonElement(content).autoParseStringContent(depth + 1)
                } catch (e: Exception) {
                    this
                }
            } else this

            is JsonObject -> buildJsonObject {
                this@autoParseStringContent.forEach { (key, value) ->
                    put(key, value.autoParseStringContent(depth + 1))
                }
            }

            is JsonArray -> buildJsonArray {
                this@autoParseStringContent.forEach {
                    add(it.autoParseStringContent(depth + 1))
                }
            }

            else -> this
        }
    }
    // endregion

    // region 优化的类型转换方法
    private inline fun <reified T> asType(
        typeName: String,
        crossinline primitiveGetter: JsonPrimitive.() -> T
    ): T {
        return when (val element = jsonElement.autoParseStringContent()) {
            is JsonPrimitive -> try {
                element.primitiveGetter()
            } catch (e: Exception) {
                throw JsonUltraException.ConversionException(typeName, element.content, e)
            }
            else -> throw JsonUltraException.TypeMismatchException(
                typeName, 
                element::class.simpleName ?: "Unknown",
                getAllPaths()
            )
        }
    }

    fun asString(): String {
        return when (val element = jsonElement.autoParseStringContent()) {
            is JsonPrimitive -> element.content
            is JsonObject, is JsonArray -> Json.encodeToString(jsonElement)
            else -> throw JsonUltraException.TypeMismatchException(
                "String", 
                element::class.simpleName ?: "Unknown",
                getAllPaths()
            )
        }
    }

    fun asStringOrNull(): String? = runCatching { asString() }.getOrNull()
    fun asInt(): Int = asType("Int") { int }
    fun asIntOrNull(): Int? = runCatching { asInt() }.getOrNull()
    fun asBoolean(): Boolean = asType("Boolean") { boolean }
    fun asBooleanOrNull(): Boolean? = runCatching { asBoolean() }.getOrNull()
    fun asDouble(): Double = asType("Double") { double }
    fun asDoubleOrNull(): Double? = runCatching { asDouble() }.getOrNull()

    fun <T> asList(converter: (JsonUltra) -> T): List<T> {
        return when (val element = jsonElement.autoParseStringContent()) {
            is JsonArray -> element.map { JsonUltra(it, autoParse).let(converter) }
            else -> throw JsonUltraException.TypeMismatchException(
                "JsonArray", 
                element::class.simpleName ?: "Unknown",
                getAllPaths()
            )
        }
    }

    fun asMap(): Map<String, Any> = when (val element = jsonElement.autoParseStringContent()) {
        is JsonObject -> element.mapValues { (_, v) -> v.toAny() }
        else -> throw JsonUltraException.TypeMismatchException(
            "JsonObject", 
            element::class.simpleName ?: "Unknown",
            getAllPaths()
        )
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
    // endregion

    // region 路径和键管理
    fun getAllPaths(): List<String> {
        val paths = mutableSetOf<String>()
        val stack = ArrayDeque<Pair<String, JsonElement>>().apply {
            add("" to jsonElement.autoParseStringContent())
        }

        while (stack.isNotEmpty()) {
            val (currentPath, element) = stack.removeLast()

            when (element) {
                is JsonObject -> {
                    element.forEach { (k, v) ->
                        val newPath = if (currentPath.isEmpty()) k else "$currentPath.$k"
                        paths.add(newPath)
                        stack.add(newPath to v)
                    }
                }
                is JsonArray -> {
                    element.forEachIndexed { i, e ->
                        val newPath = "$currentPath[$i]"
                        paths.add(newPath)
                        stack.add(newPath to e)
                    }
                }
                else -> {
                    if (currentPath.isNotEmpty()) {
                        paths.add(currentPath)
                    }
                }
            }
        }

        return paths.toList()
    }

    private fun getAllKeys(jsonElement: JsonElement): Set<String> {
        val keys = mutableSetOf<String>()
        val stack = ArrayDeque<JsonElement>().apply { add(jsonElement) }

        while (stack.isNotEmpty()) {
            when (val current = stack.removeLast()) {
                is JsonObject -> {
                    current.keys.forEach { key ->
                        keys.add(key)
                        current[key]?.let { stack.add(it) }
                    }
                }
                is JsonArray -> {
                    current.forEach { stack.add(it) }
                }
            }
        }
        return keys
    }

    private fun calibrationPath(path: String): String {
        val allKeys = cacheManager.getCachedKeys(jsonElement.hashCode()) {
            getAllKeys(jsonElement)
        }

        var modifiedPath = path
        allKeys.sortedByDescending { it.length }.forEach { key ->
            modifiedPath = modifiedPath.replace(
                Regex("(?<!')${Regex.escape(key)}(?!')"),
                "'$key'"
            )
        }
        return modifiedPath
    }

    private fun parseCalibratedPath(rawPath: String): List<String> {
        val calibrated = calibrationPath(rawPath)
        return pathParser.parsePath(calibrated).also {
            validatePathSegments(it)
        }
    }

    private fun validatePathSegments(segments: List<String>) {
        if (segments.any { it.contains("''") || it.contains("\"\"") }) {
            throw IllegalArgumentException("路径包含无效的空引号：${segments.joinToString(".")}")
        }
    }
    // endregion

    // region 私有工具方法
    private data class ParsedSegment(
        val key: String,
        val index: Int?,
        val isArray: Boolean
    )

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
            segment.key.isEmpty() && element is JsonArray -> {
                element.getOrNull(segment.index ?: return null)
            }
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

    fun convertJsonString(): String = Json.encodeToString(jsonElement)

    override fun close() {
        cacheManager.clear()
    }

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
                    "JSON解析失败：${e.message}\n" +
                    "原始内容：${jsonString.take(200)}${if (jsonString.length > 200) "..." else ""}\n" +
                    "建议：使用JsonUltra.format()预处理字符串"
                )
            }
        }

        @JvmStatic
        fun formatAll(inputs: List<String>): Map<String, String> {
            return inputs.associateWith { format(it) }
        }

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

    // region 构建器类
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
            is Map<*, *> -> JsonObjectBuilder().apply {
                this@toJsonElement.forEach { (k, v) ->
                    k?.toString()?.let { key -> key with v }
                }
            }.build()
            is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
            else -> Json.parseToJsonElement(toString())
        }
    }
    // endregion

    // region 扩展函数
    fun JsonElement.toExpert(autoParse: Boolean = true): JsonUltra =
        JsonUltra(this, autoParse)
    // endregion
}