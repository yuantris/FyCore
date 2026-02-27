package io.core.utils.constant.detector

import android.os.Build
import io.core.utils.constant.DeviceOS.Rom
import io.core.utils.constant.DeviceOS.SystemRomInfo
import io.core.utils.constant.SystemPropertyCache

/**
 * 魅族设备检测器
 * 支持Flyme检�?
 */
class MeizuDetector : RomDetectionStrategy {
    
    override val marketName: String
        get() = ""
    
    override fun detect(): SystemRomInfo {
        return if (SystemPropertyCache.hasProperty("ro.build.flyme.version")) {
            SystemRomInfo(
                Rom.Flyme,
                SystemPropertyCache.getProperty("ro.build.flyme.version"),
                SystemPropertyCache.getProperty("ro.build.display.id", Build.DISPLAY)
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
