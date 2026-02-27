package io.core.utils.constant.detector

import io.core.utils.constant.DeviceOS.Brand
import io.core.utils.constant.DeviceOS.Rom
import io.core.utils.constant.DeviceOS.SystemRomInfo

/**
 * ROM检测器工厂
 * 根据品牌创建对应的ROM检测器
 */
object RomDetectorFactory {
    
    private val detectors: Map<Brand, RomDetectionStrategy> = mapOf(
        // 华为�?
        Brand.HUAWEI to HuaweiDetector(),
        Brand.HONOR to HonorDetector(),
        
        // 小米�?
        Brand.Xiaomi to XiaomiDetector(),
        Brand.REDMI to XiaomiDetector(),
        Brand.POCO to XiaomiDetector(),
        
        // OPPO�?
        Brand.OPPO to OppoDetector(),
        Brand.realme to RealmeDetector(),
        Brand.OnePlus to OnePlusDetector(),
        
        // vivo�?
        Brand.vivo to VivoDetector(),
        Brand.IQOO to IqooDetector(),
        
        // 三星
        Brand.SAMSUNG to SamsungDetector(),
        
        // 魅族
        Brand.MEIZU to MeizuDetector(),
        
        // 其他品牌可以继续添加
        // Brand.SONY to SonyDetector(),
        // Brand.LENOVO to LenovoDetector(),
        // Brand.ZTE to ZteDetector(),
        // Brand.NUBIA to NubiaDetector(),
        // Brand.ASUS to AsusDetector(),
        // Brand.Google to GoogleDetector(),
        // Brand.MOTOROLA to MotorolaDetector(),
        // Brand.NOKIA to NokiaDetector()
    )
    
    /**
     * 创建ROM检测器
     */
    fun createDetector(brand: Brand): RomDetectionStrategy {
        return detectors[brand] ?: DefaultAndroidUIDetector()
    }
}

/**
 * 默认Android UI检测器
 */
private class DefaultAndroidUIDetector : RomDetectionStrategy {
    
    override val marketName: String
        get() = "Android Phone"
    
    override fun detect(): SystemRomInfo {
        return SystemRomInfo(
            Rom.ANDROID,
            android.os.Build.VERSION.RELEASE,
            android.os.Build.VERSION.INCREMENTAL,
            "Android ${android.os.Build.VERSION.RELEASE} (${android.os.Build.ID})"
        )
    }
}
