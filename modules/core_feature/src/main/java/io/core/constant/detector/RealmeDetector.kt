package io.core.constant.detector

import android.os.Build
import io.core.common.util.tools.StringTools
import io.core.constant.DeviceOS.Rom
import io.core.constant.DeviceOS.SystemRomInfo
import io.core.constant.SystemPropertyCache

/**
 * realme设备检测器
 * 支持realme UI检测
 */
class RealmeDetector : RomDetectionStrategy {
    
    companion object {
        object RealmeProps {
            const val REALME_VERSION = "ro.build.version.realmeui"
            const val REALME_VERSION_NAME = "ro.build.display.id"
            const val MARKET_NAME_CN = "ro.vendor.oplus.market.name"
            const val MARKET_NAME_EN = "ro.vendor.oplus.market.enname"
        }
    }
    
    override val marketName: String
        get() = SystemPropertyCache.getProperty(RealmeProps.MARKET_NAME_EN)
    
    override fun detect(): SystemRomInfo {
        return if (SystemPropertyCache.hasProperty(RealmeProps.REALME_VERSION)) {
            val property = SystemPropertyCache.getProperty(RealmeProps.REALME_VERSION)
            SystemRomInfo(
                Rom.realme_UI,
                SystemPropertyCache.getProperty(RealmeProps.REALME_VERSION_NAME),
                SystemPropertyCache.getProperty(RealmeProps.REALME_VERSION, Build.VERSION.INCREMENTAL),
                "realme UI ${StringTools.extractNumber(property)}"
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
