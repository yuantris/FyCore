# ImageProcessor 使用指南

## 概述

`ImageProcessor` 是一个全面重构的图片处理工具类，合并了原有的 `BitmapTools` 和 `ImageTools` 功能，提供了更加流畅和易用的 API。

## 主要特性

- **Builder 模式**：提供链式调用的流畅 API
- **功能分离**：解码、保存、转换功能独立
- **统一验证**：集中的 Bitmap 有效性检查
- **更好的错误处理**：统一的异常处理机制
- **向后兼容**：提供兼容性类保持原有 API

## 使用示例

### 1. 图片解码

#### 从文件解码
```kotlin
// 基本解码
val bitmap = ImageProcessor.decode()
    .fromFile("/path/to/image.jpg")
    .build()

// 指定尺寸解码（防止 OOM）
val bitmap = ImageProcessor.decode()
    .fromFile("/path/to/image.jpg")
    .withSize(800, 600)
    .build()

// 低内存模式解码
val bitmap = ImageProcessor.decode()
    .fromFile("/path/to/image.jpg")
    .withLowMemory()
    .build()
```

#### 从资源解码
```kotlin
// 标准解码
val bitmap = ImageProcessor.decode()
    .fromResource(R.drawable.my_image)
    .build()

// 低内存模式
val bitmap = ImageProcessor.decode()
    .fromResource(R.drawable.my_image)
    .withLowMemory()
    .build()

// 指定配置
val bitmap = ImageProcessor.decode()
    .fromResource(R.drawable.my_image)
    .withConfig(Bitmap.Config.RGB_565)
    .build()
```

#### 从 Assets 解码
```kotlin
val bitmap = ImageProcessor.decode()
    .fromAssets("images/sample.jpg")
    .withSize(400, 300)
    .build()
```

### 2. 图片保存

#### 保存到文件
```kotlin
val success = ImageProcessor.save()
    .bitmap(myBitmap)
    .toFile(File("/path/to/save/image.jpg"))
    .withFormat(Bitmap.CompressFormat.JPEG)
    .withQuality(90)
    .execute() as Boolean
```

#### 保存到相册
```kotlin
val savedFile = ImageProcessor.save()
    .bitmap(myBitmap)
    .toAlbum("MyApp")
    .withFormat(Bitmap.CompressFormat.JPEG)
    .withQuality(85)
    .withRecycle(true)
    .execute() as File?
```

### 3. 图片转换

#### 转换为 InputStream
```kotlin
val inputStream = ImageProcessor.transform()
    .bitmap(myBitmap)
    .toInputStream(quality = 90)
```

#### 转换为字节数组
```kotlin
val byteArray = ImageProcessor.transform()
    .bitmap(myBitmap)
    .toByteArray(Bitmap.CompressFormat.PNG, 100)
```

#### 创建缩放图片
```kotlin
val scaledBitmap = ImageProcessor.transform()
    .bitmap(myBitmap)
    .createScaled(200, 200, filter = true)
```

## 兼容性

为了保持向后兼容，我们提供了兼容性类：

### BitmapToolsCompat
替代原有的 `BitmapTools`，保持相同的 API：

```kotlin
// 原有代码无需修改
val bitmap = BitmapToolsCompat.decodeBitmap(path, 800, 600)
val lowMemBitmap = BitmapToolsCompat.decodeBitmapWithLowMemory(R.drawable.image)
val inputStream = BitmapToolsCompat.toInputStream(bitmap)
```

### ImageToolsCompat
替代原有的 `ImageTools`，保持相同的 API：

```kotlin
// 原有代码无需修改
val success = ImageToolsCompat.save(bitmap, file, Bitmap.CompressFormat.JPEG, 90)
val savedFile = ImageToolsCompat.save2Album(bitmap, Bitmap.CompressFormat.JPEG, "MyApp", 85)
```

## 迁移建议

1. **新项目**：直接使用 `ImageProcessor` 的新 API
2. **现有项目**：
   - 可以继续使用兼容性类，无需修改现有代码
   - 逐步迁移到新 API，享受更好的开发体验
   - 在新功能中使用新 API

## 错误处理

新的 `ImageProcessor` 提供了更好的错误处理：

- 自动验证 Bitmap 有效性
- 统一的日志输出
- 异常安全的资源管理
- 详细的错误信息

## 性能优化

- 智能采样率计算，防止 OOM
- 资源自动管理，防止内存泄漏
- 支持低内存模式
- 可选的 Bitmap 回收机制