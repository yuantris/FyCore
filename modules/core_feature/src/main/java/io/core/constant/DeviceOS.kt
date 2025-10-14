package io.core.constant

import android.os.Build
import io.core.common.util.log.LogPure
import io.core.constant.detector.BrandDetectorImpl
import io.core.constant.detector.DeviceInfo
import io.core.constant.detector.RomDetectorFactory

/**
 * 设备信息检测工具类（优化版）
 * 功能：
 * 1. 检测设备品牌（华米OV等）
 * 2. 检测系统Rom类型（MIUI、EMUI、ColorOS等）
 * 3. 获取系统版本信息
 * 
 * 优化特性：
 * - 系统属性缓存，提升性能
 * - 模块化设计，易于扩展
 * - 线程安全，支持并发访问
 * - 数据外部化，便于维护
 */
object DeviceOS {

    // ========== 枚举定义 ==========

    /** 设备品牌枚举 */
    enum class Brand {
        // 主品牌
        HUAWEI, Xiaomi, OPPO, vivo, SAMSUNG,
        SONY, LENOVO, ZTE, NUBIA, HONOR, ASUS,
        Google, MOTOROLA, NOKIA, MEIZU,

        // 子品牌
        REDMI, POCO, IQOO, OnePlus, realme,

        UNKNOWN
    }

    /** 系统Rom类型枚举 */
    /** 带<$>表示已完成验证 */
    enum class Rom {
        MIUI,         // 小米MIUI (Deprecation)
        HyperOS,     // 小米澎湃OS <$>
        EMUI,         // 华为EMUI (Deprecation)
        HarmonyOS,   // 华为鸿蒙OS <$>
        ColorOS,     // OPPO ColorOS <$>
        FuntouchOS,  // vivo FuntouchOS (Deprecation)
        OriginOS,   // vivo原OS <$>
        Flyme,        // 魅族Flyme
        H2OS,         // 一加H2OS (Deprecation)
        realme_UI,    // realme UI <$>
        ONE_UI,       // 三星One UI <$>
        XPERIA_UI,    // 索尼Xperia UI
        ZUI,          // 联想ZUI
        MYOS,         // 中兴MyOS
        NUBIA_UI,     // 努比亚UI
        MAGIC_UI,     // 荣耀Magic UI (Deprecation)
        MagicOS,   // 荣耀MagicOS <$>
        ROG_UI,       // 华硕ROG UI
        ANDROID      // 安卓原生（Google Pixel UI）
    }

    /** 系统UI信息数据类（包含类型和版本信息） */
    data class SystemRomInfo(
        val type: Rom,
        val verName: String,
        val verCode: String,
        val verDesc: String = "" // HyperOS 2.0
    )

    // ========== 初始化 ==========
    private val brandDetector = BrandDetectorImpl()

    // ========== 公共API ==========

    /**
     * 获取设备品牌（带缓存）
     */
    @JvmStatic
    val brand: Brand by lazy { 
        val key = DeviceInfoCache.generateKey(
            Build.MANUFACTURER, 
            Build.BRAND, 
            Build.MODEL
        )
        DeviceInfoCache.getOrCompute(key) {
            DeviceInfo(
                brandDetector.detectBrand(),
                SystemRomInfo(Rom.ANDROID, "", ""),
                ""
            )
        }.brand
    }

    /**
     * 获取ROM信息（带缓存）
     */
    @JvmStatic
    val romInfo: SystemRomInfo by lazy {
        val key = DeviceInfoCache.generateKey(
            Build.MANUFACTURER, 
            Build.BRAND, 
            Build.MODEL
        )
        DeviceInfoCache.getOrCompute(key) {
            val detectedBrand = brandDetector.detectBrand()
            val romDetector = RomDetectorFactory.createDetector(detectedBrand)
            DeviceInfo(
                detectedBrand,
                romDetector.detect(),
                romDetector.marketName
            )
        }.romInfo
    }

    @JvmStatic
    val rom: Rom get() = romInfo.type

    @JvmStatic
    val marketName: String by lazy {
        val key = DeviceInfoCache.generateKey(
            Build.MANUFACTURER, 
            Build.BRAND, 
            Build.MODEL
        )
        DeviceInfoCache.getOrCompute(key) {
            val detectedBrand = brandDetector.detectBrand()
            val romDetector = RomDetectorFactory.createDetector(detectedBrand)
            DeviceInfo(
                detectedBrand,
                romDetector.detect(),
                romDetector.marketName
            )
        }.marketName
    }

    // 快捷访问属性
    @JvmStatic
    val isHuawei: Boolean get() = brand == Brand.HUAWEI

    @JvmStatic
    val isXiaomi: Boolean get() = brand == Brand.Xiaomi

    @JvmStatic
    val isMIUI: Boolean get() = rom == Rom.MIUI

    @JvmStatic
    val isHyperOS: Boolean get() = rom == Rom.HyperOS

    @JvmStatic
    val isHarmonyOS: Boolean get() = rom == Rom.HarmonyOS

    @JvmStatic
    val isColorOS: Boolean get() = rom == Rom.ColorOS

    /**
     * 清除所有缓存
     */
    @JvmStatic
    fun clearCache() {
        DeviceInfoCache.clear()
        SystemPropertyCache.clearCache()
        DeviceMappingLoader.clearCache()
        LogPure.d("DeviceOS") { "All caches cleared" }
    }
    
    /**
     * 获取缓存统计信息
     */
    @JvmStatic
    fun getCacheStats(): String {
        return "DeviceInfo: ${DeviceInfoCache.size()}, SystemProperty: ${SystemPropertyCache.getCacheSize()}"
    }

}