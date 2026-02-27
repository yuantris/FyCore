package io.core.engine.validation.rules

import io.core.engine.validation.ValidationResult
import io.core.engine.validation.ValidationRule


/**
 * 集合验证规则集合
 */
object CollectionValidationRules {

    /**
     * 非空集合验证
     */
    fun <T> notEmpty(errorMessage: String = "列表不能为空"): ValidationRule<Collection<T>?> {
        return ValidationRule { value ->
            if (value.isNullOrEmpty()) {
                ValidationResult.failure(errorMessage, "COLLECTION_EMPTY")
            } else {
                ValidationResult.success()
            }
        }
    }

    /**
     * 集合大小验证
     */
    fun <T> size(
        min: Int? = null,
        max: Int? = null,
        errorMessage: String? = null
    ): ValidationRule<Collection<T>?> {
        return ValidationRule { value ->
            val size = value?.size ?: 0
            val actualMessage = errorMessage ?: when {
                min != null && max != null -> "列表大小必须�?{min}-${max}之间"
                min != null -> "列表至少需�?{min}个元�?
                max != null -> "列表最多只能有${max}个元�?
                else -> "列表大小不符合要�?
            }

            when {
                min != null && size < min -> ValidationResult.failure(actualMessage, "SIZE_TOO_SMALL")
                max != null && size > max -> ValidationResult.failure(actualMessage, "SIZE_TOO_LARGE")
                else -> ValidationResult.success()
            }
        }
    }

    /**
     * 集合元素验证
     */
    fun <T> allElementsValid(
        elementRule: ValidationRule<T>,
        errorMessage: String = "列表中存在无效元�?
    ): ValidationRule<Collection<T>?> {
        return ValidationRule { value ->
            if (value == null) {
                ValidationResult.success()
            } else {
                for ((index, element) in value.withIndex()) {
                    val result = elementRule.validate(element)
                    if (result.isFailure) {
                        return@ValidationRule ValidationResult.failure(
                            "�?{index + 1}个元�?{result.errorMessage}",
                            "INVALID_ELEMENT"
                        )
                    }
                }
                ValidationResult.success()
            }
        }
    }

    /**
     * 集合唯一性验�?
     */
    fun <T> unique(errorMessage: String = "列表中存在重复元�?): ValidationRule<Collection<T>?> {
        return ValidationRule { value ->
            if (value == null) {
                ValidationResult.success()
            } else {
                val uniqueElements = value.toSet()
                if (uniqueElements.size == value.size) {
                    ValidationResult.success()
                } else {
                    ValidationResult.failure(errorMessage, "DUPLICATE_ELEMENTS")
                }
            }
        }
    }

    /**
     * 包含指定元素验证
     */
    fun <T> contains(
        requiredElement: T,
        errorMessage: String? = null
    ): ValidationRule<Collection<T>?> {
        return ValidationRule { value ->
            if (value == null) {
                ValidationResult.failure("列表不能为空", "COLLECTION_NULL")
            } else {
                if (value.contains(requiredElement)) {
                    ValidationResult.success()
                } else {
                    val message = errorMessage ?: "列表必须包含元素: $requiredElement"
                    ValidationResult.failure(message, "MISSING_ELEMENT")
                }
            }
        }
    }

    /**
     * 不包含指定元素验�?
     */
    fun <T> notContains(
        forbiddenElement: T,
        errorMessage: String? = null
    ): ValidationRule<Collection<T>?> {
        return ValidationRule { value ->
            if (value == null) {
                ValidationResult.success()
            } else {
                if (!value.contains(forbiddenElement)) {
                    ValidationResult.success()
                } else {
                    val message = errorMessage ?: "列表不能包含元素: $forbiddenElement"
                    ValidationResult.failure(message, "FORBIDDEN_ELEMENT")
                }
            }
        }
    }
}