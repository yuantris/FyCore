package io.core.utils.constant.detector

import io.core.utils.constant.DeviceOS.Brand
import io.core.utils.constant.DeviceOS.SystemRomInfo

/**
 * 设备检测器接口
 */
interface DeviceDetector {
    /**
     * 检测设备信�?
     */
    fun detect(): DeviceInfo
    
    /**
     * 获取市场名称
     */
    fun getMarketName(): String
}

/**
 * 品牌检测器接口
 */
interface BrandDetector {
    /**
     * 检测设备品�?
     */
    fun detectBrand(): Brand
}

/**
     * 检测系统ROM信息
     */
interface RomDetector {
    fun detectRom(): SystemRomInfo
}

/**
 * 设备信息数据�?
 */
data class DeviceInfo(
    val brand: Brand,
    val romInfo: SystemRomInfo,
    val marketName: String
)

/**
 * 品牌检测策略接�?
 */
interface BrandDetectionStrategy {
    fun matches(
        manufacturer: String,
        brand: String,
        model: String = "",
        product: String = ""
    ): Boolean
}

/**
 * ROM检测策略接�?
 */
interface RomDetectionStrategy {
    val marketName: String
    fun detect(): SystemRomInfo
}
