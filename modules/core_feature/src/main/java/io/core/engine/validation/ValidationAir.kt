package io.core.engine.validation

/**
 * 验证工具类 - 提供便捷的静态方法
 */
object ValidationAir {

    /**
     * 快速验证多个条件
     */
    fun validateAll(vararg validations: () -> ValidationResult): ValidationResult {
        for (validation in validations) {
            val result = validation()
            if (result.isFailure) {
                return result
            }
        }
        return ValidationResult.Companion.success()
    }

    /**
     * 验证任意一个条件通过即可
     */
    fun validateAny(vararg validations: () -> ValidationResult): ValidationResult {
        val failures = mutableListOf<ValidationResult>()
        for (validation in validations) {
            val result = validation()
            if (result.isValid) {
                return result
            }
            failures.add(result)
        }
        
        val allErrors = failures.joinToString("; ") { it.errorMessage ?: "" }
        return ValidationResult.Companion.failure("所有验证都失败: $allErrors", "ALL_FAILED")
    }

    /**
     * 批量验证对象
     */
    fun <T> validateBatch(
        items: Collection<T>,
        validator: (T) -> ValidationResult
    ): List<ValidationResult> {
        return items.map { validator(it) }
    }

    /**
     * 批量验证对象，返回第一个失败的结果
     */
    fun <T> validateBatchStopOnFailure(
        items: Collection<T>,
        validator: (T) -> ValidationResult
    ): ValidationResult {
        for ((index, item) in items.withIndex()) {
            val result = validator(item)
            if (result.isFailure) {
                return ValidationResult.Companion.failure(
                    "第${index + 1}项验证失败: ${result.errorMessage}",
                    result.errorCode
                )
            }
        }
        return ValidationResult.Companion.success()
    }

    /**
     * 条件验证 - 只有满足条件时才验证
     */
    fun <T> validateIf(
        value: T,
        condition: (T) -> Boolean,
        validator: (T) -> ValidationResult
    ): ValidationResult {
        return if (condition(value)) {
            validator(value)
        } else {
            ValidationResult.Companion.success()
        }
    }

    /**
     * 创建简单的验证规则
     */
    fun <T> createRule(
        predicate: (T) -> Boolean,
        errorMessage: String,
        errorCode: String? = null
    ): ValidationRule<T> {
        return ValidationRule { value ->
            if (predicate(value)) {
                ValidationResult.Companion.success()
            } else {
                ValidationResult.Companion.failure(errorMessage, errorCode)
            }
        }
    }

    /**
     * 组合多个验证规则
     */
    fun <T> combineRules(
        vararg rules: ValidationRule<T>,
        stopOnFirstFailure: Boolean = true
    ): ValidationRule<T> {
        return CompositeValidationRule(rules.toList(), stopOnFirstFailure)
    }
}