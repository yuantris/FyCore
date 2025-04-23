# Environment 扩展函数

提供Android系统路径操作的扩展函数和工具类。

## 目录类型枚举 PathType

定义系统标准目录类型的枚举，包含公共目录和私有目录。

### 枚举值

| 枚举值            | 类型 | 描述           |
|----------------|----|--------------|
| CACHE          | 私有 | 内部缓存目录（自动清理） |
| INTERNAL_FILES | 私有 | 内部持久化文件目录    |
| EXTERNAL_FILES | 私有 | 外部私有文件目录     |
| EXTERNAL_CACHE | 私有 | 外部缓存目录       |
| DCIM           | 公共 | 公共相册目录       |
| DOCUMENTS      | 公共 | 公共文档目录       |
| DOWNLOADS      | 公共 | 公共下载目录       |
| PICTURES       | 公共 | 公共图片目录       |
| MUSIC          | 公共 | 公共音乐目录       |

## 扩展属性

### 公共目录快捷访问

```kotlin
val dcimDir: File       // DCIM目录
val documentsDir: File  // Documents目录
val downloadsDir: File  // Downloads目录
val picturesDir: File   // Pictures目录
val musicDir: File      // Music目录
```

## 核心函数

### getBasePath

获取指定类型的基础目录路径。

```kotlin
fun Context.getBasePath(type: PathType): File
```

### getSettingsPath

获取系统标准路径，支持子目录和自动创建。

```kotlin
@JvmOverloads
fun Context.getSettingsPath(
    type: PathType,
    subDir: String? = null,
    autoCreate: Boolean = true
): String
```

### getSettingsPathV2

路径拼接优化版，支持文件名参数和自动转换路径分隔符。

```kotlin
@JvmOverloads
fun Context.getSettingsPathV2(
    type: PathType,
    subDir: String? = null,
    fileName: String? = null,
    autoCreate: Boolean = true
): String
```

## 路径处理工具

### toPath

路径构建辅助函数，统一转换路径分隔符。

```kotlin
fun String.toPath(): String
```

### joinPath

安全拼接路径的扩展函数。

```kotlin
fun String.joinPath(vararg parts: String): String
fun joinPath(vararg parts: String): String
```

## 使用示例

```kotlin
// 获取下载目录路径
val downloadPath = context.getSettingsPath(PathType.DOWNLOADS)
// 返回结果示例："/storage/emulated/0/Download"

// 拼接多级路径
val configPath = "MyApp".joinPath("config", "settings.json")
// 返回结果示例："MyApp/config/settings.json"

// 获取缓存子目录并自动创建
val cachePath = context.getSettingsPathV2(
    type = PathType.CACHE,
    subDir = "images",
    autoCreate = true
)
// 返回结果示例："/data/user/0/com.example.app/cache/images
```