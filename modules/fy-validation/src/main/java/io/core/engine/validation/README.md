# Android 验证框架 (Validation Framework)

一个功能强大、易于使用的 Android 验证框架，支持链式调用、灵活配置和扩展。适用于各种验证场景，如表单验证、业务逻辑验证等。

## 特性

- 🔗 **链式调用**: 支持流畅的链式验证语法
- 🎯 **类型安全**: 完全基于 Kotlin 类型系统
- 🔧 **高度可扩展**: 易于添加自定义验证规则
- 📦 **内置规则**: 提供丰富的常用验证规则
- 🚀 **高性能**: 支持条件验证和早期退出
- 🧪 **完全测试**: 包含完整的单元测试
- 📱 **Android 兼容**: 可直接在 Java Android 项目中使用

## 快速开始

### 基本用法

```kotlin
// 字符串验证
val result = "user@example.com".valid()
    .notBlank("邮箱不能为空")
    .email("邮箱格式不正确")
    .validate()

if (result.isValid) {
    // 验证通过
} else {
    // 显示错误信息
    showError(result.errorMessage)
}
```

### 复杂验证示例

```kotlin
// 用户注册验证
fun validateUserRegistration(username: String?, email: String?, password: String?) {
    val usernameResult = username.valid()
        .notBlank("用户名不能为空")
        .length(3, 20, "用户名长度必须在3-20个字符之间")
        .regex("^[a-zA-Z0-9_]+$", "用户名只能包含字母、数字和下划线")
        .notContainsForbiddenWords(listOf("admin", "root"), "用户名包含禁止词汇")
        .validate()
    
    val emailResult = email.valid()
        .notBlank("邮箱不能为空")
        .email("邮箱格式不正确")
        .validate()
    
    val passwordResult = password.valid()
        .notBlank("密码不能为空")
        .passwordStrength(8, true, true, true, true)
        .validate()
    
    // 处理验证结果...
}
```

## 核心组件

### ValidationChain
验证链是框架的核心，支持链式调用和灵活配置：

```kotlin
val result = value.valid(stopOnFirstFailure = true)
    .addRule(customRule)
    .addCustomRule({ it.isNotEmpty() }, "不能为空")
    .addRuleIf(condition, conditionalRule)
    .validate()
```

### ValidationResult
验证结果封装类：

```kotlin
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?,
    val errorCode: String?,
    val data: Any?
)
```

### 内置验证规则

#### 字符串验证
- `notEmpty()` - 非空验证
- `notBlank()` - 非空白验证
- `length(min, max)` - 长度验证
- `regex(pattern)` - 正则表达式验证
- `email()` - 邮箱格式验证
- `phoneNumber()` - 手机号验证
- `idCard()` - 身份证号验证
- `passwordStrength()` - 密码强度验证
- `notContainsForbiddenWords()` - 禁词验证

#### 数字验证
- `range(min, max)` - 范围验证
- `positive()` - 正数验证
- `nonNegative()` - 非负数验证
- `integer()` - 整数验证
- `decimalPlaces(max)` - 小数位数验证

#### 集合验证
- `notEmpty()` - 非空集合验证
- `size(min, max)` - 集合大小验证
- `unique()` - 唯一性验证
- `contains(element)` - 包含元素验证
- `allElementsValid(rule)` - 所有元素验证

## 高级用法

### 条件验证
```kotlin
val result = userType.valid()
    .addRuleIf(
        { it == "company" },
        ValidationRule { 
            companyName.valid()
                .notBlank("企业用户必须填写公司名称")
                .validate()
        }
    )
    .validate()
```

### 自定义验证规则
```kotlin
val customRule = ValidationRule<String> { value ->
    if (value.contains("custom")) {
        ValidationResult.success()
    } else {
        ValidationResult.failure("必须包含custom")
    }
}

val result = "test".valid()
    .addRule(customRule)
    .validate()
```

### 批量验证
```kotlin
// 验证所有条件
val result = ValidationAir.validateAll(
    { validation1() },
    { validation2() },
    { validation3() }
)

// 任意一个通过即可
val result = ValidationAir.validateAny(
    { validation1() },
    { validation2() }
)
```

### 验证回调处理
```kotlin
val result = input.valid()
    .notBlank()
    .length(5, 20)
    .validateAndHandle(
        onSuccess = { value ->
            // 验证成功处理
            processValidInput(value)
        },
        onFailure = { result ->
            // 验证失败处理
            showError(result.errorMessage)
        }
    )
```

## 在 Java 中使用

框架完全兼容 Java，可以直接在 Java Android 项目中使用：

```java
// Java 中使用
ValidationResult result = ValidationKt.valid("test@example.com", true)
    .notBlank("邮箱不能为空")
    .email("邮箱格式不正确")
    .validate();

if (result.isValid()) {
    // 验证通过
} else {
    // 显示错误
    showError(result.getErrorMessage());
}
```

## 实际应用场景

### 1. 表单验证
```kotlin
fun validateLoginForm(username: String?, password: String?): ValidationResult {
    return ValidationAir.validateAll(
        { username.valid().notBlank("用户名不能为空").validate() },
        { password.valid().notBlank("密码不能为空").validate() }
    )
}
```

### 2. 业务逻辑验证
```kotlin
fun validateOrderSubmission(order: Order): ValidationResult {
    return ValidationChain.of(order)
        .addCustomRule({ it.items.isNotEmpty() }, "订单不能为空")
        .addCustomRule({ it.totalAmount > 0 }, "订单金额必须大于0")
        .addCustomRule({ it.shippingAddress.isNotBlank() }, "收货地址不能为空")
        .validate()
}
```

### 3. 文件上传验证
```kotlin
fun validateFileUpload(file: File?): ValidationResult {
    return ValidationChain.of(file)
        .addCustomRule({ it != null }, "请选择文件")
        .addCustomRule({ it?.length() ?: 0 <= MAX_FILE_SIZE }, "文件大小超出限制")
        .addCustomRule({ 
            val extension = it?.extension?.lowercase()
            extension in listOf("jpg", "png", "gif")
        }, "不支持的文件格式")
        .validate()
}
```

## 性能优化

- **早期退出**: 默认在第一个验证失败时停止，提高性能
- **条件验证**: 只在满足条件时执行验证，避免不必要的计算
- **延迟计算**: 验证规则只在需要时才执行

## 扩展开发

### 添加自定义验证规则
```kotlin
object CustomValidationRules {
    fun customRule(errorMessage: String = "自定义验证失败"): ValidationRule<String?> {
        return ValidationRule { value ->
            // 自定义验证逻辑
            if (customLogic(value)) {
                ValidationResult.success()
            } else {
                ValidationResult.failure(errorMessage, "CUSTOM_ERROR")
            }
        }
    }
}

// 添加扩展方法
fun ValidationChain<String?>.customRule(errorMessage: String = "自定义验证失败"): ValidationChain<String?> {
    return addRule(CustomValidationRules.customRule(errorMessage))
}
```

## 测试

框架包含完整的单元测试，确保代码质量和稳定性。运行测试：

```bash
./gradlew test
```

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

## 更新日志

### v1.0.0
- 初始版本发布
- 支持基本的验证功能
- 包含常用验证规则
- 完整的测试覆盖