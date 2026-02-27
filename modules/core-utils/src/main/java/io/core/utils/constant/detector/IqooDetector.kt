package io.core.utils.constant.detector

import android.os.Build
import io.core.utils.constant.DeviceOS.Rom
import io.core.utils.constant.DeviceOS.SystemRomInfo
import io.core.utils.constant.SystemPropertyCache

/**
 * iQOO设备检测器
 * 支持OriginOS检�?
 */
class IqooDetector : RomDetectionStrategy {
    
    companion object {
        object IqooProps {
            const val IQOO_VERSION = "ro.vivo.os.version"
            const val IQOO_ORIGIN_OS = "ro.vivo.os.build.display.id"
            const val IQOO_VERSION_CODE_INC = "ro.vivo.product.version.incremental"
            const val MARKET_NAME = "ro.vivo.market.name"
        }
    }
    
    override val marketName: String
        get() = SystemPropertyCache.getProperty(IqooProps.MARKET_NAME)
    
    override fun detect(): SystemRomInfo {
        return if (SystemPropertyCache.hasProperty(IqooProps.IQOO_ORIGIN_OS)) {
            SystemRomInfo(
                Rom.OriginOS,
                SystemPropertyCache.getProperty(IqooProps.IQOO_VERSION_CODE_INC),
                SystemPropertyCache.getProperty(IqooProps.IQOO_VERSION, Build.VERSION.INCREMENTAL),
                SystemPropertyCache.getProperty(IqooProps.IQOO_ORIGIN_OS)
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
