# FileType 文件类型工具类

## 类功能
提供文件扩展名与MIME类型的双向映射能力，支持Android系统MIME类型自动适配。

## 核心方法

### getMimeType(extension: String)
```kotlin
// 基本用法
val mime = FileType.getMimeType("jpg") // 返回 "image/jpeg"

// 处理未知扩展名
val unknownMime = FileType.getMimeType(".xyz") // 回退系统API，最后返回"*/*"

// 根据文件路径获取
val pathMime = FileType.getMimeTypeFromFile("/data/files/report.pdf") // 返回 "application/pdf"
```

### getExtensionsByMimeType(mimeType: String)
```kotlin
// 获取video/mp4对应的扩展名
FileType.getExtensionsByMimeType("video/mp4") // 返回 [".mp4", ".mpg4"]
```

## MIME类型映射表
| 文件扩展名      | MIME类型                                                            | 分类    |
|------------|-------------------------------------------------------------------|-------|
| .bmp       | image/bmp                                                         | 图片    |
| .gif       | image/gif                                                         | 图片    |
| .heic      | image/heic                                                        | 图片    |
| .ico       | image/x-icon                                                      | 图片    |
| .jpeg/.jpg | image/jpeg                                                        | 图片    |
| .png       | image/png                                                         | 图片    |
| .webp      | image/webp                                                        | 图片    |
| .3gp       | video/3gpp                                                        | 视频    |
| .mp4       | video/mp4                                                         | 视频    |
| .avi       | video/x-msvideo                                                   | 视频    |
| .mov       | video/quicktime                                                   | 视频    |
| .pdf       | application/pdf                                                   | 文档    |
| .docx      | application/msword                                                | 文档    |
| .xlsx      | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet | 文档    |
| .zip       | application/zip                                                   | 压缩文件  |
| .7z        | application/x-7z-compressed                                       | 压缩文件  |
| .apk       | application/vnd.android.package-archive                           | 可执行文件 |
| .jar       | application/java-archive                                          | 可执行文件 |
| .js        | application/x-javascript                                          | 编程代码  |
| .json      | application/json                                                  | 编程代码  |
| .html      | text/html                                                         | 网页    |
| .css       | text/css                                                          | 网页    |

完整包含87种扩展名映射，覆盖以下9大类文件类型：
- 图片（14种）
- 视频（13种）
- 音频（12种）
- 文档（10种）
- 压缩文件（8种）
- 可执行文件（5种）
- 编程代码（19种）
- 网页文件（4种）
- 其他类型（12种）

## 典型场景
1. **文件上传校验**：通过扩展名验证允许上传的文件类型
2. **文件预览处理**：根据MIME类型选择正确的预览方式
3. **系统兼容处理**：当系统API无法识别新扩展名时自动回退自定义配置

## 版本记录
- v1.2 (2025/1/4)：新增HEIC/WebP等新型图片格式支持
- v1.1 (2024/12/15)：优化压缩文件类型检测逻辑