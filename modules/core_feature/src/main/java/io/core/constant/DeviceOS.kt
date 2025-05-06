package io.core.constant

import android.annotation.SuppressLint
import android.os.Build
import io.core.common.util.extensions.cool.removeWhitespace
import io.core.common.util.log.LogPure
import io.core.common.util.tools.StringTools

/**
 * 设备信息检测工具类
 * 功能：
 * 1. 检测设备品牌（华米OV等）
 * 2. 检测系统Rom类型（MIUI、EMUI、ColorOS等）
 * 3. 获取系统版本信息
 */
object DeviceOS {

    // ========== 枚举定义 ==========

    /** 设备品牌枚举 */
    enum class Brand {
        // 主品牌
        HUAWEI, Xiaomi, OPPO, vivo, SAMSUNG,
        SONY, LENOVO, ZTE, NUBIA, HONOR, ASUS,
        Google, MOTOROLA, NOKIA, MEIZU,

        // 子品牌
        REDMI, POCO, IQOO, OnePlus, realme,

        UNKNOWN
    }

    /** 系统Rom类型枚举 */
    /** 带<$>表示已完成验证 */
    enum class Rom {
        MIUI,         // 小米MIUI (Deprecation)
        HyperOS,     // 小米澎湃OS <$>
        EMUI,         // 华为EMUI (Deprecation)
        HarmonyOS,   // 华为鸿蒙OS <$>
        ColorOS,     // OPPO ColorOS <$>
        FuntouchOS,  // vivo FuntouchOS (Deprecation)
        OriginOS,   // vivo原OS <$>
        Flyme,        // 魅族Flyme
        H2OS,         // 一加H2OS (Deprecation)
        realme_UI,    // realme UI <$>
        ONE_UI,       // 三星One UI <$>
        XPERIA_UI,    // 索尼Xperia UI
        ZUI,          // 联想ZUI
        MYOS,         // 中兴MyOS
        NUBIA_UI,     // 努比亚UI
        MAGIC_UI,     // 荣耀Magic UI (Deprecation)
        MagicOS,   // 荣耀MagicOS <$>
        ROG_UI,       // 华硕ROG UI
        ANDROID      // 安卓原生（Google Pixel UI）
    }

    /** 系统UI信息数据类（包含类型和版本信息） */
    data class SystemRomInfo(
        val type: Rom,
        val verName: String,
        val verCode: String,
        val verDesc: String = "" // HyperOS 2.0
    )

    // ========== 公共API ==========

    @JvmStatic
    val brand: Brand by lazy { BrandDetector.detect() }

    @JvmStatic
    val romInfo: SystemRomInfo by lazy {
        RomDetectorFactory.createDetector(brand).detect()
    }

    @JvmStatic
    val romName: Rom get() = romInfo.type

    @JvmStatic
    val marketName: String by lazy {
        RomDetectorFactory.createDetector(brand).marketName
    }

    // 快捷访问属性
    @JvmStatic
    val isHuawei: Boolean get() = brand == Brand.HUAWEI

    @JvmStatic
    val isXiaomi: Boolean get() = brand == Brand.Xiaomi

    @JvmStatic
    val isMIUI: Boolean get() = romName == Rom.MIUI

    @JvmStatic
    val isHyperOS: Boolean get() = romName == Rom.HyperOS

    @JvmStatic
    val isHarmonyOS: Boolean get() = romName == Rom.HarmonyOS

    @JvmStatic
    val isColorOS: Boolean get() = romName == Rom.ColorOS

    // ========== 品牌检测核心 ==========

    private object BrandDetector {
        // 所有品牌检测策略列表
        private val strategies: List<Pair<Brand, BrandDetectionStrategy>> = listOf(
            // Sub-brands first
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

        /** 执行品牌检测 */
        fun detect(): Brand {
            val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
            val brandName = Build.BRAND.orEmpty().lowercase()
            val model = Build.MODEL.orEmpty().lowercase()
            val product = Build.PRODUCT.orEmpty().lowercase()

            LogPure.d("DeviceOS") {
                "detect: manufacturer=$manufacturer, brand=$brandName, model=$model, product=$product"
            }
            return strategies.firstOrNull { (_, strategy) ->
                strategy.matches(manufacturer, brandName, model, product)
            }?.first ?: Brand.UNKNOWN
        }
    }

    // ========== 策略接口 ==========
    private interface BrandDetectionStrategy {
        fun matches(
            manufacturer: String,
            brand: String,
            model: String = "",
            product: String = ""
        ): Boolean
    }

