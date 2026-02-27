package io.core.utils.constant.detector

import android.os.Build
import io.core.utils.constant.DeviceOS.Rom
import io.core.utils.constant.DeviceOS.SystemRomInfo
import io.core.utils.constant.SystemPropertyCache

/**
 * 一加设备检测器
 * 支持ColorOS检�?
 */
class OnePlusDetector : RomDetectionStrategy {
    
    companion object {
        object OnePlusProps {
            const val ONEPLUS_VERSION = "ro.build.version.oplusrom"
            const val ONEPLUS_VERSION_DISPLAY = "ro.build.version.oplusrom.display"
            const val ONEPLUS_OTA_DISPLAY = "persist.sys.oplus.ota_ver_display"
            const val MARKET_NAME_CN = "ro.vendor.oplus.market.name"
            const val MARKET_NAME_EN = "ro.vendor.oplus.market.enname"
        }
    }
    
    override val marketName: String
        get() = SystemPropertyCache.getProperty(OnePlusProps.MARKET_NAME_EN)
    
    override fun detect(): SystemRomInfo {
        return if (SystemPropertyCache.hasProperty(OnePlusProps.ONEPLUS_VERSION_DISPLAY)) {
            val property = SystemPropertyCache.getProperty(OnePlusProps.ONEPLUS_VERSION)
            SystemRomInfo(
                Rom.ColorOS,
                SystemPropertyCache.getProperty(OnePlusProps.ONEPLUS_OTA_DISPLAY),
                SystemPropertyCache.getProperty(property, Build.VERSION.INCREMENTAL),
                "ColorOS $property"
            )
        } else {
            defaultAndroidInfo()
        }
    }
    
    /**
     * 获取默认Android信息
     */
    private fun defaultAndroidInfo() = SystemRomInfo(
        Rom.ANDROID,
        Build.VERSION.RELEASE,
        Build.VERSION.INCREMENTAL,
        "Android ${Build.VERSION.RELEASE} (${Build.ID})"
    )
}
