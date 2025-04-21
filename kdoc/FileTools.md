# FileTools 文件工具类

## 概述
提供跨平台文件操作工具方法，包含文件创建/删除/复制、目录管理、文件读写等功能，支持路径规范化处理及EBUSY异常修复。

## 核心方法

### 文件基础操作
```kotlin
@JvmStatic
fun createFolderIfNotExist(filePath: String): File
```
- 功能：创建目录（包含多级目录）
- 实现特点：
  1. 自动处理路径规范化
  2. 幂等设计（已存在不重复创建）

```kotlin
@JvmStatic
@Synchronized
fun createFileIfNotExist(filePath: String): File
```
- 新增说明：
  1. 同步锁保证多线程安全
  2. 自动捕获IO异常并调试日志输出

### 文件读写操作
```kotlin
@Throws(IOException::class)
fun readFromAssets(fileName: String): String
```
- 资源加载流程：
  1. 使用AssetManager打开资源
  2. 缓冲读取优化性能
  3. 自动资源关闭管理

```kotlin
fun writeInputStream(file: File, data: InputStream): Boolean
```
- 实现特性：
  1. 支持大文件流式写入
  2. 自动创建父目录
  3. 异常安全的事务处理

### 列表管理
```kotlin
@JvmOverloads
fun listDirs(startDirPath: String, excludeDirs: Array<String>? = null, sortType: Int): Array<File>
```
- 参数说明：
  1. excludeDirs：排除目录名数组
  2. sortType：支持8种排序方式

```kotlin
@JvmOverloads
fun listFiles(startDirPath: String, filterPattern: Pattern? = null, sortType: Int): Array<File>
```
- 高级过滤：
  1. 正则表达式文件名匹配
  2. 多维度排序支持

### 路径处理
```kotlin
fun separator(path: String): String
```
- 规范化过程：
  1. 统一系统分隔符
  2. 自动补全路径结尾分隔符

```kotlin
fun getNameExcludeExtension(path: String): String
```
- 特殊处理：
  1. 支持多扩展名文件（如.tar.gz）
  2. 空安全处理

## 最佳实践
### 安全删除模式
```kotlin
FileTools.delete(file) {
    onSuccess { /* 清理缓存等后续操作 */ }
    onError { ex -> logger.error("删除失败", ex) }
}
```

### 高效目录遍历
```kotlin
val files = FileTools.listFiles("/data", Pattern.compile("^log_.*\\.txt"), 
    FileTools.BY_SIZE_DESC)
```

### 流式文件操作
```kotlin
FileTools.writeInputStream(inputStream) {
    progress = { pct -> updateProgress(pct) }
    bufferSize = 8192
}
```
```kotlin
@JvmStatic
fun createFileIfNotExist(root: File, vararg subDirFiles: String): File
```
- 功能：递归创建多级目录文件
- 实现原理：
  1. 自动处理路径分隔符兼容性
  2. 父目录不存在时自动创建
  3. 同步锁保证线程安全

### 异常安全删除
```kotlin
@JvmStatic
fun delete(file: File, deleteRootDir: Boolean = false): Boolean
```
- EBUSY解决方案：
  1. 重命名目标文件/目录
  2. 延迟删除操作
  3. 递归删除子内容

### 路径规范化
```kotlin
@JvmStatic
fun separator(path: String): String
```
- 跨平台特性：
  1. 统一为系统分隔符
  2. 自动补全结尾分隔符
  3. 支持路径层级自动合并

## 扩展功能

### 文件筛选排序
```kotlin
@IntDef(BY_NAME_ASC, BY_NAME_DESC, /*...*/)
fun listFiles(startDirPath: String, filterPattern: Pattern? = null, @SortType sortType: Int)
```
- 排序维度：
  1. 文件名（中英文混合排序）
  2. 修改时间
  3. 文件大小
  4. 扩展名

### MIME类型识别
```kotlin
@JvmStatic
fun getMimeType(pathOrUrl: String): String
```
- 实现方案：
  1. 基于Android MimeTypeMap
  2. 扩展名自动提取
  3. 默认返回application/octet-stream

## 最佳实践
- 使用`createFileIfNotExist`替代手动路径拼接
- 优先使用`delete`方法替代原生File.delete()
- 目录遍历配合SortType参数实现高效文件管理

> 注意：所有方法均使用@JvmStatic注解支持Java调用