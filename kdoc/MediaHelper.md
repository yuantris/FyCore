# MediaHelper 工具类文档

`MediaHelper` 是一个用于处理媒体文件（音频/视频）的工具类，提供了多种方法来获取媒体文件的各种属性和元数据。

## 方法列表

### 1. 获取媒体文件时长
```kotlin
fun getDuration(filePath: String, formatStr: String? = "mm:ss"): String?
```
- **功能**：获取音频/视频文件的时长，并按指定格式输出
- **参数**：
  - `filePath`: 媒体文件路径
  - `formatStr`: 格式化字符串（例如 "HH:mm:ss", "mm:ss", "m:ss.SS"）
- **返回值**：格式化后的时长（获取失败返回 null）
- **示例**：
```kotlin
val duration1 = MediaHelper.getDuration(filePath, "HH:mm:ss") // 00:03:45
val duration2 = MediaHelper.getDuration(filePath, "mm:ss")   // 03:45
```

### 2. 获取视频分辨率
```kotlin
fun getVideoResolution(filePath: String): Pair<Int, Int>?
```
- **功能**：获取视频分辨率（宽高）
- **返回值**：宽高Pair（获取失败返回 null）

### 3. 获取视频旋转角度
```kotlin
fun getVideoRotation(filePath: String): Int
```
- **功能**：获取视频旋转角度（0, 90, 180, 270）

### 4. 获取视频缩略图
```kotlin
fun getVideoThumbnail(filePath: String): Bitmap?
```
- **功能**：获取视频第一帧缩略图

### 5. 获取音频专辑封面
```kotlin
fun getAudioAlbumArt(filePath: String): Bitmap?
```
- **功能**：获取音频专辑封面图片

### 6. 获取视频帧率
```kotlin
fun getVideoFrameRate(filePath: String): Int?
```
- **功能**：获取视频帧率（单位：fps）

### 7. 获取音频比特率
```kotlin
fun getAudioBitrate(filePath: String): Int?
```
- **功能**：获取音频比特率（单位：kbps）

### 8. 获取视频关键帧
```kotlin
fun getVideoKeyFrame(filePath: String, timeUs: Long = 0): Bitmap?
```
- **功能**：获取指定时间的关键帧缩略图

### 9. 验证媒体文件有效性
```kotlin
fun isMediaFileValid(filePath: String): Boolean
```
- **功能**：检查文件是否为有效媒体文件

### 10. 获取媒体文件类型
```kotlin
fun getMimeType(filePath: String): String?
```
- **功能**：获取媒体文件MIME类型（如 "video/mp4", "audio/mpeg"）

### 11. 批量验证媒体文件
```kotlin
fun areVideosValid(filePaths: List<String>): Map<String, Boolean>
```
- **功能**：批量验证媒体文件是否有效

### 12. 增强版批量验证
```kotlin
fun areVideosValidV2(filePaths: List<String>): Pair<List<String>, Map<String, Exception>>
```
- **功能**：批量验证媒体文件有效性，返回无效文件及其异常信息

## 协程扩展方法

所有方法都有对应的协程版本，方法名以`Suspend`结尾，例如：
```kotlin
suspend fun getDurationSuspend(filePath: String, formatStr: String? = "mm:ss"): String?
```

## 注意事项
1. 所有方法都是`@WorkerThread`注解的，应在后台线程调用
2. 批量操作方法性能开销较大，建议在后台线程执行
3. 获取缩略图等方法可能返回null，需做好空值处理