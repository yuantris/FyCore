package io.core.constant

import android.annotation.SuppressLint
import android.os.Build
import io.core.common.util.extensions.cool.removeWhitespace

/**
 * 设备信息检测工具类
 * 功能：
 * 1. 检测设备品牌（华米OV等）
 * 2. 检测系统UI类型（MIUI、EMUI、ColorOS等）
 * 3. 获取系统版本信息
 */
object DeviceOS {

    // ========== 枚举定义 ==========

    /** 设备品牌枚举 */
    enum class Brand {
        HUAWEI, Xiaomi, OPPO, vivo, SAMSUNG, OnePlus, realme,
        SONY, LENOVO, ZTE, NUBIA, HONOR, ASUS, MEIZU,
        Google, MOTOROLA, NOKIA, UNKNOWN
    }

    /** 系统UI类型枚举 */
    /** 带<$>表示已完成验证 */
    enum class SystemUIType {
        EMUI,         // 华为EMUI
        HarmonyOS,   // 华为鸿蒙OS <$>
        MIUI,         // 小米MIUI
        HyperOS,     // 小米澎湃OS <$>
        ColorOS,     // OPPO ColorOS <$>
        FuntouchOS,  // vivo FuntouchOS
        OriginOS,   // vivo原OS <$>
        FLYME,        // 魅族Flyme
        H2OS,         // 一加H2OS
        REALME_UI,    // realme UI
        ONE_UI,       // 三星One UI
        XPERIA_UI,    // 索尼Xperia UI
        ZUI,          // 联想ZUI
        MYOS,         // 中兴MyOS
        NUBIA_UI,     // 努比亚UI
        MAGIC_UI,     // 荣耀Magic UI
        MagicOS,   // 荣耀MagicOS <$>
        ROG_UI,       // 华硕ROG UI
        PIXEL_UI,     // 谷歌Pixel UI
        STOCK_ANDROID,// 原生Android
        ANDROID      // 其他安卓
    }

    /** 系统UI信息数据类（包含类型和版本信息） */
    data class SystemUIInfo(
        val type: SystemUIType,
        val versionName: String,
        val versionCode: String
    )

    // ========== 公共API ==========

    @JvmStatic
    val brand: Brand by lazy { BrandDetector.detect() }

    @JvmStatic
    val systemUIInfo: SystemUIInfo by lazy {
        UIDetectorFactory.createDetector(brand).detect()
    }

    @JvmStatic
    val osName: SystemUIType get() = systemUIInfo.type

    // 快捷访问属性
    @JvmStatic
    val isHuawei: Boolean get() = brand == Brand.HUAWEI

    @JvmStatic
    val isXiaomi: Boolean get() = brand == Brand.Xiaomi

    @JvmStatic
    val isMIUI: Boolean get() = osName == SystemUIType.MIUI

    @JvmStatic
    val isHarmonyOS: Boolean get() = osName == SystemUIType.HarmonyOS

    @JvmStatic
    val isHyperOS: Boolean get() = osName == SystemUIType.HyperOS

    @JvmStatic
    val isColorOS: Boolean get() = osName == SystemUIType.ColorOS

    @JvmStatic
    val isFlyme: Boolean get() = osName == SystemUIType.FLYME

    // ========== 品牌检测核心 ==========

    private object BrandDetector {
        // 所有品牌检测策略列表
        private val strategies: Map<Brand, BrandDetectionStrategy> = mapOf(
            Brand.HUAWEI to SimpleBrandStrategy(setOf("huawei")),
            Brand.Xiaomi to SimpleBrandStrategy(setOf("xiaomi", "redmi")),
            // Note: Realme has separate detection
            Brand.OPPO to SimpleBrandStrategy(setOf("oppo", "realme")),
            Brand.vivo to SimpleBrandStrategy(setOf("vivo", "iqoo")),
            Brand.MEIZU to SimpleBrandStrategy(setOf("meizu")),
            Brand.OnePlus to SimpleBrandStrategy(setOf("oneplus")),
            Brand.realme to RealmeStrategy(),
            Brand.SAMSUNG to SimpleBrandStrategy(setOf("samsung")),
            Brand.HONOR to HonorStrategy()
            // ...其他品牌策略
        )

        /** 执行品牌检测 */
        fun detect(): Brand {
            val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
            val brand = Build.BRAND.orEmpty().lowercase()

            return strategies.entries.firstOrNull { (_, strategy) ->
                strategy.matches(manufacturer, brand)
            }?.key ?: Brand.UNKNOWN
        }
    }

    // ========== 策略接口 ==========
    private interface BrandDetectionStrategy {
        fun matches(manufacturer: String, brand: String): Boolean
    }

