package io.core.engine.validation

/**
 * 验证�?- 核心验证框架�?
 * 支持链式调用，灵活配置验证规�?
 * @param T 被验证数据的类型
 */
class ValidationChain<T> private constructor(
    private val value: T,
    private val rules: MutableList<ValidationRule<T>> = mutableListOf(),
    private val stopOnFirstFailure: Boolean = true
) {
    
    companion object {
        /**
         * 创建验证�?
         * @param value 待验证的�?
         * @param stopOnFirstFailure 是否在第一个失败时停止验证
         */
        fun <T> of(value: T, stopOnFirstFailure: Boolean = true): ValidationChain<T> {
            return ValidationChain(value, mutableListOf(), stopOnFirstFailure)
        }
    }

    /**
     * 添加验证规则
     */
    fun addRule(rule: ValidationRule<T>): ValidationChain<T> {
        rules.add(rule)
        return this
    }

    /**
     * 添加条件验证规则
     */
    fun addRuleIf(condition: (T) -> Boolean, rule: ValidationRule<T>): ValidationChain<T> {
        rules.add(ConditionalValidationRule(condition, rule))
        return this
    }

    /**
     * 添加自定义验证逻辑
     */
    fun addCustomRule(validator: (T) -> ValidationResult): ValidationChain<T> {
        rules.add(ValidationRule { validator(it) })
        return this
    }

    /**
     * 添加自定义验证逻辑（简化版�?
     */
    fun addCustomRule(
        validator: (T) -> Boolean,
        errorMessage: String,
        errorCode: String? = null
    ): ValidationChain<T> {
        rules.add(ValidationRule { value ->
            if (validator(value)) {
                ValidationResult.Companion.success()
            } else {
                ValidationResult.Companion.failure(errorMessage, errorCode)
            }
        })
        return this
    }

    /**
     * 执行所有验证规�?
     */
    fun validate(): ValidationResult {
        return CompositeValidationRule(rules, stopOnFirstFailure).validate(value)
    }

    /**
     * 执行验证并在失败时执行回�?
     */
    fun validateAndHandle(
        onSuccess: ((T) -> Unit)? = null,
        onFailure: ((ValidationResult) -> Unit)? = null
    ): ValidationResult {
        val result = validate()
        if (result.isValid) {
            onSuccess?.invoke(value)
        } else {
            onFailure?.invoke(result)
        }
        return result
    }

    /**
     * 执行验证并返回布尔�?
     */
    fun isValid(): Boolean = validate().isValid

    /**
     * 获取第一个错误信�?
     */
    fun getFirstError(): String? = validate().errorMessage
}