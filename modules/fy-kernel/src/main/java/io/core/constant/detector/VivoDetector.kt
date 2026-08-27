package io.core.constant.detector

import android.os.Build
import io.core.constant.DeviceOS.Brand
import io.core.constant.DeviceOS.Rom
import io.core.constant.DeviceOS.SystemRomInfo
import io.core.constant.SystemPropertyCache

/**
 * vivo设备检测器
 * 支持OriginOS和FuntouchOS检测
 */
class VivoDetector : RomDetectionStrategy {
    
    companion object {
        object VivoProps {
            const val VIVO_SERIES = "ro.vivo.product.series"
            const val VIVO_VERSION = "ro.vivo.os.version"
            const val VIVO_ORIGIN_OS = "ro.vivo.os.build.display.id"
            const val VIVO_VERSION_CODE_INC = "ro.vivo.product.version.incremental"
            const val VIVO_VERSION_CODE = "ro.vendor.vivo.product.version"
            const val MARKET_NAME = "ro.vivo.market.name"
        }
    }
    
    override val marketName: String
        get() = SystemPropertyCache.getProperty(VivoProps.MARKET_NAME)
    
    override fun detect(): SystemRomInfo {
        val isOriginOS = SystemPropertyCache.hasProperty(VivoProps.VIVO_ORIGIN_OS)
        val isFuntouchOS = SystemPropertyCache.hasProperty(VivoProps.VIVO_VERSION)
        
        return when {
            Brand.vivo == Brand.vivo && isOriginOS -> SystemRomInfo(
                Rom.OriginOS,
                SystemPropertyCache.getProperty(VivoProps.VIVO_VERSION_CODE_INC),
                SystemPropertyCache.getProperty(VivoProps.VIVO_VERSION, Build.VERSION.INCREMENTAL),
                SystemPropertyCache.getProperty(VivoProps.VIVO_ORIGIN_OS)
            )
            
            Brand.vivo == Brand.vivo && isFuntouchOS -> SystemRomInfo(
                Rom.FuntouchOS,
                SystemPropertyCache.getProperty(VivoProps.VIVO_VERSION_CODE),
                SystemPropertyCache.getProperty(VivoProps.VIVO_VERSION, Build.VERSION.INCREMENTAL)
            )
            
            else -> defaultAndroidInfo()
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
