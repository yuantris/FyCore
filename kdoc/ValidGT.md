# ValidGT 校验器使用指南

## 基本校验流程
```kotlin
// 创建校验器并添加条件
val result = ValidGT.forFile()
    .excludeHiddenFiles()
    .hasExtension(".jpg", ".png")
    .maxSize(5 * 1024 * 1024) // 5MB
    .build(File("/path/to/file"))

// 处理校验结果
when {
    result.isValid -> println("校验通过")
    else -> println("校验失败: ${result.errors.joinToString()}")
}
```

## 文件校验专项
```kotlin
ValidGT.forFile()
    .addCondition({ it.name.contains("temp") }, "临时文件禁止上传")
    .minSize(1024) // 1KB
    .hasExtension(".pdf")
    .build(pdfFile)
```

## 字符串校验
```kotlin
ValidGT.forString()
    .nonEmpty()
    .matches(Regex("^[A-Za-z0-9]{6,20}$"))
    .build(inputPassword)
```

## 自定义校验规则
```kotlin
// 创建自定义校验器
val ageValidator = ValidGT.create<Int>()
    .addCondition({ it in 18..60 }, "年龄必须在18-60岁之间")

// 数据类对象校验
data class User(val name: String, val age: Int)

ValidGT.create<User>()
    .addCondition({ it.age >= 18 }, "必须年满18周岁")
    .build(User("Alice", 20))

// 使用扩展函数增强
fun ValidGT<Int>.isAdult() = this.addCondition(
    { it >= 18 }, "未满18岁禁止访问"
)
```

## 高级用法

### 组合条件校验
```kotlin
val complexCondition = Predicate<File> { it.name.startsWith("temp") }
    .and(Predicate { it.length() > 1024 })

ValidGT.forFile()
    .addCondition(complexCondition, "文件名必须以temp开头且大于1KB")
    .build(targetFile)
```

## 最佳实践
1. **条件组合**：优先使用预置校验方法，通过`addCondition`补充业务规则
2. **错误信息**：明确错误提示，方便快速定位问题
3. **复用校验器**：对通用规则创建常体验证器
4. **性能优化**：大数据量校验时采用并行处理
5. **扩展机制**：通过扩展函数封装领域特定规则