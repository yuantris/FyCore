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
    val rom: Rom get() = romInfo.type

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
    val isMIUI: Boolean get() = rom == Rom.MIUI

    @JvmStatic
    val isHyperOS: Boolean get() = rom == Rom.HyperOS

    @JvmStatic
    val isHarmonyOS: Boolean get() = rom == Rom.HarmonyOS

    @JvmStatic
    val isColorOS: Boolean get() = rom == Rom.ColorOS

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
            val huaweiMarketNameMap by lazy {
                hashMapOf(
                    // Mate 系列
                    "MT1-T00" to "华为 Ascend Mate 移动版",
                    "MT1-U06" to "华为 Ascend Mate 联通版",
                    "MT2-U071" to "华为 Ascend Mate 2 联通 3G 版",
                    "MT2-C00" to "华为 Ascend Mate 2 电信 3G 版",
                    "MT2-L02" to "华为 Ascend Mate 2 移动 4G 版",
                    "MT2-L05" to "华为 Ascend Mate 2 联通 4G 版",
                    "MT7-TL00" to "华为 Ascend Mate 7 移动版",
                    "MT7-TL10" to "华为 Ascend Mate 7 双 4G 版",
                    "MT7-UL00" to "华为 Ascend Mate 7 联通版",
                    "MT7-CL00" to "华为 Ascend Mate 7 电信版",
                    "CRR-TL00" to "HUAWEI Mate S 移动臻享版",
                    "CRR-UL00" to "HUAWEI Mate S 双 4G 臻享版",
                    "CRR-UL20" to "HUAWEI Mate S 双 4G 臻逸版",
                    "CRR-CL00" to "HUAWEI Mate S 电信臻享版",
                    "CRR-CL20" to "HUAWEI Mate S 电信臻逸版",
                    "NXT-AL10" to "HUAWEI Mate 8 全网通版",
                    "NXT-TL00" to "HUAWEI Mate 8 移动版",
                    "NXT-DL00" to "HUAWEI Mate 8 双 4G 版",
                    "NXT-CL00" to "HUAWEI Mate 8 电信版",
                    "MHA-AL00" to "HUAWEI Mate 9 全网通版",
                    "MHA-TL00" to "HUAWEI Mate 9 移动 4G+ 版",
                    "LON-AL00" to "HUAWEI Mate 9 Pro",
                    "LON-AL00" to "HUAWEI Mate 9 保时捷设计",
                    "ALP-AL00" to "HUAWEI Mate 10 全网通版",
                    "ALP-TL00" to "HUAWEI Mate 10 移动 4G+ 版",
                    "BLA-AL00" to "HUAWEI Mate 10 Pro 全网通版",
                    "BLA-TL00" to "HUAWEI Mate 10 Pro 移动 4G+ 版",
                    "BLA-AL00" to "HUAWEI Mate 10 保时捷设计",
                    "NEO-AL00" to "HUAWEI Mate RS 保时捷设计",
                    "HMA-AL00" to "HUAWEI Mate 20 全网通版",
                    "HMA-TL00" to "HUAWEI Mate 20 移动 4G+ 版",
                    "LYA-AL00" to "HUAWEI Mate 20 Pro 全网通版",
                    "LYA-AL10" to "HUAWEI Mate 20 Pro 全网通版 (8GB+256GB)",
                    "LYA-TL00" to "HUAWEI Mate 20 Pro 移动 4G+ 版",
                    "EVR-AL00" to "HUAWEI Mate 20 X 全网通版",
                    "EVR-TL00" to "HUAWEI Mate 20 X 移动 4G+ 版",
                    "EVR-AN00" to "HUAWEI Mate 20 X 5G",
                    "LYA-AL00P" to "HUAWEI Mate 20 RS 保时捷设计",
                    "TAS-AL00" to "HUAWEI Mate 30 全网通版",
                    "TAS-TL00" to "HUAWEI Mate 30 移动 4G+ 版",
                    "TAS-AN00" to "HUAWEI Mate 30 5G 全网通版",
                    "TAS-TN00" to "HUAWEI Mate 30 5G 移动版",
                    "LIO-AL00" to "HUAWEI Mate 30 Pro 全网通版",
                    "LIO-TL00" to "HUAWEI Mate 30 Pro 移动 4G+ 版",
                    "LIO-AN00" to "HUAWEI Mate 30 Pro 5G 全网通版",
                    "LIO-TN00" to "HUAWEI Mate 30 Pro 5G 移动版",
                    "LIO-AN00m" to "HUAWEI Mate 30E Pro 5G",
                    "LIO-AN00P" to "HUAWEI Mate 30 RS 保时捷设计",
                    "OCE-AN10" to "HUAWEI Mate 40 5G",
                    "OCE-AN50" to "HUAWEI Mate 40E 5G",
                    "OCE-AL50" to "HUAWEI Mate 40E 4G",
                    "NOH-AN00" to "HUAWEI Mate 40 Pro 5G",
                    "NOH-AN01" to "HUAWEI Mate 40 Pro 5G",
                    "NOH-AL00" to "HUAWEI Mate 40 Pro 4G",
                    "NOH-AL10" to "HUAWEI Mate 40 Pro 4G",
                    "NOH-AN50" to "HUAWEI Mate 40E Pro 5G",
                    "NOH-AN80" to "HUAWEI Mate 40E Pro 5G",
                    "NOP-AN00" to "HUAWEI Mate 40 Pro+ 5G",
                    "NOP-AN00" to "HUAWEI Mate 40 RS 保时捷设计",
                    "CET-AL00" to "HUAWEI Mate 50",
                    "CET-AL60" to "HUAWEI Mate 50E",
                    "DCO-AL00" to "HUAWEI Mate 50 Pro",
                    "DCO-AL00" to "HUAWEI Mate 50 RS 保时捷设计",
                    "BRA-AL00" to "HUAWEI Mate 60",
                    "ALN-AL00" to "HUAWEI Mate 60 Pro",
                    "ALN-AL80" to "HUAWEI Mate 60 Pro",
                    "ALN-AL10" to "HUAWEI Mate 60 Pro+",
                    "ALN-AL10" to "HUAWEI Mate 60 RS ULTIMATE DESIGN 非凡大师",
                    "CLS-AL00" to "HUAWEI Mate 70",
                    "CLS-AL30" to "HUAWEI Mate 70",
                    "PLR-AL00" to "HUAWEI Mate 70 Pro",
                    "PLR-AL30" to "HUAWEI Mate 70 Pro",
                    "PLR-AL50" to "HUAWEI Mate 70 Pro 优享版",
                    "PLA-AL10" to "HUAWEI Mate 70 Pro+",
                    "PLU-AL10" to "HUAWEI Mate 70 RS ULTIMATE DESIGN 非凡大师",
                    "TAH-AN00" to "HUAWEI Mate X",
                    "TAH-AN00m" to "HUAWEI Mate Xs",
                    "TET-AN00" to "HUAWEI Mate X2 5G",
                    "TET-AN10" to "HUAWEI Mate X2 5G",
                    "TET-AN50" to "HUAWEI Mate X2 典藏版 5G",
                    "TET-AL00" to "HUAWEI Mate X2 4G",
                    "PAL-AL00" to "HUAWEI Mate Xs 2",
                    "PAL-AL10" to "HUAWEI Mate Xs 2",
                    "ALT-AL00" to "HUAWEI Mate X3",
                    "ALT-AL10" to "HUAWEI Mate X5",
                    "GRL-AL10" to "HUAWEI Mate XT ULTIMATE DESIGN 非凡大师",
                    "ICL-AL10" to "HUAWEI Mate X6",
                    "ICL-AL20" to "HUAWEI Mate X6 典藏版",
                    // P / Pura 系列
                    "U9200" to "华为 Ascend P1",
                    "U9200E" to "华为 Ascend P1 XL",
                    "U9200S" to "华为 Ascend P1 S",
                    "P2-0000" to "华为 Ascend P2",
                    "P6-T00" to "华为 Ascend P6 移动渠道版",
                    "P6-T00V" to "华为 Ascend P6 移动定制版",
                    "P6-U06" to "华为 Ascend P6 联通版",
                    "P6-C00" to "华为 Ascend P6 电信版",
                    "P6 S-U06" to "华为 Ascend P6S 联通版",
                    "P7-L00" to "华为 Ascend P7 联通版",
                    "P7-L05" to "华为 Ascend P7 移动渠道版",
                    "P7-L07" to "华为 Ascend P7 移动定制版",
                    "P7-L09" to "华为 Ascend P7 电信版",
                    "GRA-TL00" to "华为 P8 移动标配版",
                    "GRA-UL00" to "华为 P8 双 4G 标配版",
                    "GRA-UL10" to "华为 P8 双 4G 高配版",
                    "GRA-CL00" to "华为 P8 电信标配版",
                    "GRA-CL10" to "华为 P8 电信高配版",
                    "ALE-TL00" to "华为 P8 青春版 移动版",
                    "ALE-UL00" to "华为 P8 青春版 双 4G 版",
                    "ALE-CL00" to "华为 P8 青春版 电信版",
                    "DAV-703L" to "华为 P8 Max",
                    "DAV-713L" to "华为 P8 Max",
                    "EVA-AL00" to "HUAWEI P9 全网通版 (32GB)",
                    "EVA-AL10" to "HUAWEI P9 全网通版 (64GB)",
                    "EVA-TL00" to "HUAWEI P9 移动版",
                    "EVA-DL00" to "HUAWEI P9 双 4G 版",
                    "EVA-CL00" to "HUAWEI P9 电信版",
                    "VIE-AL10" to "HUAWEI P9 Plus",
                    "VTR-AL00" to "HUAWEI P10 全网通版",
                    "VTR-TL00" to "HUAWEI P10 移动 4G+ 版",
                    "VKY-AL00" to "HUAWEI P10 Plus 全网通版",
                    "VKY-TL00" to "HUAWEI P10 Plus 移动 4G+ 版",
                    "EML-AL00" to "HUAWEI P20 全网通版",
                    "EML-TL00" to "HUAWEI P20 移动 4G+ 版",
                    "CLT-AL00" to "HUAWEI P20 Pro 全网通版",
                    "CLT-AL01" to "HUAWEI P20 Pro 全网通版 (6GB+64GB)",
                    "CLT-AL00l" to "HUAWEI P20 Pro 真皮限量版 全网通版",
                    "CLT-TL00" to "HUAWEI P20 Pro 移动 4G+ 版",
                    "CLT-TL01" to "HUAWEI P20 Pro 移动 4G+ 版 (6GB+64GB)",
                    "ELE-AL00" to "HUAWEI P30 全网通版",
                    "ELE-TL00" to "HUAWEI P30 移动 4G+ 版",
                    "VOG-AL00" to "HUAWEI P30 Pro 全网通版 (8GB+128GB)",
                    "VOG-AL10" to "HUAWEI P30 Pro 全网通版",
                    "VOG-TL00" to "HUAWEI P30 Pro 移动 4G+ 版",
                    "ANA-AL00" to "HUAWEI P40 4G 全网通版",
                    "ANA-AN00" to "HUAWEI P40 5G 全网通版",
                    "ANA-TN00" to "HUAWEI P40 5G 移动版",
                    "ELS-AN00" to "HUAWEI P40 Pro 5G 全网通版",
                    "ELS-TN00" to "HUAWEI P40 Pro 5G 移动版",
                    "ELS-AN10" to "HUAWEI P40 Pro+ 5G 全网通版",
                    "ELS-TN10" to "HUAWEI P40 Pro+ 5G 移动版",
                    "ABR-AL00" to "HUAWEI P50",
                    "ABR-AL80" to "HUAWEI P50",
                    "ABR-AL60" to "HUAWEI P50E",
                    "ABR-AL90" to "HUAWEI P50E",
                    "JAD-AL00" to "HUAWEI P50 Pro (骁龙 888)",
                    "JAD-AL50" to "HUAWEI P50 Pro (麒麟 9000)",
                    "JAD-AL10" to "HUAWEI P50 Pro (骁龙 888)",
                    "JAD-AL60" to "HUAWEI P50 Pro (麒麟 9000)",
                    "BAL-AL00" to "HUAWEI P50 Pocket",
                    "BAL-AL80" to "HUAWEI P50 Pocket",
                    "LNA-AL00" to "HUAWEI P60",
                    "MNA-AL00" to "HUAWEI P60 Pro",
                    "MNA-AL00" to "HUAWEI P60 Art",
                    "ADY-AL00" to "HUAWEI Pura 70",
                    "ADY-AL10" to "HUAWEI Pura 70 北斗卫星消息版",
                    "HBN-AL00" to "HUAWEI Pura 70 Pro",
                    "HBN-AL10" to "HUAWEI Pura 70 Pro+",
                    "HBN-AL80" to "HUAWEI Pura 70 Pro+",
                    "HBP-AL00" to "HUAWEI Pura 70 Ultra",
                    "VDE-AL00" to "HUAWEI Pura X",
                    "VDE-AL10" to "HUAWEI Pura X 典藏版",
                    // Pocket 系列
                    "BAL-AL60" to "HUAWEI Pocket S",
                    "LEM-AL00" to "HUAWEI Pocket 2",
                    // nova 系列
                    "CAZ-AL00" to "HUAWEI nova 全网通标配版",
                    "CAZ-AL10" to "HUAWEI nova 全网通高配版",
                    "CAZ-TL10" to "HUAWEI nova 移动定制标配版",
                    "CAZ-TL20" to "HUAWEI nova 移动定制高配版",
                    "WAS-AL00" to "HUAWEI nova 青春版 全网通版",
                    "WAS-TL10" to "HUAWEI nova 青春版 移动 4G+ 版",
                    "PIC-AL00" to "HUAWEI nova 2 全网通版",
                    "PIC-TL00" to "HUAWEI nova 2 移动 4G+ 版",
                    "BAC-AL00" to "HUAWEI nova 2 Plus 全网通版",
                    "BAC-TL00" to "HUAWEI nova 2 Plus 移动 4G+ 版",
                    "HWI-AL00" to "HUAWEI nova 2s 全网通版",
                    "HWI-TL00" to "HUAWEI nova 2s 移动 4G+ 版",
                    "ANE-AL00" to "HUAWEI nova 3e 全网通版",
                    "ANE-TL00" to "HUAWEI nova 3e 移动 4G+ 版",
                    "PAR-AL00" to "HUAWEI nova 3 全网通版",
                    "PAR-TL00" to "HUAWEI nova 3 移动 4G+ 版",
                    "INE-AL00" to "HUAWEI nova 3i 全网通版",
                    "INE-TL00" to "HUAWEI nova 3i 移动 4G+ 版",
                    "VCE-AL00" to "HUAWEI nova 4 全网通版",
                    "VCE-TL00" to "HUAWEI nova 4 移动 4G+ 版",
                    "MAR-AL00" to "HUAWEI nova 4e 全网通版",
                    "MAR-TL00" to "HUAWEI nova 4e 移动 4G+ 版",
                    "SEA-AL00" to "HUAWEI nova 5 全网通版",
                    "SEA-TL00" to "HUAWEI nova 5 移动 4G+ 版",
                    "SEA-AL10" to "HUAWEI nova 5 Pro 全网通版",
                    "SEA-TL10" to "HUAWEI nova 5 Pro 移动 4G+ 版",
                    "GLK-AL00" to "HUAWEI nova 5i 全网通版",
                    "GLK-TL00" to "HUAWEI nova 5i 移动 4G+ 版",
                    "GLK-LX1U" to "HUAWEI nova 5i 联通定制版",
                    "SPN-AL00" to "HUAWEI nova 5i Pro 全网通版",
                    "SPN-TL00" to "HUAWEI nova 5i Pro 移动 4G+ 版",
                    "SPN-AL00" to "HUAWEI nova 5z 全网通版",
                    "SPN-TL00" to "HUAWEI nova 5z 移动 4G+ 版",
                    "WLZ-AL10" to "HUAWEI nova 6 4G",
                    "WLZ-AN00" to "HUAWEI nova 6 5G",
                    "JNY-AL10" to "HUAWEI nova 6 SE 全网通版",
                    "JNY-TL10" to "HUAWEI nova 6 SE 移动版",
                    "JEF-AN00" to "HUAWEI nova 7 5G 全网通版",
                    "JEF-TN00" to "HUAWEI nova 7 5G 移动版",
                    "JER-AN10" to "HUAWEI nova 7 Pro 5G 全网通版",
                    "JER-TN10" to "HUAWEI nova 7 Pro 5G 移动版",
                    "CDY-AN00" to "HUAWEI nova 7 SE 5G 全网通版",
                    "CDY-TN00" to "HUAWEI nova 7 SE 5G 移动版",
                    "CND-AN00" to "HUAWEI nova 7 SE 5G 活力版",
                    "CDL-AN50" to "HUAWEI nova 7 SE 5G 乐活版",
                    "ANG-AN00" to "HUAWEI nova 8 5G",
                    "BRQ-AN00" to "HUAWEI nova 8 Pro 5G",
                    "BRQ-AL00" to "HUAWEI nova 8 Pro 4G",
                    "JSC-AN00" to "HUAWEI nova 8 SE 5G 全网通版",
                    "JSC-TN00" to "HUAWEI nova 8 SE 5G 移动版",
                    "JSC-AL50" to "HUAWEI nova 8 SE 4G 全网通版",
                    "CHL-AL60" to "HUAWEI nova 8 SE 活力版",
                    "NAM-AL00" to "HUAWEI nova 9",
                    "RTE-AL00" to "HUAWEI nova 9 Pro",
                    "JLN-AL00" to "HUAWEI nova 9 SE",
                    "NCO-AL00" to "HUAWEI nova 10",
                    "GLA-AL00" to "HUAWEI nova 10 Pro",
                    "CHA-AL80" to "HUAWEI nova 10z",
                    "BNE-AL00" to "HUAWEI nova 10 SE",
                    "JLN-AL00" to "HUAWEI nova 10 青春版",
                    "FOA-AL00" to "HUAWEI nova 11",
                    "GOA-AL80" to "HUAWEI nova 11 Pro",
                    "GOA-AL80U" to "HUAWEI nova 11 Ultra",
                    "BON-AL00" to "HUAWEI nova 11 SE",
                    "BLK-AL00" to "HUAWEI nova 12",
                    "FIN-AL60" to "HUAWEI nova 12 活力版",
                    "FIN-AL60a" to "HUAWEI nova 12 活力版",
                    "ADA-AL00" to "HUAWEI nova 12 Pro",
                    "ADA-AL00U" to "HUAWEI nova 12 Ultra",
                    "ADA-AL10U" to "HUAWEI nova 12 Ultra 星耀版",
                    "PSD-AL00" to "HUAWEI nova Flip",
                    "BLK-AL80" to "HUAWEI nova 13",
                    "MIS-AL00" to "HUAWEI nova 13 Pro",
                    // G 系列
                    "G6-T00" to "华为 Ascend G6 移动版",
                    "G6-U00" to "华为 Ascend G6 联通版",
                    "G6-C00" to "华为 Ascend G6 电信版",
                    "G7-TL00" to "华为 Ascend G7 移动版",
                    "G7-UL20" to "华为 Ascend G7 联通版",
                    "RIO-TL00" to "华为 G7 Plus 移动版",
                    "RIO-UL00" to "华为 G7 Plus 联通版",
                    "VNS-AL00" to "HUAWEI G9 青春版 全网通版",
                    "VNS-TL00" to "HUAWEI G9 青春版 移动版",
                    "VNS-DL00" to "HUAWEI G9 青春版 双 4G 版",
                    "VNS-CL00" to "HUAWEI G9 青春版 电信版",
                    "MLA-TL00" to "HUAWEI G9 Plus 移动 4G 版",
                    "MLA-TL10" to "HUAWEI G9 Plus 移动版",
                    "MLA-UL00" to "HUAWEI G9 Plus 双 4G 版",
                    // 麦芒系列
                    "A199" to "华为麦芒 A199",
                    "B199" to "华为麦芒 B199",
                    "C199" to "华为麦芒 C199",
                    "C199s" to "华为麦芒 3S",
                    "RIO-AL00" to "华为麦芒 4 全网通版",
                    "RIO-CL00" to "华为麦芒 4 电信 4G 版",
                    "MLA-AL00" to "HUAWEI 麦芒 5 全网通标配版",
                    "MLA-AL10" to "HUAWEI 麦芒 5 全网通高配版",
                    "RNE-AL00" to "HUAWEI 麦芒 6",
                    "SNE-AL00" to "HUAWEI 麦芒 7",
                    "POT-AL00" to "HUAWEI 麦芒 8",
                    "POT-AL10" to "HUAWEI 麦芒 8",
                    "TNN-AN00" to "华为麦芒 9 5G",
                    // 畅享系列
                    "TIT-AL00" to "华为畅享 5 全网通版",
                    "TIT-TL00" to "华为畅享 5 移动 4G 版",
                    "TIT-CL00" to "华为畅享 5 电信 4G 版",
                    "TIT-CL10" to "华为畅享 5 电信 4G 版",
                    "TAG-AL00" to "华为畅享 5S 全网通版",
                    "TAG-TL00" to "华为畅享 5S 移动 4G 版",
                    "TAG-CL00" to "华为畅享 5S 电信 4G 版",
                    "NCE-AL00" to "华为畅享 6 全网通版",
                    "NCE-AL10" to "华为畅享 6 全网通版",
                    "NCE-TL00" to "华为畅享 6 移动 4G+ 版",
                    "NCE-TL10" to "华为畅享 6 移动 4G+ 版",
                    "DIG-AL00" to "华为畅享 6S 全网通版",
                    "DIG-TL10" to "华为畅享 6S 移动 4G+ 版",
                    "TRT-AL00" to "华为畅享 7 Plus 全网通版",
                    "TRT-AL00A" to "华为畅享 7 Plus 全网通版",
                    "TRT-TL10" to "华为畅享 7 Plus 移动 4G+ 版",
                    "TRT-TL10A" to "华为畅享 7 Plus 移动 4G+ 版",
                    "SLA-AL00" to "华为畅享 7 全网通版",
                    "SLA-TL10" to "华为畅享 7 移动 4G+ 版",
                    "FIG-AL00" to "华为畅享 7S 全网通标配版",
                    "FIG-AL10" to "华为畅享 7S 全网通高配版",
                    "FIG-TL00" to "华为畅享 7S 移动 4G+ 标配版",
                    "FIG-TL10" to "华为畅享 7S 移动 4G+ 高配版",
                    "FLA-AL00" to "华为畅享 8 Plus 全网通版",
                    "FLA-AL10" to "华为畅享 8 Plus 全网通版",
                    "FLA-AL20" to "华为畅享 8 Plus 全网通版",
                    "FLA-TL00" to "华为畅享 8 Plus 移动 4G+ 版",
                    "FLA-TL10" to "华为畅享 8 Plus 移动 4G+ 版",
                    "LDN-AL00" to "华为畅享 8 全网通标配版",
                    "LDN-AL10" to "华为畅享 8 全网通 NFC 版",
                    "LDN-AL20" to "华为畅享 8 全网通高配版",
                    "LDN-TL00" to "华为畅享 8 移动 4G+ 标配版",
                    "LDN-TL10" to "华为畅享 8 移动 4G+ NFC 版",
                    "LDN-TL20" to "华为畅享 8 移动 4G+ 高配版",
                    "ATU-AL10" to "华为畅享 8e 全网通版",
                    "ATU-TL10" to "华为畅享 8e 移动 4G+ 版",
                    "DRA-AL00" to "华为畅享 8e 青春 全网通版",
                    "DRA-TL00" to "华为畅享 8e 青春 移动 4G+ 版",
                    "JKM-AL00" to "华为畅享 9 Plus 全网通版",
                    "JKM-AL00a" to "华为畅享 9 Plus 全网通版 (4GB+64GB)",
                    "JKM-AL00b" to "华为畅享 9 Plus 全网通版 (麒麟 710F)",
                    "JKM-TL00" to "华为畅享 9 Plus 移动 4G+ 版",
                    "ARS-AL00" to "华为畅享 MAX 全网通版",
                    "ARS-TL00" to "华为畅享 MAX 移动 4G+ 版",
                    "DUB-AL00" to "华为畅享 9 全网通标配/高配版",
                    "DUB-AL20" to "华为畅享 9 全网通顶配版",
                    "DUB-TL00" to "华为畅享 9 移动 4G+ 标配/高配版",
                    "DUB-TL00a" to "华为畅享 9 移动 4G+ 顶配版",
                    "POT-AL00a" to "华为畅享 9S 全网通版",
                    "POT-TL00a" to "华为畅享 9S 移动 4G+ 版",
                    "MRD-AL00" to "华为畅享 9e 全网通版",
                    "MRD-TL00" to "华为畅享 9e 移动 4G+ 版",
                    "STK-AL00" to "华为畅享 10 Plus 全网通版",
                    "STK-TL00" to "华为畅享 10 Plus 移动 4G+ 版",
                    "ART-AL00x" to "华为畅享 10 全网通版",
                    "ART-AL00m" to "华为畅享 10 全网通版",
                    "ART-TL00x" to "华为畅享 10 移动 4G+ 版",
                    "AQM-AL00" to "华为畅享 10S 全网通版",
                    "AQM-TL00" to "华为畅享 10S 移动 4G+ 版",
                    "MED-AL00" to "华为畅享 10e 全网通版",
                    "MED-AL20" to "华为畅享 10e 全网通版",
                    "MED-TL00" to "华为畅享 10e 移动 4G+ 版",
                    "DVC-AN00" to "华为畅享 Z 5G",
                    "DVC-AN20" to "华为畅享 20 Pro 5G 全网通版",
                    "DVC-TN20" to "华为畅享 20 Pro 5G 移动版",
                    "WKG-AN00" to "华为畅享 20 5G 全网通版",
                    "WKG-TN00" to "华为畅享 20 5G 移动版",
                    "FRL-AN00a" to "华为畅享 20 Plus 5G 全网通版",
                    "FRL-TN00" to "华为畅享 20 Plus 5G 移动版",
                    "PPA-AL20" to "华为畅享 20 SE",
                    "MLD-AL00" to "华为畅享 20e (麒麟 710A)",
                    "MLD-AL10" to "华为畅享 20e (Helio P35)",
                    "MGA-AL00" to "华为畅享 50",
                    "CTR-AL00" to "华为畅享 50 Pro",
                    "EVE-AL00" to "华为畅享 50z",
                    "MGA-AL40" to "华为畅享 60",
                    "STG-AL00" to "华为畅享 60X",
                    "MAO-AL00" to "华为畅享 60 Pro",
                    "FGD-AL00" to "华为畅享 70",
                    "CTR-AL20" to "华为畅享 70 Pro",
                    "MGA-AL40" to "华为畅享 70z",
                    "GFY-AL00" to "华为畅享 70S",
                    "BRE-AL80" to "华为畅享 70X",
                    "BRE-AL00a" to "华为畅享 70X 活力版 (128GB)",
                    "BRE-AL00b" to "华为畅享 70X 活力版 (256GB/512GB)",
                    "JUY-AL00" to "华为畅享 80",
                    // 平板 M 系列
                    "S8-301W" to "华为 MediaPad M1 Wi-Fi 版",
                    "S8-301U" to "华为 MediaPad M1 3G 版",
                    "S8-303L" to "华为 MediaPad M1 LTE 版",
                    "M2-801W" to "华为揽阅 M2 8.0 Wi-Fi 版",
                    "M2-803L" to "华为揽阅 M2 8.0 LTE 版",
                    "M2-A01W" to "华为揽阅 M2 10.0 Wi-Fi 版",
                    "M2-A01L" to "华为揽阅 M2 10.0 LTE 版",
                    "PLE-703L" to "华为揽阅 M2 青春版 7.0 英寸 全网通版",
                    "PLE-703LT" to "华为揽阅 M2 青春版 7.0 英寸 双 4G 版",
                    "FDR-A01w" to "华为揽阅 M2 青春版 10.1 英寸 Wi-Fi 版",
                    "FDR-A03L" to "华为揽阅 M2 青春版 10.1 英寸 LTE 版",
                    "BTV-W09" to "华为平板 M3 Wi-Fi 版",
                    "BTV-DL09" to "华为平板 M3 LTE 版",
                    "CPN-W09" to "华为平板 M3 青春版 8.0 英寸 Wi-Fi 版",
                    "CPN-AL00" to "华为平板 M3 青春版 8.0 英寸 LTE 版",
                    "BAH-W09" to "华为平板 M3 青春版 10.1 英寸 Wi-Fi 版",
                    "BAH-AL00" to "华为平板 M3 青春版 10.1 英寸 LTE 版",
                    "SHT-W09" to "华为平板 M5 8.4 英寸 Wi-Fi 版",
                    "SHT-AL09" to "华为平板 M5 8.4 英寸 LTE 版",
                    "CMR-W09" to "华为平板 M5 10.8 英寸 Wi-Fi 版",
                    "CMR-AL09" to "华为平板 M5 10.8 英寸 LTE 版",
                    "CMR-W19" to "华为平板 M5 Pro Wi-Fi 版",
                    "CMR-AL19" to "华为平板 M5 Pro LTE 版",
                    "BAH2-W09" to "华为平板 M5 青春版 10.1 英寸 Wi-Fi 版",
                    "BAH2-AL10" to "华为平板 M5 青春版 10.1 英寸 LTE 版",
                    "JDN2-W09" to "华为平板 M5 青春版 8 英寸 Wi-Fi 版",
                    "JDN2-AL00" to "华为平板 M5 青春版 8 英寸 LTE 版",
                    "JDN2-AL50" to "华为平板 M5 青春版 8 英寸 LTE 版",
                    "VRD-W09" to "华为平板 M6 8.4 英寸 Wi-Fi 版",
                    "VRD-AL09" to "华为平板 M6 8.4 英寸 LTE 版",
                    "VRD-W10" to "华为平板 M6 高能版 8.4 英寸 Wi-Fi 版",
                    "VRD-AL10" to "华为平板 M6 高能版 8.4 英寸 LTE 版",
                    "SCM-W09" to "华为平板 M6 10.8 英寸 Wi-Fi 版",
                    "SCM-AL09" to "华为平板 M6 10.8 英寸 LTE 版",
                    // MatePad Pro 系列
                    "MRX-W09" to "HUAWEI MatePad Pro 10.8 英寸 Wi-Fi 版 (6GB+128GB/8GB+256GB)",
                    "MRX-W29" to "HUAWEI MatePad Pro 10.8 英寸 Wi-Fi 版 (6GB+128GB/8GB+256GB)",
                    "MRX-AL09" to "HUAWEI MatePad Pro 10.8 英寸 LTE 版 (6GB+128GB/8GB+256GB)",
                    "MRX-W19" to "HUAWEI MatePad Pro 10.8 英寸 Wi-Fi 版 (8GB+256GB)",
                    "MRX-W39" to "HUAWEI MatePad Pro 10.8 英寸 Wi-Fi 版 (8GB+256GB)",
                    "MRX-AL19" to "HUAWEI MatePad Pro 10.8 英寸 LTE 版 (8GB+512GB)",
                    "MRX-AN19" to "HUAWEI MatePad Pro 10.8 英寸 5G 版",
                    "MRR-W29" to "HUAWEI MatePad Pro 10.8 英寸 2021 Wi-Fi 版",
                    "MRR-W39" to "HUAWEI MatePad Pro 10.8 英寸 2021 Wi-Fi 版",
                    "GOT-W29" to "HUAWEI MatePad Pro 11 英寸 Wi-Fi 版",
                    "GOT-W09" to "HUAWEI MatePad Pro 11 英寸 性能版 Wi-Fi 版",
                    "GOT-AL09" to "HUAWEI MatePad Pro 11 英寸 性能版 LTE 版",
                    "GOT-AL19" to "HUAWEI MatePad Pro 11 英寸 性能版 LTE 版 (12GB+512GB)",
                    "XYAO-W00" to "HUAWEI MatePad Pro 11 英寸 2024 Wi-Fi 版",
                    "WGR-W09" to "HUAWEI MatePad Pro 12.6 英寸 2021 Wi-Fi 版",
                    "WGR-W19" to "HUAWEI MatePad Pro 12.6 英寸 2021 Wi-Fi 版",
                    "WGR-AN19" to "HUAWEI MatePad Pro 12.6 英寸 2021 5G 版",
                    "WGRR-W09" to "HUAWEI MatePad Pro 12.6 英寸 2022 Wi-Fi 版",
                    "WGRR-W19" to "HUAWEI MatePad Pro 12.6 英寸 2022 Wi-Fi 版",
                    "PCE-W30" to "HUAWEI MatePad Pro 13.2 英寸 Wi-Fi 版",
                    "PCE-W40" to "HUAWEI MatePad Pro 13.2 英寸 Wi-Fi 版 (16GB+1TB)",
                    "PCE-AL30" to "HUAWEI MatePad Pro 13.2 英寸 SIM 卡版",
                    "PCE-AL40" to "HUAWEI MatePad Pro 13.2 英寸 典藏版 SIM 卡版",
                    "MRO-W00" to "HUAWEI MatePad Pro 12.2 英寸 Wi-Fi 版",
                    "MRO-W10" to "HUAWEI MatePad Pro 12.2 英寸 Wi-Fi 版 (16GB+1TB)",
                    "MRO-AL10" to "HUAWEI MatePad Pro 12.2 英寸 SIM 卡版 (16GB+1TB)",
                    "WEB-W00" to "HUAWEI MatePad Pro 13.2 英寸 2025 Wi-Fi 版",
                    "WEB-W10" to "HUAWEI MatePad Pro 13.2 英寸 2025 Wi-Fi 版 (16GB+1TB)",
                    "WEB-AL00" to "HUAWEI MatePad Pro 13.2 英寸 2025 典藏版 SIM 卡版",
                    // MatePad Air 系列
                    "DBY2-W00" to "HUAWEI MatePad Air 11.5 英寸 Wi-Fi 版",
                    "DBY2-AL00" to "HUAWEI MatePad Air 11.5 英寸 LTE 版",
                    "BKY-W00" to "HUAWEI MatePad Air 12 英寸 Wi-Fi 版 (8GB+256GB)",
                    "BKY-W10" to "HUAWEI MatePad Air 12 英寸 Wi-Fi 版 (12GB+256GB)",
                    "BKY-W20" to "HUAWEI MatePad Air 12 英寸 柔光版 Wi-Fi 版",
                    // MatePad 系列
                    "BAH3-W09" to "HUAWEI MatePad 10.4 英寸 Wi-Fi 版 (麒麟 810)",
                    "BAH3-W59" to "HUAWEI MatePad 10.4 英寸 Wi-Fi 版 (麒麟 820)",
                    "BAH3-AL00" to "HUAWEI MatePad 10.4 英寸 LTE 版 (麒麟 810)",
                    "BAH3-AN10" to "HUAWEI MatePad 5G 10.4 英寸 (麒麟 820)",
                    "SCMR-W09" to "HUAWEI MatePad 10.8 英寸 Wi-Fi 版",
                    "SCMR-AL09" to "HUAWEI MatePad 10.8 英寸 LTE 版",
                    "BAH4-W09" to "HUAWEI MatePad 10.4 英寸 2022 Wi-Fi 版 (麒麟 710A)",
                    "BAH4-W29" to "HUAWEI MatePad 10.4 英寸 2022 Wi-Fi 版 (麒麟 710A)",
                    "BAH4-W39" to "HUAWEI MatePad 10.4 英寸 2022 Wi-Fi 版 (麒麟 710A)",
                    "BAH4-W19" to "HUAWEI MatePad 10.4 英寸 2022 悦动版 Wi-Fi 版 (骁龙 778G 4G)",
                    "BAH4-AL10" to "HUAWEI MatePad 10.4 英寸 2022 悦动版 LTE 版 (骁龙 778G 4G)",
                    "DBY-W09" to "HUAWEI MatePad 11 英寸 Wi-Fi 版",
                    "DBR-W00" to "HUAWEI MatePad 11 英寸 2023 Wi-Fi 版",
                    "DBR-W10" to "HUAWEI MatePad 11 英寸 2023 柔光版 Wi-Fi 版",
                    "BTK-W00" to "HUAWEI MatePad 11.5 英寸 2023 Wi-Fi 版",
                    "BTK-AL00" to "HUAWEI MatePad 11.5 英寸 2023 LTE 版",
                    "BTKR-W00" to "HUAWEI MatePad 11.5 英寸 2024 Wi-Fi 版",
                    "TGR-W00" to " HUAWEI MatePad 11.5\" S Wi-Fi 版",
                    "TGR-W10" to " HUAWEI MatePad 11.5\" S 柔光版 Wi-Fi 版",
                    "DMG-W00" to " HUAWEI MatePad 11.5\" S 灵动款 Wi-Fi 版",
                )
            }
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
            const val PRODUCT_MODEL = "ro.product.model"

            /** 三星手机市场名称 */
            val samsungMarketNameMap by lazy {
                hashMapOf(
                    // =============== Galaxy S 系列 ===============
                    "GT-I9000" to "Galaxy S",
                    "GT-I9008" to "Galaxy S 移动定制版",
                    "GT-I9008L" to "Galaxy S 移动定制版",
                    "GT-I9018" to "Galaxy S 移动定制版",
                    "SCH-i909" to "Galaxy S 电信定制版",
                    "GT-I9100" to "Galaxy SII (Exynos)",
                    "GT-I9100G" to "Galaxy SII (德州仪器)",
                    "GT-I9108" to "Galaxy SII 移动定制版",
                    "SCH-I919" to "Galaxy S Duos 电信定制版",
                    "GT-I9300" to "Galaxy S3 公开版",
                    "GT-I9308" to "Galaxy S3 移动定制版",
                    "SCH-I939" to "Galaxy S3 电信定制版",
                    "SCH-I939D" to "Galaxy S3 电信双卡定制版",
                    "GT-I9300I" to "Galaxy S3 Neo+ 公开版",
                    "GT-I9308I" to "Galaxy S3 Neo+ 移动定制版",
                    "SCH-I939I" to "Galaxy S3 Neo+ 电信定制版",
                    "GT-I8190N" to "Galaxy S3 Mini",
                    "GT-I9500" to "Galaxy S4 公开版",
                    "GT-I9502" to "Galaxy S4 联通定制版",
                    "GT-I9508" to "Galaxy S4 移动定制版",
                    "SCH-I959" to "Galaxy S4 电信定制版",
                    "GT-I9507V" to "Galaxy S4 联通4G定制版",
                    "GT-I9508V" to "Galaxy S4 移动4G定制版",
                    "SM-C101" to "Galaxy S4 zoom",
                    "SM-G9009D" to "Galaxy S5 电信3G双卡版",
                    "SM-G9006V" to "Galaxy S5 联通4G单卡版",
                    "SM-G9008V" to "Galaxy S5 移动4G单卡版",
                    "SM-G9006W" to "Galaxy S5 联通4G双卡版",
                    "SM-G9008W" to "Galaxy S5 移动4G双卡版",
                    "SM-G9009W" to "Galaxy S5 电信4G双卡版",
                    "SM-G9200" to "Galaxy S6 全网通版",
                    "SM-G9208" to "Galaxy S6 移动定制版",
                    "SM-G9209" to "Galaxy S6 电信定制版",
                    "SM-G9250" to "Galaxy S6 edge",
                    "SM-G9280" to "Galaxy S6 edge+",
                    "SM-G9300" to "Galaxy S7 全网通版",
                    "SM-G9308" to "Galaxy S7 移动定制版",
                    "SM-G9350" to "Galaxy S7 edge",
                    "SM-G9500" to "Galaxy S8 全网通版",
                    "SM-G9508" to "Galaxy S8 4G+",
                    "SM-G9550" to "Galaxy S8+",
                    "SM-G9600/DS" to "Galaxy S9 全网通版",
                    "SM-G9608/DS" to "Galaxy S9 4G+",
                    "SM-G9650/DS" to "Galaxy S9+",
                    "SM-G8750" to "Galaxy S 轻奢版",
                    "SM-G9700" to "Galaxy S10e 全网通版",
                    "SM-G9708" to "Galaxy S10e 4G+",
                    "SM-G9730" to "Galaxy S10 全网通版",
                    "SM-G9738" to "Galaxy S10 4G+",
                    "SM-G9750" to "Galaxy S10+ 全网通版",
                    "SM-G9758" to "Galaxy S10+ 4G+",
                    "SM-G9810" to "Galaxy S20 5G",
                    "SM-G9860" to "Galaxy S20+ 5G",
                    "SM-G9880" to "Galaxy S20 Ultra 5G",
                    "SM-G7810" to "Galaxy S20 FE 5G",
                    "SM-G9910" to "Galaxy S21 5G",
                    "SM-G9960" to "Galaxy S21+ 5G",
                    "SM-G9980" to "Galaxy S21 Ultra 5G",
                    "SM-G9900" to "Galaxy S21 FE 5G",
                    "SM-S9010" to "Galaxy S22",
                    "SM-S9060" to "Galaxy S22+",
                    "SM-S9080" to "Galaxy S22 Ultra",
                    "SM-S9110" to "Galaxy S23",
                    "SM-S9160" to "Galaxy S23+",
                    "SM-S9180" to "Galaxy S23 Ultra",
                    "SM-S7110" to "Galaxy S23 FE",
                    "SM-S9210" to "Galaxy S24",
                    "SM-S9260" to "Galaxy S24+",
                    "SM-S9280" to "Galaxy S24 Ultra",
                    "SM-S9310" to "Galaxy S25",
                    "SM-S9360" to "Galaxy S25+",
                    "SM-S9370" to "Galaxy S25 Edge",
                    "SM-S9380" to "Galaxy S25 Ultra",

                    // =============== Galaxy Note 系列 ===============
                    "GT-N7100" to "Galaxy Note2 公开版",
                    "GT-N7102" to "Galaxy Note2 联通定制版",
                    "GT-N7102i" to "Galaxy Note2 联通定制版",
                    "GT-N7108" to "Galaxy Note2 移动定制版",
                    "GT-N7108D" to "Galaxy Note2 移动4G定制版",
                    "SCH-N719" to "Galaxy Note2 电信定制版",
                    "SM-N9002" to "Galaxy Note3 联通定制版",
                    "SM-N9006" to "Galaxy Note3 公开版",
                    "SM-N9008" to "Galaxy Note3 移动定制版",
                    "SM-N9008V" to "Galaxy Note3 移动4G定制版",
                    "SM-N9008S" to "Galaxy Note3 4G公开版",
                    "SM-N9009" to "Galaxy Note3 电信定制版",
                    "SM-N7506V" to "Galaxy Note3 Lite 联通定制版",
                    "SM-N7508V" to "Galaxy Note3 Lite 移动定制版",
                    "SM-N7509V" to "Galaxy Note3 Lite 电信定制版",
                    "SM-N9100" to "Galaxy Note4 公开版",
                    "SM-N9106W" to "Galaxy Note4 联通定制版",
                    "SM-N9108V" to "Galaxy Note4 移动定制版",
                    "SM-N9109W" to "Galaxy Note4 电信定制版",
                    "SM-N9150" to "Galaxy Note Edge",
                    "SM-N9200" to "Galaxy Note5 全网通版",
                    "SM-N9208" to "Galaxy Note5 移动定制版",
                    "SM-N9300" to "Galaxy Note7",
                    "SM-N9500" to "Galaxy Note8 全网通版",
                    "SM-N9508" to "Galaxy Note8 4G+",
                    "SM-N9600" to "Galaxy Note9 全网通版",
                    "SM-N9608" to "Galaxy Note9 4G+",
                    "SM-N9700" to "Galaxy Note10",
                    "SM-N9760" to "Galaxy Note10+ 5G",
                    "SM-N9810" to "Galaxy Note20 5G",
                    "SM-N9860" to "Galaxy Note20 Ultra 5G",

                    // =============== Galaxy Z 系列 ===============
                    "SM-F7000" to "Galaxy Z Flip 5G",
                    "SM-F7210" to "Galaxy Z Flip4",
                    "SM-F9360" to "Galaxy Z Fold4 5G",
                    "SM-F7110" to "Galaxy Z Flip3 5G",
                    "SM-F9160" to "Galaxy Z Fold2 5G",
                    "SM-F7070" to "Galaxy Z Flip",
                    "SM-F9000" to "Galaxy Fold",
                    "SM-F9070" to "Galaxy Fold 5G",
                    "SM-F9260" to "Galaxy Z Fold3 5G",

                    // =============== Galaxy Tab系列 ===============
                    "SM-T700" to "Galaxy Tab S 8.4 WLAN",
                    "SM-T705C" to "Galaxy Tab S 8.4 LTE",
                    "SM-T800" to "Galaxy Tab S 10.5 WLAN",
                    "SM-T805C" to "Galaxy Tab S 10.5 LTE",
                    "SM-T710" to "Galaxy Tab S2 8.0 WLAN (Exynos)",
                    "SM-T715C" to "Galaxy Tab S2 8.0 LTE (Exynos)",
                    "SM-T713" to "Galaxy Tab S2 8.0 WLAN (高通)",
                    "SM-T719C" to "Galaxy Tab S2 8.0 LTE (高通)",
                    "SM-T810" to "Galaxy Tab S2 9.7 WLAN (Exynos)",
                    "SM-T815C" to "Galaxy Tab S2 9.7 LTE (Exynos)",
                    "SM-T813" to "Galaxy Tab S2 9.7 WLAN (高通)",
                    "SM-T819C" to "Galaxy Tab S2 9.7 LTE (高通)",
                    "SM-T820" to "Galaxy Tab S3 WLAN",
                    "SM-T830" to "Galaxy Tab S4 WLAN",
                    "SM-T835C" to "Galaxy Tab S4 LTE",
                    "SM-T720" to "Galaxy Tab S5e WLAN",
                    "SM-T725C" to "Galaxy Tab S5e LTE",
                    "SM-T860" to "Galaxy Tab S6 WLAN",
                    "SM-T970" to "Galaxy Tab S7+ WLAN",
                    "SM-T730" to "Galaxy Tab S7 FE WLAN (骁龙750G)",
                    "SM-T733" to "Galaxy Tab S7 FE WLAN (骁龙778G)",
                    "SM-T735C" to "Galaxy Tab S7 FE LTE",
                    "SM-X700" to "Galaxy Tab S8 WLAN",
                    "SM-X706C" to "Galaxy Tab S8 5G",
                    "SM-X800" to "Galaxy Tab S8+ WLAN",
                    "SM-X806C" to "Galaxy Tab S8+ 5G",
                    "SM-X900" to "Galaxy Tab S8 Ultra WLAN",
                    "SM-X906C" to "Galaxy Tab S8 Ultra 5G",
                    "SM-X710" to "Galaxy Tab S9 WLAN",
                    "SM-X810" to "Galaxy Tab S9+ WLAN",
                    "SM-X910" to "Galaxy Tab S9 Ultra WLAN",
                    "SM-X916C" to "Galaxy Tab S9 Ultra 5G",
                    "SM-X510" to "Galaxy Tab S9 FE WLAN",
                    "SM-X516C" to "Galaxy Tab S9 FE 5G",
                    "SM-X610" to "Galaxy Tab S9 FE+ WLAN",
                    "SM-X626C" to "Galaxy Tab S9 FE+ 5G"
                    // ... 可根据需要继续补充其他小众型号
                )
            }
        }

        override val marketName: String
            get() = SamsungProps.samsungMarketNameMap[Build.MODEL]
                ?: getSystemProperty(SamsungProps.PRODUCT_MODEL)

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
            get() = getSystemProperty("ro.config.marketing_name")

        override fun detect(): SystemRomInfo {
            return if (hasSystemProperty("ro.horizon.version")) {
                SystemRomInfo(
                    Rom.MAGIC_UI,
                    getSystemProperty("ro.horizon.version"),
                    getSystemProperty("ro.horizon.version.code", Build.VERSION.INCREMENTAL),
                )
            }
            // 检测MagicOS
            else if (hasSystemProperty(HonorProps.HONOR_MAGIC_VERSION)) {
                val property = getSystemProperty(
                    HonorProps.HONOR_MAGIC_VERSION_CODE,
                    Build.VERSION.INCREMENTAL
                )
                SystemRomInfo(
                    Rom.MagicOS,
                    getSystemProperty(HonorProps.HONOR_MAGIC_VERSION_NAME),
                    property,
                    "MagicOS $property"
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