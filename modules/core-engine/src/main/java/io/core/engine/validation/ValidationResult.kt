package io.core.engine.validation

/**
 * 验证结果封装�?
 * @param isValid 是否验证通过
 * @param errorMessage 错误信息
 * @param errorCode 错误代码
 * @param data 附加数据
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val data: Any? = null
) {
    companion object {
        /**
         * 创建成功结果
         */
        fun success(data: Any? = null): ValidationResult {
            return ValidationResult(true, null, null, data)
        }

        /**
         * 创建失败结果
         */
        fun failure(
            errorMessage: String,
            errorCode: String? = null,
            data: Any? = null
        ): ValidationResult {
            return ValidationResult(false, errorMessage, errorCode, data)
        }
    }

    /**
     * 是否失败
     */
    val isFailure: Boolean get() = !isValid

    /**
     * 获取错误信息，如果验证通过则返回空字符�?
     */
    fun getErrorMessageOrEmpty(): String = errorMessage ?: ""
}