    private interface UIDetectionStrategy {
        fun detect(): SystemUIInfo
    }

    private class SimpleBrandStrategy(
        private val keywords: Set<String>
    ) : BrandDetectionStrategy {
        override fun matches(manufacturer: String, brand: String): Boolean {
            return keywords.any { it in manufacturer || it in brand }
        }
    }

    // ===== 特殊品牌检测策略实现 =====
    private class RealmeStrategy : BrandDetectionStrategy {
        override fun matches(manufacturer: String, brand: String): Boolean {
            return "realme" in manufacturer || "realme" in brand ||
                    (Build.DEVICE?.lowercase()?.contains("realme") == true)
        }
    }

    private class HonorStrategy : BrandDetectionStrategy {
        override fun matches(manufacturer: String, brand: String): Boolean {
            return "honor" in manufacturer || "honor" in brand ||
                    (Build.DEVICE?.lowercase()?.startsWith("hw") == true)
        }
    }

    // ========== UI检测核心 ==========

    /** UI检测器工厂（根据品牌创建对应检测器） */
    private object UIDetectorFactory {
        private val detectors: Map<Brand, UIDetectionStrategy> = mapOf(
            Brand.HUAWEI to HuaweiUIDetector(),
            Brand.Xiaomi to XiaomiUIDetector(),
            Brand.OPPO to OppoUIDetector(),
            Brand.vivo to VivoUIDetector(),
            Brand.MEIZU to MeizuUIDetector(),
            Brand.OnePlus to OnePlusUIDetector(),
            Brand.realme to RealmeUIDetector(),
            Brand.SAMSUNG to SamsungUIDetector(),
            Brand.HONOR to HonorUIDetector()
            // ...其他品牌UI检测器
        )

        fun createDetector(brand: Brand): UIDetectionStrategy {
            return detectors[brand] ?: DefaultAndroidUIDetector()
        }
    }

    // ===== 具体UI检测策略实现 =====

    /** 华为UI检测（支持鸿蒙和EMUI） */
    private class HuaweiUIDetector : UIDetectionStrategy {
        override fun detect(): SystemUIInfo {
            return when {
                isHarmonyOS() -> {
                    val versionName =
                        getSystemProperty("ro.huawei.build.display.id", "HarmonyOS")
                            .replace(getSystemProperty("ro.hw.oemName"), "")
                            .removeWhitespace()
                    SystemUIInfo(
                        SystemUIType.HarmonyOS,
                        versionName,
                        getSystemProperty("hw_sc.build.platform.version", Build.VERSION.INCREMENTAL)
                    )
                }

                hasSystemProperty("ro.build.version.emui") -> SystemUIInfo(
                    SystemUIType.EMUI,
                    getSystemProperty("ro.build.version.emui"),
                    getSystemProperty("ro.build.version.emui.code", Build.VERSION.INCREMENTAL)
                )
                // 默认回退
                else -> defaultAndroidInfo()
            }
        }

