# ReflectHelper 反射工具使用指南

## 快速入门
```kotlin
// 初始化反射上下文
val helper = ReflectHelper.on("com.example.MyClass")

// 链式调用示例
val result = helper.newInstance("param1", 2)
    .chainInvoke("methodA", 3.14)
    .setField("fieldName", "value")
    .get<String>()
```

## 核心API
### 1. 对象初始化
- `on(className)`：通过类名初始化
- `with(instance)`：通过对象实例初始化

### 2. 方法操作
- `getMethod()` 支持精确参数类型匹配
- `chainInvoke()` 实现链式调用，自动处理返回值上下文

## 类型处理机制
自动识别以下基本类型转换：
| 基本类型 | 包装类型 |
|---------|---------|
| Int     | Integer |
| Double  | Double  |
| ...     | ...     |

## 缓存策略
- 方法/字段缓存使用LRU策略
- 建议单个Helper实例不要跨线程使用

## 异常处理
```kotlin
try {
    helper.getMethod("nonExistMethod")
} catch (e: NoSuchMethodException) {
    // 处理找不到方法的情况
}
```