# DeviceOS 设备信息检测工具

## 功能概述
提供设备品牌检测、系统ROM识别和基础信息获取能力，支持 **20+** 主流安卓设备品牌检测和 **15种** ROM类型识别

## 核心能力
```kotlin
object DeviceOS {
    // 品牌检测
    val brand: Brand
    
    // ROM信息
    val romInfo: SystemRomInfo
    
    // 快捷判断属性
    val isHarmonyOS: Boolean
    val isHyperOS: Boolean
}
```

## 品牌检测支持
### Brand 枚举说明
| 主品牌    | 子品牌         | 检测特征                   |
|--------|-------------|------------------------|
| HUAWEI | -           | EMUI/HarmonyOS 系统属性    |
| Xiaomi | REDMI, POCO | MIUI/HyperOS 系统属性      |
| OPPO   | realme      | ColorOS 专属属性           |
| vivo   | IQOO        | FuntouchOS/OriginOS 特性 |

## ROM类型对照表
### Rom 枚举值
```kotlin
enum class Rom {
    HyperOS,     // 小米澎湃OS
    HarmonyOS,   // 华为鸿蒙OS
    ColorOS,     // OPPO ColorOS
    OriginOS,    // vivo原OS
    ONE_UI,      // 三星One UI
    MagicOS,     // 荣耀MagicOS
    // ...其他类型
}
```

## 使用示例
### 基础信息获取
```kotlin
// 获取设备品牌
when (DeviceOS.brand) {
    Brand.HUAWEI -> // 华为设备处理
    Brand.Xiaomi -> // 小米设备处理
}

// 判断系统类型
if (DeviceOS.isHarmonyOS) {
    // 鸿蒙系统专属逻辑
}

// 获取ROM详细信息
val romVersion = DeviceOS.romInfo.verName
```

### OEM信息展示
```kotlin
// 构建系统信息字符串
val systemInfo = buildString {
    append("品牌: ${DeviceOS.brand}")
    append("ROM: ${DeviceOS.romInfo.type} ${DeviceOS.romInfo.verCode}")
}
```

## 技术实现
- 通过反射读取`SystemProperties`获取设备专属属性
- 采用策略模式实现**品牌检测算法**与**ROM检测算法**解耦
- 支持鸿蒙OS/HyperOS等新型系统的特征识别