package io.core.engine.validation

import io.core.engine.validation.rules.CollectionValidationRules
import io.core.engine.validation.rules.NumberValidationRules
import io.core.engine.validation.rules.StringValidationRules


/**
 * 验证扩展函数，提供更便捷的使用方式
 */

/**
 * 字符串验证扩展
 */
fun String?.valid(stopOnFirstFailure: Boolean = true): ValidationChain<String?> {
    return ValidationChain.Companion.of(this, stopOnFirstFailure)
}

/**
 * 数字验证扩展
 */
fun <T : Number> T?.valid(stopOnFirstFailure: Boolean = true): ValidationChain<T?> {
    return ValidationChain.Companion.of(this, stopOnFirstFailure)
}

/**
 * 集合验证扩展
 */
fun <T> Collection<T>?.valid(stopOnFirstFailure: Boolean = true): ValidationChain<Collection<T>?> {
    return ValidationChain.Companion.of(this, stopOnFirstFailure)
}

/**
 * 任意对象验证扩展
 */
fun <T> T.valid(stopOnFirstFailure: Boolean = true): ValidationChain<T> {
    return ValidationChain.Companion.of(this, stopOnFirstFailure)
}

/**
 * 字符串验证链扩展方法
 */
fun ValidationChain<String?>.notEmpty(errorMessage: String = "内容不能为空"): ValidationChain<String?> {
    return addRule(StringValidationRules.notEmpty(errorMessage))
}

fun ValidationChain<String?>.notBlank(errorMessage: String = "内容不能为空白"): ValidationChain<String?> {
    return addRule(StringValidationRules.notBlank(errorMessage))
}

fun ValidationChain<String?>.length(
    min: Int? = null,
    max: Int? = null,
    errorMessage: String? = null
): ValidationChain<String?> {
    return addRule(StringValidationRules.length(min, max, errorMessage))
}

fun ValidationChain<String?>.regex(
    pattern: String,
    errorMessage: String = "格式不正确"
): ValidationChain<String?> {
    return addRule(StringValidationRules.regex(pattern, errorMessage))
}

fun ValidationChain<String?>.notContainsForbiddenWords(
    forbiddenWords: List<String>,
    errorMessage: String = "包含禁止使用的词汇"
): ValidationChain<String?> {
    return addRule(StringValidationRules.notContainsForbiddenWords(forbiddenWords, errorMessage))
}

fun ValidationChain<String?>.email(errorMessage: String = "邮箱格式不正确"): ValidationChain<String?> {
    return addRule(StringValidationRules.email(errorMessage))
}

fun ValidationChain<String?>.phoneNumber(errorMessage: String = "手机号格式不正确"): ValidationChain<String?> {
    return addRule(StringValidationRules.phoneNumber(errorMessage))
}

fun ValidationChain<String?>.passwordStrength(
    minLength: Int = 8,
    requireUppercase: Boolean = true,
    requireLowercase: Boolean = true,
    requireDigit: Boolean = true,
    requireSpecialChar: Boolean = true,
    errorMessage: String? = null
): ValidationChain<String?> {
    return addRule(StringValidationRules.passwordStrength(
        minLength, requireUppercase, requireLowercase, requireDigit, requireSpecialChar, errorMessage
    ))
}

/**
 * 数字验证链扩展方法
 */
fun <T : Comparable<T>> ValidationChain<T?>.range(
    min: T? = null,
    max: T? = null,
    errorMessage: String? = null
): ValidationChain<T?> {
    return addRule(NumberValidationRules.range(min, max, errorMessage))
}

fun <T : Number> ValidationChain<T?>.positive(errorMessage: String = "必须是正数"): ValidationChain<T?> {
    return addRule(NumberValidationRules.positive(errorMessage))
}

fun <T : Number> ValidationChain<T?>.nonNegative(errorMessage: String = "不能是负数"): ValidationChain<T?> {
    return addRule(NumberValidationRules.nonNegative(errorMessage))
}

/**
 * 集合验证链扩展方法
 */
@JvmName("collectionNotEmpty")
fun <T> ValidationChain<Collection<T>?>.notEmpty(errorMessage: String = "列表不能为空"): ValidationChain<Collection<T>?> {
    return addRule(CollectionValidationRules.notEmpty(errorMessage))
}

fun <T> ValidationChain<Collection<T>?>.size(
    min: Int? = null,
    max: Int? = null,
    errorMessage: String? = null
): ValidationChain<Collection<T>?> {
    return addRule(CollectionValidationRules.size(min, max, errorMessage))
}

fun <T> ValidationChain<Collection<T>?>.unique(errorMessage: String = "列表中存在重复元素"): ValidationChain<Collection<T>?> {
    return addRule(CollectionValidationRules.unique(errorMessage))
}