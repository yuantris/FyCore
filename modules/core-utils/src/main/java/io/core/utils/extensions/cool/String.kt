@file:Suppress("unused")

package io.core.utils.extensions.cool

import android.icu.text.Collator
import android.icu.util.ULocale
import android.net.Uri
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import io.core.utils.Toaster
import io.core.utils.tools.isAndroid7Plus
import java.io.File
import java.lang.Character.codePointCount
import java.lang.Character.offsetByCodePoints
import java.util.Locale
import java.util.regex.Pattern
import androidx.core.net.toUri

// ========================================
// 字符串验证与转换扩展
// ========================================

/**
 * 安全去除字符串两端空白字�?
 * 如果字符串为null或空白，返回null；否则返回去除空白后的字符串
 *
 * @return 去除空白后的字符串或null
 */
fun String?.safeTrim() = if (this.isNullOrBlank()) null else this.trim()

/**
 * 检查字符串是否为content://协议
 *
 * @return true表示是content://协议，false表示不是
 */
fun String?.isContentScheme(): Boolean = this?.startsWith("content://") == true

/**
 * 将字符串转换为Editable对象
 * 用于EditText等需要可编辑文本的场�?
 *
 * @return Editable对象
 */
fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

/**
 * 将字符串解析为Uri对象
 * 如果是URI格式则直接解析，否则作为文件路径处理
 *
 * @return Uri对象
 */
fun String.parseToUri(): Uri {
    return if (isUri()) this.toUri() else {
        Uri.fromFile(File(this))
    }
}

/**
 * 检查字符串是否为URI格式
 * 支持file://和content://协议
 *
 * @return true表示是URI格式，false表示不是
 */
fun String?.isUri(): Boolean {
    this ?: return false
    return this.startsWith("file://", true) || isContentScheme()
}

/**
 * 检查字符串是否为绝对URL
 * 支持http://和https://协议
 *
 * @return true表示是绝对URL，false表示不是
 */
fun String?.isAbsUrl() =
    this?.let {
        it.startsWith("http://", true) || it.startsWith("https://", true)
    } ?: false

val dataUriRegex = Regex("data:.*?;base64,(.*)")

/**
 * 检查字符串是否为Data URL格式
 * 格式：data:[<mediatype>][;base64],<data>
 *
 * @return true表示是Data URL，false表示不是
 */
fun String?.isDataUrl() =
    this?.let {
        dataUriRegex.matches(it)
    } ?: false

/**
 * 检查字符串是否为JSON格式
 * 支持JSON对象{}和JSON数组[]
 *
 * @return true表示是JSON格式，false表示不是
 */
fun String?.isJson(): Boolean =
    this?.run {
        val str = this.trim()
        when {
            str.startsWith("{") && str.endsWith("}") -> true
            str.startsWith("[") && str.endsWith("]") -> true
            else -> false
        }
    } ?: false

/**
 * 检查字符串是否为JSON对象格式
 * 格式：{...}
 *
 * @return true表示是JSON对象，false表示不是
 */
fun String?.isJsonObject(): Boolean =
    this?.run {
        val str = this.trim()
        str.startsWith("{") && str.endsWith("}")
    } ?: false

/**
 * 检查字符串是否为JSON数组格式
 * 格式：[...]
 *
 * @return true表示是JSON数组，false表示不是
 */
fun String?.isJsonArray(): Boolean =
    this?.run {
        val str = this.trim()
        str.startsWith("[") && str.endsWith("]")
    } ?: false

/**
 * 检查字符串是否为XML格式
 * 简单检查是否以<开头并�?结尾
 *
 * @return true表示可能是XML格式，false表示不是
 */
fun String?.isXml(): Boolean =
    this?.run {
        val str = this.trim()
        str.startsWith("<") && str.endsWith(">")
    } ?: false

/**
 * 将字符串解析为布尔�?
 * 支持多种表示false的字符串：false、no、not�?（忽略大小写�?
 *
 * @param nullIsTrue 当字符串为null时的返回值，默认为false
 * @return 解析后的布尔�?
 */
fun String?.isTrue(nullIsTrue: Boolean = false): Boolean {
    if (this.isNullOrBlank() || this == "null") {
        return nullIsTrue
    }
    return !this.trim().matches("(?i)^(false|no|not|0)$".toRegex())
}

