package io.core.engine.validation.rules

import io.core.engine.validation.ValidationResult
import io.core.engine.validation.ValidationRule


/**
 * 数字验证规则集合
 */
object NumberValidationRules {

    /**
     * 范围验证
     */
    fun <T : Comparable<T>> range(
        min: T? = null,
        max: T? = null,
        errorMessage: String? = null
    ): ValidationRule<T?> {
        return ValidationRule { value ->
            if (value == null) {
                ValidationResult.success()
            } else {
                val actualMessage = errorMessage ?: when {
                    min != null && max != null -> "值必须在${min}到${max}之间"
                    min != null -> "值不能小于${min}"
                    max != null -> "值不能大于${max}"
                    else -> "值不在有效范围内"
                }

                when {
                    min != null && value < min -> ValidationResult.failure(actualMessage, "VALUE_TOO_SMALL")
                    max != null && value > max -> ValidationResult.failure(actualMessage, "VALUE_TOO_LARGE")
                    else -> ValidationResult.success()
                }
            }
        }
    }

    /**
     * 正数验证
     */
    fun <T : Number> positive(errorMessage: String = "必须是正数"): ValidationRule<T?> {
        return ValidationRule { value ->
            if (value == null) {
                ValidationResult.success()
            } else {
                if (value.toDouble() > 0) {
                    ValidationResult.success()
                } else {
                    ValidationResult.failure(errorMessage, "NOT_POSITIVE")
                }
            }
        }
    }

    /**
     * 非负数验证
     */
    fun <T : Number> nonNegative(errorMessage: String = "不能是负数"): ValidationRule<T?> {
        return ValidationRule { value ->
            if (value == null) {
                ValidationResult.success()
            } else {
                if (value.toDouble() >= 0) {
                    ValidationResult.success()
                } else {
                    ValidationResult.failure(errorMessage, "NEGATIVE")
                }
            }
        }
    }

    /**
     * 整数验证
     */
    fun integer(errorMessage: String = "必须是整数"): ValidationRule<String?> {
        return ValidationRule { value ->
            if (value == null) {
                ValidationResult.success()
            } else {
                try {
                    value.toInt()
                    ValidationResult.success()
                } catch (e: NumberFormatException) {
                    ValidationResult.failure(errorMessage, "NOT_INTEGER")
                }
            }
        }
    }

    /**
     * 小数位数验证
     */
    fun decimalPlaces(
        maxPlaces: Int,
        errorMessage: String? = null
    ): ValidationRule<Double?> {
        return ValidationRule { value ->
            if (value == null) {
                ValidationResult.success()
            } else {
                val valueStr = value.toString()
                val decimalIndex = valueStr.indexOf('.')
                if (decimalIndex == -1) {
                    ValidationResult.success()
                } else {
                    val actualPlaces = valueStr.length - decimalIndex - 1
                    if (actualPlaces <= maxPlaces) {
                        ValidationResult.success()
                    } else {
                        val message = errorMessage ?: "小数位数不能超过${maxPlaces}位"
                        ValidationResult.failure(message, "TOO_MANY_DECIMAL_PLACES")
                    }
                }
            }
        }
    }
}