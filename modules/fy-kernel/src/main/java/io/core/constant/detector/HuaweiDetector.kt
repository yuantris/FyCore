package io.core.constant.detector

import io.core.constant.DeviceMappingLoader
import io.core.constant.DeviceOS.Brand
import io.core.constant.DeviceOS.Rom
import io.core.constant.DeviceOS.SystemRomInfo
import io.core.constant.SystemPropertyCache

/**
 * 华为设备检测器
 * 支持鸿蒙OS和EMUI检测
 */
class HuaweiDetector : RomDetectionStrategy {
    
    companion object {
        // 华为专属系统属性常量
        object HuaweiProps {
            const val EMUI_VERSION = "ro.build.version.emui"
            const val EMUI_VERSION_CODE = "ro.build.version.emui.code"
            const val HARMONY_DISPLAY_ID = "ro.huawei.build.display.id"
            const val HARMONY_PLATFORM_VER = "hw_sc.build.platform.version"
            const val OEM_NAME = "ro.hw.oemName"
            const val MARKET_NAME = "ro.product.name"
        }
    }
    
    override val marketName: String
        get() {
            val productName = SystemPropertyCache.getProperty(HuaweiProps.MARKET_NAME)
            return DeviceMappingLoader.getHuaweiMappings()[productName]
                ?: "${Brand.HUAWEI.name} $productName"
        }
    
    override fun detect(): SystemRomInfo {
        return when {
            isHarmonyOS() -> detectHarmonyOS()
            hasEMUI() -> detectEMUI()
            else -> defaultAndroidInfo()
        }
    }
    
    /**
     * 检测鸿蒙OS
     */
    private fun detectHarmonyOS(): SystemRomInfo {
        val versionName = SystemPropertyCache.getProperty(HuaweiProps.HARMONY_DISPLAY_ID, "HarmonyOS")
            .replace(SystemPropertyCache.getProperty(HuaweiProps.OEM_NAME), "")
            .removeWhitespace()
        val versionCode = SystemPropertyCache.getProperty(
            HuaweiProps.HARMONY_PLATFORM_VER,
            android.os.Build.VERSION.INCREMENTAL
        )
        return SystemRomInfo(
            Rom.HarmonyOS,
            versionName,
            versionCode,
            "${Rom.HarmonyOS.name} $versionCode"
        )
    }
    
    /**
     * 检测EMUI
     */
    private fun detectEMUI(): SystemRomInfo {
        return SystemRomInfo(
            Rom.EMUI,
            SystemPropertyCache.getProperty(HuaweiProps.EMUI_VERSION),
            SystemPropertyCache.getProperty(HuaweiProps.EMUI_VERSION_CODE, android.os.Build.VERSION.INCREMENTAL)
        )
    }
    
    /**
     * 检查是否是鸿蒙系统
     */
    private fun isHarmonyOS(): Boolean {
        return try {
            Class.forName("ohos.system.version.SystemVersion")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    /**
     * 检查是否有EMUI属性
     */
    private fun hasEMUI(): Boolean {
        return SystemPropertyCache.hasProperty(HuaweiProps.EMUI_VERSION)
    }
    
    /**
     * 获取默认Android信息
     */
    private fun defaultAndroidInfo() = SystemRomInfo(
        Rom.ANDROID,
        android.os.Build.VERSION.RELEASE,
        android.os.Build.VERSION.INCREMENTAL,
        "Android ${android.os.Build.VERSION.RELEASE} (${android.os.Build.ID})"
    )
}
