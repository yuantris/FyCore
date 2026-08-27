package io.core.constant.detector

import android.os.Build
import io.core.constant.DeviceOS.Rom
import io.core.constant.DeviceOS.SystemRomInfo
import io.core.constant.SystemPropertyCache

/**
 * 荣耀设备检测器
 * 支持MagicOS和Magic UI检测
 */
class HonorDetector : RomDetectionStrategy {
    
    companion object {
        object HonorProps {
            const val HONOR_MAGIC_VERSION = "ro.magic.systemversion"
            const val HONOR_MAGIC_VERSION_NAME = "mscw.hnouc.patch.display.version"
            const val HONOR_MAGIC_VERSION_CODE = "msc.config.magic.version"
        }
    }
    
    override val marketName: String
        get() = SystemPropertyCache.getProperty("ro.config.marketing_name")
    
    override fun detect(): SystemRomInfo {
        return if (SystemPropertyCache.hasProperty("ro.horizon.version")) {
            SystemRomInfo(
                Rom.MAGIC_UI,
                SystemPropertyCache.getProperty("ro.horizon.version"),
                SystemPropertyCache.getProperty("ro.horizon.version.code", Build.VERSION.INCREMENTAL)
            )
        }
        // 检测MagicOS
        else if (SystemPropertyCache.hasProperty(HonorProps.HONOR_MAGIC_VERSION)) {
            val property = SystemPropertyCache.getProperty(
                HonorProps.HONOR_MAGIC_VERSION_CODE,
                Build.VERSION.INCREMENTAL
            )
            SystemRomInfo(
                Rom.MagicOS,
                SystemPropertyCache.getProperty(HonorProps.HONOR_MAGIC_VERSION_NAME),
                property,
                "MagicOS $property"
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