    private interface RomDetectionStrategy {
        val marketName: String
        fun detect(): SystemRomInfo
    }

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

    // ===== 特殊品牌检测策略实现 =====
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

    private class IqooStrategy : BrandDetectionStrategy {
        override fun matches(
            manufacturer: String,
            brand: String,
            model: String,
            product: String
        ): Boolean {
            return getSystemProperty(VivoRomDetector.VivoProps.VIVO_SERIES).lowercase() == "iqoo" ||
                    "iqoo" in manufacturer ||
                    "iqoo" in brand ||
                    model.startsWith("iq") || // iQOO models like IQOO 9
                    product.startsWith("iq") ||
                    model.contains("iqoo") ||
                    product.contains("iqoo")
        }
    }

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
                    model.startsWith("op") || // OnePlus model abbreviations
                    product.startsWith("op")
        }
    }

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
                    model.startsWith("rmx") || // Realme model numbers
                    product.startsWith("rmx")
        }
    }

    // ========== Rom检测核心 ==========

    /** Rom检测器工厂（根据品牌创建对应检测器） */
    private object RomDetectorFactory {
        private val detectors: Map<Brand, RomDetectionStrategy> = mapOf(
            Brand.HUAWEI to HuaweiRomDetector(),
            Brand.Xiaomi to XiaomiRomDetector(),
            Brand.REDMI to XiaomiRomDetector(),
            Brand.POCO to XiaomiRomDetector(),
            Brand.OPPO to OppoRomDetector(),
            Brand.realme to RealmeRomDetector(),
            Brand.OnePlus to OnePlusRomDetector(),
            Brand.vivo to VivoRomDetector(),
            Brand.IQOO to IQOORomDetector(),
            Brand.SAMSUNG to SamsungRomDetector(),
            Brand.MEIZU to MeizuRomDetector(),
            Brand.HONOR to HonorRomDetector()
            // ...其他品牌Rom检测器
        )

        fun createDetector(brand: Brand): RomDetectionStrategy {
            return detectors[brand] ?: DefaultAndroidUIDetector()
        }
    }

    // ===== 具体Rom检测策略实现 =====

    /** 华为Rom检测（支持鸿蒙和EMUI） */
    private class HuaweiRomDetector : RomDetectionStrategy {

        // 华为专属系统属性常量
        object HuaweiProps {
            const val EMUI_VERSION = "ro.build.version.emui"
            const val EMUI_VERSION_CODE = "ro.build.version.emui.code"
            const val HARMONY_DISPLAY_ID = "ro.huawei.build.display.id"
            const val HARMONY_PLATFORM_VER = "hw_sc.build.platform.version"
            const val OEM_NAME = "ro.hw.oemName"
            const val MARKET_NAME = "ro.product.name"

            /** 华为市场名称映射 */
            val huaweiMarketNameMap: HashMap<String, String> = hashMapOf(
                // Mate系列
                "ALP-AL00" to "HUAWEI Mate 10",
                "BLA-AL00" to "HUAWEI Mate 10 Pro",
                "LYA-AL00" to "HUAWEI Mate 20 Pro",
                "EVR-AN00" to "HUAWEI Mate 20 X 5G",
                "TAH-AN00" to "HUAWEI Mate X",
                "TAS-AN00" to "HUAWEI Mate 30 5G",
                "TAS-AL00" to "HUAWEI Mate 30",
                "LIO-AN00" to "HUAWEI Mate 30 Pro 5G",
                "OCE-AN00" to "HUAWEI Mate 40",
                "NOH-AN00" to "HUAWEI Mate 40 Pro",
                "NOP-AN00" to "HUAWEI Mate 40 Pro+",
                "TET-AN00" to "HUAWEI Mate X2",
                "PAL-AL00" to "HUAWEI Mate Xs 2",
                "ALT-AL00" to "HUAWEI Mate X3",
                "ALN-AL00" to "HUAWEI Mate 60 Pro",
                "BRA-AL00" to "HUAWEI Mate 60",
                "ALN-AL10" to "HUAWEI Mate 60 Pro+",
                "CLS-AL00" to "HUAWEI Mate 70",
                "PLR-AL00" to "HUAWEI Mate 70 Pro",
                "PLA-AL10" to "HUAWEI Mate 70 Pro+",

                // P/Pura系列
                "ELS-AN00" to "HUAWEI P40 Pro",
                "BAL-AL00" to "HUAWEI P50 Pocket",
                "JAD-AL00" to "HUAWEI P50 Pro",
                "LNA-AL00" to "HUAWEI P60",
                "MNA-AL00" to "HUAWEI P60 Pro",
                "ADY-AL00" to "HUAWEI Pura 70",
                "HBN-AL00" to "HUAWEI Pura 70 Pro",
                "HBN-AL10" to "HUAWEI Pura 70 Pro+",
                "HBP-AL00" to "HUAWEI Pura 70 Ultra",

                // Pocket折叠屏系列
                "BAL-AL60" to "HUAWEI Pocket S",
                "LEM-AL00" to "HUAWEI Pocket 2",

                // Nova系列
                "WAS-AL00" to "HUAWEI Nova 青春版",
                "PAR-AL00" to "HUAWEI Nova 3",
                "SEA-AL00" to "HUAWEI Nova 5",
                "WLZ-AL10" to "HUAWEI Nova 6",
                "NAM-AL00" to "HUAWEI Nova 9",
                "NOC-AL00" to "HUAWEI Nova 10",
                "FOA-AL00" to "HUAWEI Nova 11",
                "PSD-AL00" to "HUAWEI Nova 12",

                // 麦芒系列
                "RNE-AL00" to "HUAWEI 麦芒6",
                "SNE-AL00" to "HUAWEI 麦芒7",
                "POT-AL00" to "HUAWEI 麦芒8",

                // 特殊型号
                "CRR-UL00" to "HUAWEI Mate S",
                "MHA-AL00" to "HUAWEI Mate 9"
            )
        }

        override val marketName: String
            get() = HuaweiProps.huaweiMarketNameMap[getSystemProperty(HuaweiProps.MARKET_NAME)]
                ?: "${brand.name} ${getSystemProperty(HuaweiProps.MARKET_NAME)}"

        override fun detect(): SystemRomInfo {
            return when {
                isHarmonyOS() -> {
                    val versionName =
                        getSystemProperty(HuaweiProps.HARMONY_DISPLAY_ID, "HarmonyOS")
                            .replace(getSystemProperty(HuaweiProps.OEM_NAME), "")
                            .removeWhitespace()
                    val versionCode = getSystemProperty(
                        HuaweiProps.HARMONY_PLATFORM_VER,
                        Build.VERSION.INCREMENTAL
                    )
                    SystemRomInfo(
                        Rom.HarmonyOS,
                        versionName,
                        versionCode,
                        "${Rom.HarmonyOS.name} $versionCode"
                    )
                }

                hasSystemProperty(HuaweiProps.EMUI_VERSION) -> SystemRomInfo(
                    Rom.EMUI,
                    getSystemProperty(HuaweiProps.EMUI_VERSION),
                    getSystemProperty(HuaweiProps.EMUI_VERSION_CODE, Build.VERSION.INCREMENTAL)
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

    /** 小米Rom检测（支持HyperOS和MIUI） */
    private class XiaomiRomDetector : RomDetectionStrategy {

        // 小米专属系统属性常量
        object XiaomiProps {
            const val MIUI_VERSION_NAME = "ro.miui.ui.version.name"
            const val MIUI_VERSION_CODE = "ro.miui.ui.version.code"
            const val MIUI_VERSION_NAME_INC = "ro.odm.build.version.incremental"
            const val HYPER_VERSION_NAME = "ro.mi.os.version.name"
            const val HYPER_VERSION_INC = "ro.mi.os.version.incremental"
            const val HYPER_VERSION_CODE = "ro.mi.os.version.code"
            const val MARKET_NAME = "ro.product.marketname"
        }

        override val marketName: String
            get() = getSystemProperty(XiaomiProps.MARKET_NAME)

        override fun detect(): SystemRomInfo {
            val versionName = getSystemProperty(XiaomiProps.MIUI_VERSION_NAME, "")
            return when {
                // 检测澎湃OS（V8+版本）
                versionName.startsWith("V8") || hasSystemProperty(XiaomiProps.HYPER_VERSION_NAME) -> {
                    val property =
                        getSystemProperty(XiaomiProps.HYPER_VERSION_NAME, versionName)
                    val verCode = StringTools.extractNumber(property)
                    SystemRomInfo(
                        Rom.HyperOS,
                        getSystemProperty(
                            XiaomiProps.HYPER_VERSION_INC,
                            property
                        ),
                        verCode,
                        "HyperOS $verCode"
                    )
                }

                hasSystemProperty(XiaomiProps.MIUI_VERSION_NAME) -> {
                    val verCode = getSystemProperty(XiaomiProps.MIUI_VERSION_CODE)
                    SystemRomInfo(
                        Rom.MIUI,
                        getSystemProperty(XiaomiProps.MIUI_VERSION_NAME_INC),
                        verCode,
                        "MIUI $verCode"
                    )
                }

                else -> defaultAndroidInfo()
            }
        }
    }

    /** OPPO ColorOS检测 */
    private class OppoRomDetector : RomDetectionStrategy {

        object OppoProps {
            const val OPPO_VERSION_CODE = "ro.build.version.oplusrom"
            const val OPPO_VERSION_NAME = "ro.build.display.id"
            const val OPPO_BRAND = "ro.oplus.image.system_ext.brand"
            const val MARKET_NAME = "ro.vendor.oplus.market.name"
        }

        override val marketName: String
            get() = getSystemProperty(OppoProps.MARKET_NAME)

        override fun detect(): SystemRomInfo {
            return if (hasSystemProperty(OppoProps.OPPO_BRAND)) {
                val property =
                    getSystemProperty(OppoProps.OPPO_VERSION_CODE)
                SystemRomInfo(
                    Rom.ColorOS,
                    getSystemProperty(OppoProps.OPPO_VERSION_NAME),
                    property,
                    "ColorOS ${StringTools.extractNumber(property)}"
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** vivo Rom检测 */
    private class VivoRomDetector : RomDetectionStrategy {

        object VivoProps {
            const val VIVO_SERIES = "ro.vivo.product.series"
            const val VIVO_VERSION = "ro.vivo.os.version"
            const val VIVO_ORIGIN_OS = "ro.vivo.os.build.display.id"
            const val VIVO_VERSION_CODE_INC = "ro.vivo.product.version.incremental"
            const val VIVO_VERSION_CODE = "ro.vendor.vivo.product.version"
            const val MARKET_NAME = "ro.vivo.market.name"
        }

        override val marketName: String
            get() = getSystemProperty(VivoProps.MARKET_NAME)

        override fun detect(): SystemRomInfo {
            val isOriginOS = hasSystemProperty(VivoProps.VIVO_ORIGIN_OS)
            val isFuntouchOS = hasSystemProperty(VivoProps.VIVO_VERSION)

            return when {
                brand == Brand.vivo && isOriginOS -> SystemRomInfo(
                    Rom.OriginOS,
                    getSystemProperty(VivoProps.VIVO_VERSION_CODE_INC),
                    getSystemProperty(VivoProps.VIVO_VERSION, Build.VERSION.INCREMENTAL),
                    getSystemProperty(VivoProps.VIVO_ORIGIN_OS)
                )

                brand == Brand.vivo && isFuntouchOS -> SystemRomInfo(
                    Rom.FuntouchOS,
                    getSystemProperty(VivoProps.VIVO_VERSION_CODE),
                    getSystemProperty(VivoProps.VIVO_VERSION, Build.VERSION.INCREMENTAL)
                )

                else -> defaultAndroidInfo()
            }
        }

    }

    /** 三星Rom检测 */
    private class SamsungRomDetector : RomDetectionStrategy {

        object SamsungProps {
            const val ONEUI_VERSION = "ro.build.version.oneui"
            const val ONEUI_GSM_NAME = "gsm.version.baseband"
        }

        override val marketName: String
            get() = ""

        override fun detect(): SystemRomInfo {
            return if (hasSystemProperty(SamsungProps.ONEUI_VERSION)) {
                val version =
                    getSystemProperty(SamsungProps.ONEUI_VERSION, Build.VERSION.INCREMENTAL)
                SystemRomInfo(
                    Rom.ONE_UI,
                    getSystemProperty(SamsungProps.ONEUI_GSM_NAME),
                    version,
                    "One UI $version"
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** 荣耀Rom检测 */
    private class HonorRomDetector : RomDetectionStrategy {

        object HonorProps {
            const val HONOR_MAGIC_VERSION = "ro.magic.systemversion"
            const val HONOR_MAGIC_VERSION_NAME = "mscw.hnouc.patch.display.version"
            const val HONOR_MAGIC_VERSION_CODE = "msc.config.magic.version"
        }

        override val marketName: String
            get() = ""

        override fun detect(): SystemRomInfo {
            return if (hasSystemProperty("ro.horizon.version")) {
                SystemRomInfo(
                    Rom.MAGIC_UI,
                    getSystemProperty("ro.horizon.version"),
                    getSystemProperty("ro.horizon.version.code", Build.VERSION.INCREMENTAL)
                )
            }
            // 检测MagicOS
            else if (hasSystemProperty(HonorProps.HONOR_MAGIC_VERSION)) {
                SystemRomInfo(
                    Rom.MagicOS,
                    getSystemProperty(HonorProps.HONOR_MAGIC_VERSION_NAME),
                    getSystemProperty(
                        HonorProps.HONOR_MAGIC_VERSION_CODE,
                        Build.VERSION.INCREMENTAL
                    )
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** 魅族Rom检测 */
    private class MeizuRomDetector : RomDetectionStrategy {

        override val marketName: String
            get() = ""

        override fun detect(): SystemRomInfo {
            return if (hasSystemProperty("ro.build.flyme.version")) {
                SystemRomInfo(
                    Rom.Flyme,
                    getSystemProperty("ro.build.flyme.version"),
                    getSystemProperty("ro.build.display.id", Build.DISPLAY)
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** 一加Rom检测 */
    private class OnePlusRomDetector : RomDetectionStrategy {

        object OnePlusProps {
            const val ONEPLUS_VERSION = "ro.build.version.oplusrom"
            const val ONEPLUS_VERSION_DISPLAY = "ro.build.version.oplusrom.display"
            const val ONEPLUS_OTA_DISPLAY = "persist.sys.oplus.ota_ver_display"
            const val MARKET_NAME_CN = "ro.vendor.oplus.market.name"
            const val MARKET_NAME_EN = "ro.vendor.oplus.market.enname"
        }

        override val marketName: String
            get() = getSystemProperty(OnePlusProps.MARKET_NAME_EN)

        override fun detect(): SystemRomInfo {
            return if (hasSystemProperty(OnePlusProps.ONEPLUS_VERSION_DISPLAY)) {
                val property = getSystemProperty(OnePlusProps.ONEPLUS_VERSION)
                SystemRomInfo(
                    Rom.ColorOS,
                    getSystemProperty(OnePlusProps.ONEPLUS_OTA_DISPLAY),
                    getSystemProperty(property, Build.VERSION.INCREMENTAL),
                    "ColorOS $property"
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** 真我 Rom检测 */
    private class RealmeRomDetector : RomDetectionStrategy {

        object RealmeProps {
            const val REALME_VERSION = "ro.build.version.realmeui"
            const val REALME_VERSION_NAME = "ro.build.display.id"
            const val MARKET_NAME_CN = "ro.vendor.oplus.market.name"
            const val MARKET_NAME_EN = "ro.vendor.oplus.market.enname"
        }

        override val marketName: String
            get() = getSystemProperty(RealmeProps.MARKET_NAME_EN)

        override fun detect(): SystemRomInfo {
            return if (hasSystemProperty(RealmeProps.REALME_VERSION)) {
                val property = getSystemProperty(RealmeProps.REALME_VERSION)
                SystemRomInfo(
                    Rom.realme_UI,
                    getSystemProperty(RealmeProps.REALME_VERSION_NAME),
                    getSystemProperty(RealmeProps.REALME_VERSION, Build.VERSION.INCREMENTAL),
                    "realme UI ${StringTools.extractNumber(property)}"
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** IQOO Rom检测 */
    private class IQOORomDetector : RomDetectionStrategy {

        object IQOOProps {
            const val IQOO_VERSION = "ro.vivo.os.version"
            const val IQOO_ORIGIN_OS = "ro.vivo.os.build.display.id"
            const val IQOO_VERSION_CODE_INC = "ro.vivo.product.version.incremental"
            const val MARKET_NAME = "ro.vivo.market.name"
        }

        override val marketName: String
            get() = getSystemProperty(IQOOProps.MARKET_NAME)

        override fun detect(): SystemRomInfo {
            return if (hasSystemProperty(IQOOProps.IQOO_ORIGIN_OS)) {
                SystemRomInfo(
                    Rom.OriginOS,
                    getSystemProperty(IQOOProps.IQOO_VERSION_CODE_INC),
                    getSystemProperty(IQOOProps.IQOO_VERSION, Build.VERSION.INCREMENTAL),
                    getSystemProperty(IQOOProps.IQOO_ORIGIN_OS)
                )
            } else {
                defaultAndroidInfo()
            }
        }
    }

    /** 默认Android UI检测器 */
    private class DefaultAndroidUIDetector : RomDetectionStrategy {

        override val marketName: String
            get() = "Android Phone"

        override fun detect() = defaultAndroidInfo()
    }

    // ========== 工具方法 ==========

    /** 获取默认Android信息 */
    private fun defaultAndroidInfo() = SystemRomInfo(
        Rom.ANDROID,
        Build.VERSION.RELEASE,      // 系统版本（如13）
        Build.VERSION.INCREMENTAL,   // 内部版本号
        "Android ${Build.VERSION.RELEASE} (${Build.ID})" // 默认以Google Pixel UI版本号
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