package io.core.constant.detector

import android.os.Build
import io.core.constant.DeviceMappingLoader
import io.core.constant.DeviceOS.Rom
import io.core.constant.DeviceOS.SystemRomInfo
import io.core.constant.SystemPropertyCache

/**
 * 三星设备检测器
 * 支持One UI检测
 */
class SamsungDetector : RomDetectionStrategy {
    
    companion object {
        object SamsungProps {
            const val ONEUI_VERSION = "ro.build.version.oneui"
            const val ONEUI_GSM_NAME = "gsm.version.baseband"
            const val PRODUCT_MODEL = "ro.product.model"
        }
    }
    
    override val marketName: String
        get() = DeviceMappingLoader.getSamsungMappings()[Build.MODEL]
            ?: SystemPropertyCache.getProperty(SamsungProps.PRODUCT_MODEL)
    
    override fun detect(): SystemRomInfo {
        return if (SystemPropertyCache.hasProperty(SamsungProps.ONEUI_VERSION)) {
            val version = SystemPropertyCache.getProperty(SamsungProps.ONEUI_VERSION, Build.VERSION.INCREMENTAL)
            SystemRomInfo(
                Rom.ONE_UI,
                SystemPropertyCache.getProperty(SamsungProps.ONEUI_GSM_NAME),
                version,
                "One UI $version"
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
