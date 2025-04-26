package io.core.common.helper.valid

import java.io.File
import java.util.function.Predicate

class ValidGT<T> private constructor() {
    private data class Condition<T>(
        val predicate: Predicate<T>,
        val errorMessage: String
    )

    private val conditions: MutableList<Condition<T>> = mutableListOf()

    /**
     * 添加带错误信息的校验条件
     */
    fun addCondition(
        predicate: Predicate<T>,
        errorMessage: String
    ): ValidGT<T> {
        conditions.add(Condition(predicate, errorMessage))
        return this
    }

    /**
     * 执行校验并返回详细结果
     */
    fun build(input: T): ValidationResult {
        val errors = conditions.filterNot { it.predicate.test(input) }
            .map { it.errorMessage }
        return ValidationResult(errors.isEmpty(), errors)
    }

    companion object {
        /**
         * 创建通用校验器
         */
        fun <T> create(): ValidGT<T> = ValidGT()

        /**
         * 创建文件专用校验器（预置基础条件）
         */
        fun forFile(): ValidGT<File> = create<File>()
            .addCondition({ it.exists() }, "文件不存在")
            .addCondition({ it.isFile }, "不是文件类型")
            .addCondition({ it.length() > 0 }, "文件内容为空")
            .addCondition({ it.lastModified() != 0L }, "文件时间戳无效")
    }
}

/**
 * 校验结果数据类
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

/* 文件校验扩展函数 */
/**
 * 排除隐藏文件/路径
 */
fun ValidGT<File>.excludeHiddenFiles(): ValidGT<File> = this.addCondition(
    { !it.containsHiddenPart() },
    "文件路径包含隐藏目录或文件"
)

/**
 * 添加文件扩展名校验
 */
fun ValidGT<File>.hasExtension(vararg extensions: String): ValidGT<File> = this.addCondition(
    { file -> extensions.any { file.name.endsWith(it) } },
    "文件扩展名必须为: ${extensions.joinToString()}"
)

/**
 * 添加最大文件大小限制
 */
fun ValidGT<File>.maxSize(maxBytes: Long): ValidGT<File> = this.addCondition(
    { it.length() <= maxBytes },
    "文件大小超过限制 (最大 ${maxBytes}bytes)"
)

/**
 * 添加最小文件大小限制
 */
fun ValidGT<File>.minSize(minBytes: Long): ValidGT<File> = this.addCondition(
    { it.length() >= minBytes },
    "文件大小不足 (最小 ${minBytes}bytes)"
)

/* 隐藏文件检查工具方法 */
private fun File.containsHiddenPart(): Boolean {
    var currentFile: File? = this
    while (currentFile != null) {
        if (currentFile.name.startsWith(".")) return true
        currentFile = currentFile.parentFile
    }
    return false
}

/* 字符串校验扩展 */
/**
 * 创建字符串校验器
 */
fun ValidGT.Companion.forString() = ValidGT.create<String>()

/**
 * 添加非空校验
 */
fun ValidGT<String>.nonEmpty(): ValidGT<String> = this.addCondition(
    { it.isNotEmpty() },
    "字符串不能为空"
)

/**
 * 添加正则匹配校验
 */
fun ValidGT<String>.matches(regex: Regex): ValidGT<String> = this.addCondition(
    { it.matches(regex) },
    "格式不匹配 (要求: ${regex.pattern})"
)