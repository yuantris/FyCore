package io.core.constant

import io.core.appCtx
import io.core.common.util.log.LogPure
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * 设备映射数据加载器
 * 从外部JSON文件加载设备型号映射，替代硬编码
 */
object DeviceMappingLoader {
    
    private val cache = ConcurrentHashMap<String, Map<String, String>>()
    
    /**
     * 获取华为设备映射
     */
    fun getHuaweiMappings(): Map<String, String> {
        return cache.getOrPut("huawei") {
            loadMappingsFromAssets("device_mappings/huawei.fy")
        }
    }
    
    /**
     * 获取三星设备映射
     */
    fun getSamsungMappings(): Map<String, String> {
        return cache.getOrPut("samsung") {
            loadMappingsFromAssets("device_mappings/samsung.fy")
        }
    }
    
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        cache.clear()
    }
    
    /**
     * 从Assets加载映射数据
     */
    private fun loadMappingsFromAssets(fileName: String): Map<String, String> {
        return try {
            val jsonString = appCtx.assets.open(fileName).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val result = mutableMapOf<String, String>()
            
            // 遍历JSON对象，将所有系列的设备映射合并到一个Map中
            jsonObject.keys().forEach { seriesKey ->
                val series = jsonObject.getJSONObject(seriesKey)
                series.keys().forEach { modelKey ->
                    result[modelKey] = series.getString(modelKey)
                }
            }
            
            LogPure.d("DeviceMappingLoader") { "Loaded ${result.size} mappings from $fileName" }
            result
        } catch (e: IOException) {
            LogPure.w("DeviceMappingLoader") { "Failed to load mappings from $fileName: ${e.message}" }
            emptyMap()
        } catch (e: Exception) {
            LogPure.w("DeviceMappingLoader") { "Error parsing mappings from $fileName: ${e.message}" }
            emptyMap()
        }
    }
}