        /**
         * 校验是否是鸿蒙系统
         *
         * @return true-鸿蒙系统
         */
        private fun isHarmonyOS(): Boolean {
            return try {
                Class.forName("ohos.system.version.SystemVersion")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }

    /** 小米UI检测（支持HyperOS和MIUI） */
    private class XiaomiUIDetector : UIDetectionStrategy {
        override fun detect(): SystemUIInfo {
            val versionName = getSystemProperty("ro.miui.ui.version.name", "")
            return when {
                // 检测澎湃OS（V8+版本）
                versionName.startsWith("V8") || hasSystemProperty("ro.mi.os.version.name") ->
                    SystemUIInfo(
                        SystemUIType.HyperOS,
                        getSystemProperty("ro.mi.os.version.name", versionName),
                        getSystemProperty("ro.mi.os.version.code", Build.VERSION.INCREMENTAL)
                    )

                hasSystemProperty("ro.miui.ui.version.name") ->
                    SystemUIInfo(
                        SystemUIType.MIUI,
                        versionName,
                        getSystemProperty("ro.miui.ui.version.code", Build.VERSION.INCREMENTAL)
                    )

                else -> defaultAndroidInfo()
            }
        }
    }

    /** OPPO ColorOS检测 */
    private class OppoUIDetector : UIDetectionStrategy {
        override fun detect(): SystemUIInfo {
            return if (hasSystemProperty("ro.oplus.image.system_ext.brand")) {
                SystemUIInfo(
                    SystemUIType.ColorOS,
                    getSystemProperty("ro.build.version.oplusrom"),
                    getSystemProperty("ro.build.display.id", Build.VERSION.INCREMENTAL)
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** vivoUI检测 */
    private class VivoUIDetector : UIDetectionStrategy {
        override fun detect(): SystemUIInfo {
            val series = getSystemProperty("ro.vivo.product.series").lowercase()
            val isOriginOS = hasSystemProperty("ro.vivo.os.build.display.id")
            val isFuntouchOS = hasSystemProperty("ro.vivo.os.version")

            return when {
                series.contains("iqoo") && isOriginOS -> createSystemUIInfo(
                    SystemUIType.OriginOS,
                    "ro.vivo.product.version"
                )

                series.contains("vivo") && isOriginOS -> createSystemUIInfo(
                    SystemUIType.OriginOS,
                    "ro.vendor.vivo.product.version"
                )

                series.contains("vivo") && isFuntouchOS -> createSystemUIInfo(
                    SystemUIType.FuntouchOS,
                    "ro.vendor.vivo.product.version"
                )

                else -> defaultAndroidInfo()
            }
        }

        private fun createSystemUIInfo(type: SystemUIType, versionProperty: String): SystemUIInfo {
            return SystemUIInfo(
                type,
                getSystemProperty(versionProperty),
                getSystemProperty("ro.vivo.os.version", Build.VERSION.INCREMENTAL)
            )
        }
    }

    /** 三星UI检测 */
    private class SamsungUIDetector : UIDetectionStrategy {
        override fun detect(): SystemUIInfo {
            return if (hasSystemProperty("ro.build.scafe.version")) {
                SystemUIInfo(
                    SystemUIType.ONE_UI,
                    getSystemProperty("ro.build.scafe.version"),
                    getSystemProperty("ro.build.scafe.version.code", Build.VERSION.INCREMENTAL)
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** 荣耀UI检测 */
    private class HonorUIDetector : UIDetectionStrategy {
        override fun detect(): SystemUIInfo {
            return if (hasSystemProperty("ro.horizon.version")) {
                SystemUIInfo(
                    SystemUIType.MAGIC_UI,
                    getSystemProperty("ro.horizon.version"),
                    getSystemProperty("ro.horizon.version.code", Build.VERSION.INCREMENTAL)
                )
            }
            // 检测MagicOS
            else if (hasSystemProperty("ro.magic.systemversion")) {
                SystemUIInfo(
                    SystemUIType.MagicOS,
                    getSystemProperty("mscw.hnouc.patch.display.version"),
                    getSystemProperty("msc.config.magic.version", Build.VERSION.INCREMENTAL)
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** 魅族UI检测 */
    private class MeizuUIDetector : UIDetectionStrategy {
        override fun detect(): SystemUIInfo {
            return if (hasSystemProperty("ro.build.flyme.version")) {
                SystemUIInfo(
                    SystemUIType.FLYME,
                    getSystemProperty("ro.build.flyme.version"),
                    getSystemProperty("ro.build.display.id", Build.DISPLAY)
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** 一加UI检测 */
    private class OnePlusUIDetector : UIDetectionStrategy {
        override fun detect(): SystemUIInfo {
            return if (hasSystemProperty("ro.oneplus.version")) {
                SystemUIInfo(
                    SystemUIType.H2OS,
                    getSystemProperty("ro.oneplus.version"),
                    getSystemProperty("ro.oneplus.software.version", Build.VERSION.INCREMENTAL)
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** 真我 UI检测 */
    private class RealmeUIDetector : UIDetectionStrategy {
        override fun detect(): SystemUIInfo {
            return if (hasSystemProperty("ro.realme.version")) {
                SystemUIInfo(
                    SystemUIType.REALME_UI,
                    getSystemProperty("ro.realme.version"),
                    getSystemProperty("ro.realme.software.version", Build.VERSION.INCREMENTAL)
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** 默认Android UI检测器 */
    private class DefaultAndroidUIDetector : UIDetectionStrategy {
        override fun detect() = defaultAndroidInfo()
    }

    // ========== 工具方法 ==========

    /** 获取默认Android信息 */
    private fun defaultAndroidInfo() = SystemUIInfo(
        SystemUIType.ANDROID,
        Build.VERSION.RELEASE,      // 系统版本（如13）
        Build.VERSION.INCREMENTAL   // 内部版本号
    )

    /** 检查系统属性是否存在 */
    private fun hasSystemProperty(key: String): Boolean {
        return getSystemProperty(key).isNotEmpty()
    }

    /** 获取系统属性（通过反射调用SystemProperties） */
    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String, default: String = ""): String {
        return try {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, key) as? String ?: default
        } catch (e: Exception) {
            default
        }
    }
}