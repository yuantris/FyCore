package io.core.engine.validation

/**
 * 验证规则接口
 * @param T 被验证数据的类型
 */
fun interface ValidationRule<T> {
    /**
     * 执行验证
     * @param value 待验证的�?
     * @return 验证结果
     */
    fun validate(value: T): ValidationResult
}

/**
 * 条件验证规则 - 只有满足条件时才执行验证
 */
class ConditionalValidationRule<T>(
    private val condition: (T) -> Boolean,
    private val rule: ValidationRule<T>
) : ValidationRule<T> {
    override fun validate(value: T): ValidationResult {
        return if (condition(value)) {
            rule.validate(value)
        } else {
            ValidationResult.success()
        }
    }
}

/**
 * 组合验证规则 - 将多个规则组合成一�?
 */
class CompositeValidationRule<T>(
    private val rules: List<ValidationRule<T>>,
    private val stopOnFirstFailure: Boolean = true
) : ValidationRule<T> {
    override fun validate(value: T): ValidationResult {
        val failures = mutableListOf<ValidationResult>()
        
        for (rule in rules) {
            val result = rule.validate(value)
            if (result.isFailure) {
                failures.add(result)
                if (stopOnFirstFailure) {
                    return result
                }
            }
        }
        
        return if (failures.isEmpty()) {
            ValidationResult.success()
        } else {
            // 如果不是遇到第一个失败就停止，则返回所有错误信�?
            val allErrors = failures.joinToString("; ") { it.errorMessage ?: "" }
            ValidationResult.failure(allErrors)
        }
    }
}