// ========================================
// 字符串分割与处理扩展
// ========================================

/**
 * 分割字符串并过滤空白�?
 *
 * @param delimiter 分隔符数�?
 * @param limit 分割限制�?表示无限�?
 * @return 过滤空白后的字符串数�?
 */
fun String.splitNotBlank(vararg delimiter: String, limit: Int = 0): Array<String> = run {
    this.split(*delimiter, limit = limit).map { it.trim() }.filterNot { it.isBlank() }
        .toTypedArray()
}

/**
 * 使用正则表达式分割字符串并过滤空白项
 *
 * @param regex 正则表达�?
 * @param limit 分割限制�?表示无限�?
 * @return 过滤空白后的字符串数�?
 */
fun String.splitNotBlank(regex: Regex, limit: Int = 0): Array<String> = run {
    this.split(regex, limit).map { it.trim() }.filterNot { it.isBlank() }.toTypedArray()
}

/**
 * 比较两个中文字符串的排序顺序（支持多版本兼容�?
 *
 * 实现说明�?
 * - Android 7+ 使用ICU4J的Collator实现更准确的区域敏感排序
 * - 旧版本使用标准Java的Collator实现向后兼容
 *
 * @param other 要比较的目标字符�?
 * @return 比较结果�?
 *         - 正数 表示当前字符串在排序中位于参数之�?
 *         - 负数 表示当前字符串在排序中位于参数之�?
 *         - 0    表示两个字符串排序位置相�?
 */
fun String.cnCompare(other: String): Int {
    return if (isAndroid7Plus) {
        Collator.getInstance(ULocale.SIMPLIFIED_CHINESE).compare(this, other)
    } else {
        java.text.Collator.getInstance(Locale.CHINA).compare(this, other)
    }
}

// ========================================
// 字符串分析与计算扩展
// ========================================

/**
 * 计算字符串所占内存大小（字节�?
 * 基于Java字符串内存模型：对象�?40字节) + 字符数据(2*length字节)
 *
 * @return 内存大小（字节），null返回0
 */
fun String?.memorySize(): Int {
    this ?: return 0
    return 40 + 2 * length
}

/**
 * 检查字符串是否包含中文字符
 * 使用Unicode范围[\u4e00-\u9fa5]匹配中文字符
 *
 * @return true表示包含中文，false表示不包�?
 */
fun String.isChinese(): Boolean {
    val p = Pattern.compile("[\u4e00-\u9fa5]")
    val m = p.matcher(this)
    return m.find()
}

/**
 * 将字符序列拆分为单个字符数组（支持emoji等多字节字符�?
 * 使用Unicode代码点正确处理emoji和其他复合字�?
 *
 * @return 字符数组，每个元素为一个完整的字符（包括emoji�?
 */
fun CharSequence.toStringArray(): Array<String> {
    var codePointIndex = 0
    return try {
        Array(codePointCount(this, 0, length)) {
            val start = codePointIndex
            codePointIndex = offsetByCodePoints(this, start, 1)
            substring(start, codePointIndex)
        }
    } catch (e: Exception) {
        split("").toTypedArray()
    }
}

// ========================================
// 字符串样式与显示扩展
// ========================================

/**
 * 为字符串设置前景色并返回SpannableString
 * 用于在TextView中显示带颜色的文�?
 *
 * @param color 前景色（ARGB格式�?
 * @return 设置了颜色的SpannableString
 */
fun String.spanForeColor(@ColorInt color: Int): SpannableString {
    val spannableString = SpannableString(this)
    spannableString.setSpan(
        ForegroundColorSpan(color),
        0,
        spannableString.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return spannableString
}

/**
 * 将字符串作为Toast消息显示
 * 使用全局Toaster显示消息
 */
fun String.toast() {
    Toaster.show(this)
}

/**
 * 移除字符串中的所有空白字�?
 * 该函数通过正则表达式匹配并移除字符串中的所有空白字符（包括空格、制表符、换行符等）
 *
 * @return 返回一个不包含任何空白字符的新字符�?
 */
fun String.removeWhitespace(): String {
    return this.replace("\\s+".toRegex(), "")
}

