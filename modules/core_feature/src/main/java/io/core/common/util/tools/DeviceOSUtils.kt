package io.core.common.util.tools

import android.annotation.SuppressLint
import android.os.Build

object DeviceOSUtils {

    enum class DeviceBrand {
        HUAWEI, XIAOMI, OPPO, VIVO, MEIZU, ONEPLUS, REALME,
        SAMSUNG, SONY, LENOVO, ZTE, NUBIA, HONOR, UNKNOWN
    }

    enum class SystemUIType {
        EMUI, MIUI, COLOR_OS, FUNTOUCH_OS, FLYME, H2OS, REALME_UI,
        ONE_UI, XPERIA_UI, ZUI, MYOS, NUBIA_UI, MAGIC_UI, ANDROID
    }

    // 通过 lazy 实现缓存，避免重复检测
    @JvmStatic
    val deviceBrand: DeviceBrand by lazy { detectDeviceBrand() }
    @JvmStatic
    val systemUIType: SystemUIType by lazy { detectSystemUIType() }

    // 设备品牌检测核心逻辑
    private fun detectDeviceBrand(): DeviceBrand {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        return when {
            "huawei" in manufacturer || "huawei" in brand -> DeviceBrand.HUAWEI
            "honor" in manufacturer || "honor" in brand -> DeviceBrand.HONOR
            "xiaomi" in manufacturer || "xiaomi" in brand -> DeviceBrand.XIAOMI
            "oppo" in manufacturer || "oppo" in brand -> DeviceBrand.OPPO
            "vivo" in manufacturer || "vivo" in brand -> DeviceBrand.VIVO
            "meizu" in manufacturer || "meizu" in brand -> DeviceBrand.MEIZU
            "oneplus" in manufacturer || "oneplus" in brand -> DeviceBrand.ONEPLUS
            "realme" in manufacturer || "realme" in brand -> DeviceBrand.REALME
            "samsung" in manufacturer || "samsung" in brand -> DeviceBrand.SAMSUNG
            "sony" in manufacturer || "sony" in brand -> DeviceBrand.SONY
            "lenovo" in manufacturer || "lenovo" in brand -> DeviceBrand.LENOVO
            "zte" in manufacturer || "zte" in brand -> DeviceBrand.ZTE
            "nubia" in manufacturer || "nubia" in brand -> DeviceBrand.NUBIA
            else -> DeviceBrand.UNKNOWN
        }
    }

    // 系统类型检测核心逻辑
    private fun detectSystemUIType(): SystemUIType {
        return when (deviceBrand) {
            DeviceBrand.HUAWEI -> if (getSystemProperty("ro.build.version.emui").isNotEmpty()) SystemUIType.EMUI else SystemUIType.ANDROID
            DeviceBrand.HONOR -> if (getSystemProperty("ro.build.version.emui").isNotEmpty()) SystemUIType.MAGIC_UI else SystemUIType.ANDROID
            DeviceBrand.XIAOMI -> if (getSystemProperty("ro.miui.ui.version.name").isNotEmpty()) SystemUIType.MIUI else SystemUIType.ANDROID
            DeviceBrand.OPPO -> if (getSystemProperty("ro.oplus.os.version").isNotEmpty()) SystemUIType.COLOR_OS else SystemUIType.ANDROID
            DeviceBrand.VIVO -> if (getSystemProperty("ro.vivo.os.version").isNotEmpty()) SystemUIType.FUNTOUCH_OS else SystemUIType.ANDROID
            DeviceBrand.MEIZU -> if (getSystemProperty("ro.build.flavor").contains("flyme")) SystemUIType.FLYME else SystemUIType.ANDROID
            DeviceBrand.ONEPLUS -> if (getSystemProperty("ro.oxygen.version").isNotEmpty()) SystemUIType.H2OS else SystemUIType.ANDROID
            DeviceBrand.REALME -> if (getSystemProperty("ro.build.version.opporom").contains("realme")) SystemUIType.REALME_UI else SystemUIType.ANDROID
            DeviceBrand.SAMSUNG -> if (getSystemProperty("ro.build.scafe.version").isNotEmpty()) SystemUIType.ONE_UI else SystemUIType.ANDROID
            DeviceBrand.LENOVO -> if (getSystemProperty("ro.zui.version").isNotEmpty()) SystemUIType.ZUI else SystemUIType.ANDROID
            DeviceBrand.ZTE -> if (getSystemProperty("ro.build.MyosVersion").isNotEmpty()) SystemUIType.MYOS else SystemUIType.ANDROID
            DeviceBrand.NUBIA -> if (getSystemProperty("ro.nubia.ui.version").isNotEmpty()) SystemUIType.NUBIA_UI else SystemUIType.ANDROID
            else -> SystemUIType.ANDROID
        }
    }

    // 通过反射获取系统属性
    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String): String {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod = clazz.getMethod("get", String::class.java)
            getMethod.invoke(null, key) as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // 扩展属性快捷访问
    @JvmStatic
    val isHuawei get() = deviceBrand == DeviceBrand.HUAWEI
    @JvmStatic
    val isXiaomi get() = deviceBrand == DeviceBrand.XIAOMI
    @JvmStatic
    val isOPPO get() = deviceBrand == DeviceBrand.OPPO
    @JvmStatic
    val isVivo get() = deviceBrand == DeviceBrand.VIVO
    @JvmStatic
    val isMIUI get() = systemUIType == SystemUIType.MIUI
    @JvmStatic
    val isEMUI get() = systemUIType == SystemUIType.EMUI
    @JvmStatic
    val isColorOS get() = systemUIType == SystemUIType.COLOR_OS
    // 其他厂商快捷属性可按需添加...
}
