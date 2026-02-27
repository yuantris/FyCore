package io.core.engine.validation.examples

import io.core.engine.validation.ValidationChain
import io.core.engine.validation.ValidationRule
import io.core.engine.validation.ValidationAir
import io.core.engine.validation.ValidationResult
import io.core.engine.validation.email
import io.core.engine.validation.length
import io.core.engine.validation.notBlank
import io.core.engine.validation.notContainsForbiddenWords
import io.core.engine.validation.notEmpty
import io.core.engine.validation.passwordStrength
import io.core.engine.validation.positive
import io.core.engine.validation.range
import io.core.engine.validation.regex
import io.core.engine.validation.rules.StringValidationRules
import io.core.engine.validation.size
import io.core.engine.validation.unique
import io.core.engine.validation.valid

/**
 * 验证框架使用示例
 */
object ValidationExamples {

    /**
     * 示例1: 用户注册表单验证
     */
    fun validateUserRegistration(
        username: String?,
        email: String?,
        password: String?,
        confirmPassword: String?
    ): ValidationResult {
        
        // 用户名验�?
        val usernameResult = username.valid()
            .notBlank("用户名不能为�?)
            .length(3, 20, "用户名长度必须在3-20个字符之�?)
            .regex("^[a-zA-Z0-9_]+$", "用户名只能包含字母、数字和下划�?)
            .notContainsForbiddenWords(listOf("admin", "root", "test"), "用户名包含禁止使用的词汇")
            .validate()
        
        if (usernameResult.isFailure) return usernameResult

        // 邮箱验证
        val emailResult = email.valid()
            .notBlank("邮箱不能为空")
            .email("邮箱格式不正�?)
            .validate()
        
        if (emailResult.isFailure) return emailResult

        // 密码验证
        val passwordResult = password.valid()
            .notBlank("密码不能为空")
            .passwordStrength(8, true, true, true, true)
            .validate()
        
        if (passwordResult.isFailure) return passwordResult

        // 确认密码验证
        val confirmPasswordResult = ValidationChain.of(confirmPassword)
            .addCustomRule(
                { it == password },
                "两次输入的密码不一�?
            )
            .validate()
        
        if (confirmPasswordResult.isFailure) return confirmPasswordResult

        return ValidationResult.success("注册信息验证通过")
    }

    /**
     * 示例2: 文章发布验证
     */
    fun validateArticle(
        title: String?,
        content: String?,
        tags: List<String>?
    ): ValidationResult {
        
        return ValidationAir.validateAll(
            // 标题验证
            { 
                title.valid()
                    .notBlank("标题不能为空")
                    .length(5, 100, "标题长度必须�?-100个字符之�?)
                    .notContainsForbiddenWords(listOf("广告", "推广", "色情"), "标题包含违禁词汇")
                    .validate()
            },
            // 内容验证
            {
                content.valid()
                    .notBlank("内容不能为空")
                    .length(50, 10000, "内容长度必须�?0-10000个字符之�?)
                    .validate()
            },
            // 标签验证
            {
                tags.valid()
                    .notEmpty("至少需要一个标�?)
                    .size(1, 5, "标签数量必须�?-5个之�?)
                    .unique("标签不能重复")
                    .validate()
            }
        )
    }

    /**
     * 示例3: 商品价格验证
     */
    fun validateProductPrice(
        price: Double?,
        discountPrice: Double?
    ): ValidationResult {
        
        // 原价验证
        val priceResult = price.valid()
            .addCustomRule({ it != null }, "价格不能为空")
            .positive("价格必须大于0")
            .range(0.01, 999999.99, "价格必须�?.01-999999.99之间")
            .validate()
        
        if (priceResult.isFailure) return priceResult

        // 折扣价验证（可选）
        if (discountPrice != null) {
            val discountResult = discountPrice.valid()
                .positive("折扣价必须大�?")
                .addCustomRule(
                    { price != null && it!! < price },
                    "折扣价必须小于原�?
                )
                .validate()
            
            if (discountResult.isFailure) return discountResult
        }

        return ValidationResult.success()
    }

    /**
     * 示例4: 复杂业务逻辑验证
     */
    fun validateOrderSubmission(
        userId: String?,
        items: List<OrderItem>?,
        shippingAddress: String?,
        paymentMethod: String?
    ): ValidationResult {
        
        return ValidationChain.of(Unit)
            // 用户ID验证
            .addCustomRule(
                { userId?.isNotBlank() == true },
                "用户ID不能为空"
            )
            // 订单项验�?
            .addCustomRule(
                { items?.isNotEmpty() == true },
                "订单不能为空"
            )
            .addCustomRule(
                { 
                    items?.all { it.quantity > 0 && it.price > 0 } == true
                },
                "订单项数据无�?
            )
            // 收货地址验证
            .addCustomRule(
                { shippingAddress?.length ?: 0 >= 10 },
                "收货地址至少需�?0个字�?
            )
            // 支付方式验证
            .addCustomRule(
                { paymentMethod in listOf("alipay", "wechat", "card") },
                "不支持的支付方式"
            )
            // 业务规则验证
            .addCustomRule(
                { 
                    val totalAmount = items?.sumOf { it.price * it.quantity } ?: 0.0
                    totalAmount >= 0.01
                },
                "订单总金额必须大�?.01�?
            )
            .validate()
    }

    /**
     * 示例5: 条件验证
     */
    fun validateUserProfile(
        userType: String,
        companyName: String?,
        personalId: String?
    ): ValidationResult {
        
        return ValidationChain.of(userType)
            .addCustomRule({ it in listOf("personal", "company") }, "用户类型无效")
            // 企业用户需要验证公司名�?
            .addRuleIf(
                { it == "company" },
                ValidationRule { 
                    companyName.valid()
                        .notBlank("企业用户必须填写公司名称")
                        .length(2, 50, "公司名称长度必须�?-50个字符之�?)
                        .validate()
                }
            )
            // 个人用户需要验证身份证
            .addRuleIf(
                { it == "personal" },
                ValidationRule {
                    personalId.valid()
                        .notBlank("个人用户必须填写身份证号")
                        .addRule(StringValidationRules.idCard())
                        .validate()
                }
            )
            .validate()
    }

    // 订单项数据类
    data class OrderItem(
        val productId: String,
        val quantity: Int,
        val price: Double
    )
}