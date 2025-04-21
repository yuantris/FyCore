# FileSize 工具类文档

## 核心功能
- 📏 智能文件大小格式化（自动单位转换）
- ⏲️ 精确时间间隔格式化
- 🔄 单位字符串逆向解析

## 方法详解

### 文件大小格式化
```kotlin
fun format(
    size: Long,
    withUnit: Boolean = true,
    unitStyle: SizeUnitStyle = SizeUnitStyle.StandardUpper,
    precision: Int = 2,
    minUnit: SizeUnit = SizeUnit.BYTE
): String
```
**参数说明**
- `size`: 文件字节数
- `withUnit`: 是否显示单位（默认显示）
- `unitStyle`: 单位显示风格（见下文单位风格说明）
- `precision`: 小数精度（默认2位）
- `minUnit`: 最小显示单位（默认自动适配）

### 时间格式化扩展
```kotlin
fun formatDuration(
    millis: Long,
    showMillis: Boolean = false,
    compact: Boolean = false,
    unitStyle: TimeUnitStyle = TimeUnitStyle.SHORT_ENGLISH
): String
```
**模式示例**
- `"HH:mm:ss"` → 01:02:03
- `"mm分ss秒"` → 02分03秒

## 单位风格对照表

### 文件大小单位
| 风格类型          | 示例    |
|---------------|-------|
| StandardUpper | KB MB |
| StandardLower | kb mb |
| ShortUpper    | K M   |
| ShortLower    | k m   |

### 时间单位风格
```kotlin
// 英文风格
TimeUnitStyle.English.Short(UnitCase.LOWER) → 1h 2m 3s
// 中文风格
TimeUnitStyle.Chinese.Long() → 1小时 2分钟 3秒
```

## 异常处理
- `NumberFormatException`: 数值格式错误时抛出
- `IllegalArgumentException`: 无效单位或格式错误时抛出

## 扩展函数示例
```kotlin
val size = 37230L
toFormattedFileSize() // "36.35KB"

val duration = 3723000L
toFormattedDuration(unitStyle = Chinese.Short()) // "1时2分3秒"
```

## 最佳实践
1. 大文件处理使用`TB`作为最小单位：
```kotlin
FileSize.format(5L * 1024L.pow(4), minUnit = SizeUnit.TB)
```
2. 高精度场景设置`precision=4`
3. 使用`parseToBytes`解析用户输入