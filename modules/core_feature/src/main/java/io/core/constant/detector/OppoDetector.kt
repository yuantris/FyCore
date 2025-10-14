package io.core.constant.detector

import io.core.common.util.tools.StringTools
import io.core.constant.DeviceOS.Rom
import io.core.constant.DeviceOS.SystemRomInfo
import io.core.constant.SystemPropertyCache

/**
 * OPPO设备检测器
 * 支持ColorOS检测
 */
class OppoDetector : RomDetectionStrategy {
    
    companion object {
        object OppoProps {
            const val OPPO_VERSION_CODE = "ro.build.version.oplusrom"
            const val OPPO_VERSION_NAME = "ro.build.display.id"
            const val OPPO_BRAND = "ro.oplus.image.system_ext.brand"
            const val MARKET_NAME = "ro.vendor.oplus.market.name"
        }
    }
    
    override val marketName: String
        get() = SystemPropertyCache.getProperty(OppoProps.MARKET_NAME)
    
    override fun detect(): SystemRomInfo {
        return if (SystemPropertyCache.hasProperty(OppoProps.OPPO_BRAND)) {
            val property = SystemPropertyCache.getProperty(OppoProps.OPPO_VERSION_CODE)
            SystemRomInfo(
                Rom.ColorOS,
                SystemPropertyCache.getProperty(OppoProps.OPPO_VERSION_NAME),
                property,
                "ColorOS ${StringTools.extractNumber(property)}"
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
        android.os.Build.VERSION.RELEASE,
        android.os.Build.VERSION.INCREMENTAL,
        "Android ${android.os.Build.VERSION.RELEASE} (${android.os.Build.ID})"
    )
}
