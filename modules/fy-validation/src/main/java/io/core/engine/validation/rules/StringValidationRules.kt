package io.core.engine.validation.rules

import io.core.engine.validation.ValidationResult
import io.core.engine.validation.ValidationRule
import java.util.regex.Pattern

/**
 * 字符串验证规则集合
 */
object StringValidationRules {

    /**
     * 非空验证
     */
    fun notEmpty(errorMessage: String = "内容不能为空"): ValidationRule<String?> {
        return ValidationRule { value ->
            if (value.isNullOrEmpty()) {
                ValidationResult.failure(errorMessage, "EMPTY")
            } else {
                ValidationResult.success()
            }
        }
    }

    /**
     * 非空白验证
     */
    fun notBlank(errorMessage: String = "内容不能为空白"): ValidationRule<String?> {
        return ValidationRule { value ->
            if (value.isNullOrBlank()) {
                ValidationResult.failure(errorMessage, "BLANK")
            } else {
                ValidationResult.success()
            }
        }
    }

    /**
     * 长度验证
     */
    fun length(
        min: Int? = null,
        max: Int? = null,
        errorMessage: String? = null
    ): ValidationRule<String?> {
        return ValidationRule { value ->
            val length = value?.length ?: 0
            val actualMessage = errorMessage ?: when {
                min != null && max != null -> "长度必须在${min}-${max}个字符之间"
                min != null -> "长度不能少于${min}个字符"
                max != null -> "长度不能超过${max}个字符"
                else -> "长度不符合要求"
            }

            when {
                min != null && length < min -> ValidationResult.failure(actualMessage, "LENGTH_TOO_SHORT")
                max != null && length > max -> ValidationResult.failure(actualMessage, "LENGTH_TOO_LONG")
                else -> ValidationResult.success()
            }
        }
    }

    /**
     * 正则表达式验证
     */
    fun regex(
        pattern: String,
        errorMessage: String = "格式不正确"
    ): ValidationRule<String?> {
        return ValidationRule { value ->
            if (value == null) {
                ValidationResult.success()
            } else {
                if (Pattern.matches(pattern, value)) {
                    ValidationResult.success()
                } else {
                    ValidationResult.failure(errorMessage, "REGEX_MISMATCH")
                }
            }
        }
    }

    /**
     * 包含禁止词验证
     */
    fun notContainsForbiddenWords(
        forbiddenWords: List<String>,
        errorMessage: String = "包含禁止使用的词汇"
    ): ValidationRule<String?> {
        return ValidationRule { value ->
            if (value == null) {
                ValidationResult.success()
            } else {
                val foundWord = forbiddenWords.find { value.contains(it, ignoreCase = true) }
                if (foundWord != null) {
                    ValidationResult.failure("$errorMessage: $foundWord", "FORBIDDEN_WORD")
                } else {
                    ValidationResult.success()
                }
            }
        }
    }

    /**
     * 邮箱格式验证
     */
    fun email(errorMessage: String = "邮箱格式不正确"): ValidationRule<String?> {
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        return regex(emailPattern, errorMessage)
    }

    /**
     * 手机号验证
     */
    fun phoneNumber(errorMessage: String = "手机号格式不正确"): ValidationRule<String?> {
        val phonePattern = "^1[3-9]\\d{9}$"
        return regex(phonePattern, errorMessage)
    }

    /**
     * 身份证号验证
     */
    fun idCard(errorMessage: String = "身份证号格式不正确"): ValidationRule<String?> {
        val idCardPattern = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$"
        return regex(idCardPattern, errorMessage)
    }

    /**
     * 只包含字母和数字
     */
    fun alphanumeric(errorMessage: String = "只能包含字母和数字"): ValidationRule<String?> {
        val pattern = "^[a-zA-Z0-9]+$"
        return regex(pattern, errorMessage)
    }

    /**
     * 密码强度验证
     */
    fun passwordStrength(
        minLength: Int = 8,
        requireUppercase: Boolean = true,
        requireLowercase: Boolean = true,
        requireDigit: Boolean = true,
        requireSpecialChar: Boolean = true,
        errorMessage: String? = null
    ): ValidationRule<String?> {
        return ValidationRule { value ->
            if (value == null) {
                return@ValidationRule ValidationResult.failure("密码不能为空", "PASSWORD_NULL")
            }

            val errors = mutableListOf<String>()

            if (value.length < minLength) {
                errors.add("至少${minLength}位")
            }
            if (requireUppercase && !value.any { it.isUpperCase() }) {
                errors.add("包含大写字母")
            }
            if (requireLowercase && !value.any { it.isLowerCase() }) {
                errors.add("包含小写字母")
            }
            if (requireDigit && !value.any { it.isDigit() }) {
                errors.add("包含数字")
            }
            if (requireSpecialChar && !value.any { !it.isLetterOrDigit() }) {
                errors.add("包含特殊字符")
            }

            if (errors.isEmpty()) {
                ValidationResult.success()
            } else {
                val message = errorMessage ?: "密码必须${errors.joinToString("、")}"
                ValidationResult.failure(message, "PASSWORD_WEAK")
            }
        }
    }
}