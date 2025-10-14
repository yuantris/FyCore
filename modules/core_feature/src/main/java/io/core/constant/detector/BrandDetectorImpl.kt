package io.core.constant.detector

import android.os.Build
import io.core.constant.DeviceOS.Brand
import io.core.constant.SystemPropertyCache
import io.core.common.util.log.LogPure
import io.core.common.util.log.bury.AppLog

/**
 * 品牌检测器实现
 * 使用策略模式进行品牌检测
 */
class BrandDetectorImpl : BrandDetector {

    // 所有品牌检测策略列表
    private val strategies: List<Pair<Brand, BrandDetectionStrategy>> = listOf(
        // Sub-brands first (优先级更高)
        Brand.REDMI to RedmiStrategy(),
        Brand.POCO to PocoStrategy(),
        Brand.IQOO to IqooStrategy(),
        Brand.OnePlus to OnePlusStrategy(),
        Brand.realme to RealmeStrategy(),

        // Main brands
        Brand.HUAWEI to SimpleBrandStrategy(setOf("huawei")),
        Brand.Xiaomi to SimpleBrandStrategy(setOf("xiaomi", "redmi")),
        Brand.OPPO to SimpleBrandStrategy(setOf("oppo")),
        Brand.vivo to SimpleBrandStrategy(setOf("vivo")),
        Brand.SAMSUNG to SimpleBrandStrategy(setOf("samsung")),
        Brand.HONOR to SimpleBrandStrategy(setOf("honor")),
        Brand.MEIZU to SimpleBrandStrategy(setOf("meizu")),
        Brand.ASUS to SimpleBrandStrategy(setOf("asus")),
        Brand.Google to SimpleBrandStrategy(setOf("google")),
        Brand.MOTOROLA to SimpleBrandStrategy(setOf("motorola")),
        Brand.NOKIA to SimpleBrandStrategy(setOf("nokia"))
    )

    override fun detectBrand(): Brand {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val brandName = Build.BRAND.orEmpty().lowercase()
        val model = Build.MODEL.orEmpty().lowercase()
        val product = Build.PRODUCT.orEmpty().lowercase()

        AppLog.debug(
            "BrandDetectorImpl",
            "detect: manufacturer=$manufacturer, brand=$brandName, model=$model, product=$product"
        )

        return strategies.firstOrNull { (_, strategy) ->
            strategy.matches(manufacturer, brandName, model, product)
        }?.first ?: Brand.UNKNOWN
    }
}

/**
 * 简单品牌检测策略
 */
private class SimpleBrandStrategy(
    private val keywords: Set<String>
) : BrandDetectionStrategy {
    override fun matches(
        manufacturer: String,
        brand: String,
        model: String,
        product: String
    ): Boolean {
        return keywords.any { keyword ->
            keyword in manufacturer || keyword in brand
        }
    }
}

/**
 * Redmi品牌检测策略
 */
private class RedmiStrategy : BrandDetectionStrategy {
    override fun matches(
        manufacturer: String,
        brand: String,
        model: String,
        product: String
    ): Boolean {
        return "redmi" in manufacturer ||
                "redmi" in brand ||
                model.startsWith("redmi") ||
                product.startsWith("redmi")
    }
}

/**
 * POCO品牌检测策略
 */
private class PocoStrategy : BrandDetectionStrategy {
    override fun matches(
        manufacturer: String,
        brand: String,
        model: String,
        product: String
    ): Boolean {
        return "poco" in manufacturer ||
                "poco" in brand ||
                model.startsWith("poco") ||
                product.startsWith("poco")
    }
}

/**
 * iQOO品牌检测策略
 */
private class IqooStrategy : BrandDetectionStrategy {
    override fun matches(
        manufacturer: String,
        brand: String,
        model: String,
        product: String
    ): Boolean {
        return SystemPropertyCache.getProperty("ro.vivo.product.series").lowercase() == "iqoo" ||
                "iqoo" in manufacturer ||
                "iqoo" in brand ||
                model.startsWith("iq") ||
                product.startsWith("iq") ||
                model.contains("iqoo") ||
                product.contains("iqoo")
    }
}

/**
 * OnePlus品牌检测策略
 */
private class OnePlusStrategy : BrandDetectionStrategy {
    override fun matches(
        manufacturer: String,
        brand: String,
        model: String,
        product: String
    ): Boolean {
        return "oneplus" in manufacturer ||
                "oneplus" in brand ||
                model.startsWith("oneplus") ||
                product.startsWith("oneplus") ||
                model.startsWith("op") ||
                product.startsWith("op")
    }
}

/**
 * realme品牌检测策略
 */
private class RealmeStrategy : BrandDetectionStrategy {
    override fun matches(
        manufacturer: String,
        brand: String,
        model: String,
        product: String
    ): Boolean {
        return "realme" in manufacturer ||
                "realme" in brand ||
                model.startsWith("realme") ||
                product.startsWith("realme") ||
                model.startsWith("rmx") ||
                product.startsWith("rmx")
    }
}
