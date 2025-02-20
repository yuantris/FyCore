package com.core.fy.android.util

import com.google.gson.*
import java.util.concurrent.ConcurrentHashMap

class JsonHelper(jsonStr: String) {
    private val dataMap = ConcurrentHashMap<String, String?>()

    init {
        try {
            val jsonElement = JsonParser.parseString(jsonStr)
            processJsonElement("", jsonElement)
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("Invalid JSON format", e)
        }
    }

    private fun processJsonElement(basePath: String, element: JsonElement) {
        when {
            element.isJsonObject -> handleJsonObject(basePath, element.asJsonObject)
            element.isJsonArray -> handleJsonArray(basePath, element.asJsonArray)
            element.isJsonPrimitive -> handlePrimitive(basePath, element.asJsonPrimitive)
            element.isJsonNull -> handleJsonNull(basePath)
        }
    }

    private fun handleJsonObject(basePath: String, jsonObject: JsonObject) {
        jsonObject.entrySet().forEach { (key, value) ->
            val newPath = if (basePath.isEmpty()) key else "$basePath.$key"
            processJsonElement(newPath, value)
        }
    }

    private fun handleJsonArray(basePath: String, jsonArray: JsonArray) {
        val prefix = basePath.ifEmpty { "$" }
        jsonArray.forEachIndexed { index, element ->
            val newPath = "$prefix[$index]"
            processJsonElement(newPath, element)
        }
    }

    private fun handlePrimitive(basePath: String, primitive: JsonPrimitive) {
        dataMap[basePath] = when {
            primitive.isBoolean -> primitive.asBoolean.toString()
            primitive.isNumber -> {
                val num = primitive.asNumber
                if (num.toLong().toDouble() == num.toDouble()) {
                    num.toLong().toString()
                } else {
                    num.toString()
                }
            }
            else -> primitive.asString
        }
    }

    private fun handleJsonNull(basePath: String) {
        dataMap[basePath] = null
    }

    // region Getter Methods
    fun getString(key: String): String? = dataMap[key]
    fun getInt(key: String): Int? = dataMap[key]?.toIntOrNull()
    fun getBoolean(key: String): Boolean? = dataMap[key]?.toBooleanStrictOrNull()
    fun getDouble(key: String): Double? = dataMap[key]?.toDoubleOrNull()
    fun getLong(key: String): Long? = dataMap[key]?.toLongOrNull()
    fun getFloat(key: String): Float? = dataMap[key]?.toFloatOrNull()
    fun containsKey(key: String): Boolean = dataMap.containsKey(key)
    fun getAllKeys(): Set<String> = dataMap.keys
    // endregion

    companion object {
        @JvmStatic
        fun parse(jsonStr: String): JsonHelper {
            return JsonHelper(jsonStr)
        }
    }
}