# ColorTools 颜色工具类

## 概述
提供Android颜色处理的通用工具方法，支持颜色空间转换、亮度调节、透明度控制等操作。

## 核心方法

### Alpha通道处理
```kotlin
@JvmStatic
fun stripAlpha(@ColorInt color: Int): Int
```
- 实现原理：
  1. 使用-0x1000000(0xFF000000)进行按位或运算
  2. 保留RGB通道同时强制设置Alpha为不透明
  3. 位运算公式：`-0x1000000 or color`

```kotlin
@JvmStatic
fun adjustAlpha(@ColorInt color: Int, factor: Float): Int
```
- 透明度计算：
  1. 原始alpha值乘以factor因子
  2. 结果进行四舍五入取整
  3. 边界保护：0-255范围约束

### 颜色转换
```kotlin
@JvmStatic
fun intToString(intColor: Int): String
```
- 功能：将颜色整数值转换为#RRGGBB格式字符串
- 示例：`0xFFAABBCC → "#AABBCC"`

### 亮度调节
```kotlin
@JvmStatic
fun shiftColor(color: Int, by: Float): Int
```
- 算法实现：
  1. 转换到HSV色彩空间
  2. 调整value分量：`hsv[2] *= by`
  3. 保留原始Alpha通道
  4. 边界保护：V分量自动约束在0.0-1.0范围
- 性能优化：
  • 使用FloatArray复用内存
  • 避免新建Color对象

### 透明度控制
```kotlin
@JvmStatic
fun withAlpha(baseColor: Int, alpha: Float): Int
```
- 算法特点：
  1. 使用位运算保持RGB通道不变
  2. 支持0.0-1.0透明度范围校验

### 颜色混合算法
```kotlin
@JvmStatic
fun blendColors(color1: Int, color2: Int, ratio: Float): Int
```
- 实现原理：
  - 线性插值计算各颜色分量
  - 支持透明度混合
  - 基于CollapsingToolbarLayout的官方实现

### 颜色差异计算
```kotlin
@JvmStatic
fun getColorDifference(a: Int, b: Int): Double
```
- 使用CIE76色差算法
- 转换到L*a*b颜色空间计算欧氏距离

## 扩展功能
### 随机颜色生成
```kotlin
@JvmStatic
fun getRandomColor(): Int
```
- 实现特点：
  • RGB各通道独立生成(0-255)
  • 固定Alpha为不透明

```kotlin
@JvmStatic
fun getRandomColorWithAlpha(): Int
```
- 增强功能：
  • 包含随机Alpha通道
  • 四通道独立生成

### 最佳实践
#### UI主题动态调整
```kotlin
val primaryColor = ColorTools.getRandomColor()
val darkColor = ColorTools.darkenColor(primaryColor)
val lightColor = ColorTools.lightenColor(primaryColor)
```

#### 数据可视化颜色筛选
```kotlin
val baseColor = Color.RED
if (ColorTools.getColorDifference(baseColor, compareColor) > 15.0) {
   // 显著色差提示
}
```

- 颜色反转算法
- ARGB/RGB互转工具

> 注意：所有方法均使用@JvmStatic注解支持Java调用