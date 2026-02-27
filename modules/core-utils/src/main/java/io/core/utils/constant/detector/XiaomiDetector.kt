package io.core.utils.constant.detector

import io.core.utils.tools.StringTools
import io.core.utils.constant.DeviceOS.Rom
import io.core.utils.constant.DeviceOS.SystemRomInfo
import io.core.utils.constant.SystemPropertyCache

/**
 * 小米设备检测器
 * 支持HyperOS和MIUI检�?
 */
class XiaomiDetector : RomDetectionStrategy {
    
    companion object {
        // 小米专属系统属性常�?
        object XiaomiProps {
            const val MIUI_VERSION_NAME = "ro.miui.ui.version.name"
            const val MIUI_VERSION_CODE = "ro.miui.ui.version.code"
            const val MIUI_VERSION_NAME_INC = "ro.odm.build.version.incremental"
            const val HYPER_VERSION_NAME = "ro.mi.os.version.name"
            const val HYPER_VERSION_INC = "ro.mi.os.version.incremental"
            const val HYPER_VERSION_CODE = "ro.mi.os.version.code"
            const val MARKET_NAME = "ro.product.marketname"
        }
    }
    
    override val marketName: String
        get() = SystemPropertyCache.getProperty(XiaomiProps.MARKET_NAME)
    
    override fun detect(): SystemRomInfo {
        val versionName = SystemPropertyCache.getProperty(XiaomiProps.MIUI_VERSION_NAME, "")
        return when {
            // 检测澎湃OS（V8+版本�?
            versionName.startsWith("V8") || SystemPropertyCache.hasProperty(XiaomiProps.HYPER_VERSION_NAME) -> {
                detectHyperOS(versionName)
            }
            SystemPropertyCache.hasProperty(XiaomiProps.MIUI_VERSION_NAME) -> {
                detectMIUI()
            }
            else -> defaultAndroidInfo()
        }
    }
    
    /**
     * 检测澎湃OS
     */
    private fun detectHyperOS(versionName: String): SystemRomInfo {
        val property = SystemPropertyCache.getProperty(XiaomiProps.HYPER_VERSION_NAME, versionName)
        val verCode = StringTools.extractNumber(property)
        return SystemRomInfo(
            Rom.HyperOS,
            SystemPropertyCache.getProperty(XiaomiProps.HYPER_VERSION_INC, property),
            verCode,
            "HyperOS $verCode"
        )
    }
    
    /**
     * 检测MIUI
     */
    private fun detectMIUI(): SystemRomInfo {
        val verCode = SystemPropertyCache.getProperty(XiaomiProps.MIUI_VERSION_CODE)
        return SystemRomInfo(
            Rom.MIUI,
            SystemPropertyCache.getProperty(XiaomiProps.MIUI_VERSION_NAME_INC),
            verCode,
            "MIUI $verCode"
        )
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
