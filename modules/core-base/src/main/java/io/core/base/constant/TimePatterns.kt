package io.core.base.constant

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * ██████████████████████████████████
 * █▄█████▄█ 日期时间格式模式常量集合 █▄
 * █▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
 * █ 包含ISO、RFC标准及常用自定义格式
 * █ 所有模式符遵循SimpleDateFormat规范
 * █▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
 */
object TimePatterns {

    private val formatterCache = mutableMapOf<String, SimpleDateFormat>()


    // ====================
    // 国际标准格式
    // ====================
    
    /** ISO 8601 完整格式（带毫秒和时区）示例：2023-10-12T15:30:45.123+08:00 */
    @TimeFormat("2023-10-12T15:30:45.123+08:00")
    const val DATE_TIME_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    
    /** ISO 8601 基本格式（不带毫秒）示例：2023-10-12T15:30:45+0800 */
    @TimeFormat("2023-10-12T15:30:45+0800")
    const val DATE_TIME_ISO_8601_BASIC = "yyyy-MM-dd'T'HH:mm:ssZ"
    
    /** ISO 日期格式 示例：2023-10-12 */
    @TimeFormat("2023-10-12")
    const val DATE_ONLY_ISO = "yyyy-MM-dd"
    
    /** ISO 时间格式（24小时制带秒）示例：15:30:45 */
    @TimeFormat("15:30:45")
    const val TIME_ONLY_ISO = "HH:mm:ss"

    // ====================
    // RFC 标准
    // ====================
    
    /** RFC 822 格式 示例：12 Oct 2023 15:30:45 +0800 */
    @TimeFormat("12 Oct 2023 15:30:45 +0800")
    const val DATE_TIME_RFC_822 = "dd MMM yyyy HH:mm:ss Z"
    
    /** RFC 1123 格式 示例：Thu, 12 Oct 2023 15:30:45 +0800 */
    @TimeFormat("Thu, 12 Oct 2023 15:30:45 +0800")
    const val DATE_TIME_RFC_1123 = "EEE, dd MMM yyyy HH:mm:ss Z"

    // ====================
    // 自定义日期格式
    // ====================
    
    /** 完整中文日期 示例：2023年10月12日 */
    @TimeFormat("2023年10月12日")
    const val DATE_YMD_ZH = "yyyy年MM月dd日"
    
    /** 短横线分隔日期 示例：2023-10-12 */
    @TimeFormat("2023-10-12")
    const val DATE_YMD = "yyyy-MM-dd"
    
    /** 斜线分隔日期 示例：2023/10/12 */
    @TimeFormat("2023/10/12")
    const val DATE_YMD_SLASHES = "yyyy/MM/dd"
    
    /** 点分隔日期 示例：12.10.2023 */
    @TimeFormat("12.10.2023")
    const val DATE_DMY_DOTS = "dd.MM.yyyy"
    
    /** 月份名称格式 示例：October 12, 2023 */
    @TimeFormat("October 12, 2023")
    const val DATE_MONTH_NAME = "MMMM dd, yyyy"
    
    /** 缩写月份格式 示例：Oct 12, 2023 */
    @TimeFormat("Oct 12, 2023")
    const val DATE_MONTH_ABBR = "MMM dd, yyyy"

    // ====================
    // 自定义时间格式
    // ====================

    /** 年月日时分秒格式 示例：2023-10-12 15:30:45*/
    @TimeFormat("2023-10-12 15:30:45")
    const val TIME_FULL = "yyyy-MM-dd HH:mm:ss"
    
    /** 24小时制完整时间 示例：15:30:45 */
    @TimeFormat("15:30:45")
    const val TIME_24H_FULL = "HH:mm:ss"
    
    /** 24小时制短时间 示例：15:30 */
    @TimeFormat("15:30")
    const val TIME_24H_SHORT = "HH:mm"
    
    /** 12小时制带AM/PM 示例：3:30 PM */
    @TimeFormat("3:30 PM")
    const val TIME_12H_WITH_AMPM = "h:mm a"

    /** 分钟秒数格式 示例：mm:ss */
    @TimeFormat("13:45")
    const val TIME_MM_SS = "mm:ss"
    
    /** 带毫秒时间 示例：15:30:45.123 */
    @TimeFormat("15:30:45.123")
    const val TIME_WITH_MILLIS = "HH:mm:ss.SSS"

    // ====================
    // 组合格式
    // ====================
    
    /** 文件命名友好格式 示例：20231012_153045 */
    @TimeFormat("20231012_153045")
    const val FILE_SAFE_TIMESTAMP = "yyyyMMdd_HHmmss"
    
    /** 聊天消息时间格式 示例：Oct 12 15:30 */
    @TimeFormat("Oct 12 15:30")
    const val CHAT_MESSAGE_STYLE = "MMM dd HH:mm"
    
    /** 日志时间戳格式 示例：[2023-10-12 15:30:45.123] */
    @TimeFormat("[2023-10-12 15:30:45.123]")
    const val LOG_TIMESTAMP = "[yyyy-MM-dd HH:mm:ss.SSS]"

    /** 日志时间戳格式 示例：[2023-10-12_15:30:45_123] */
    @TimeFormat("[2023-10-12_15:30:45_123]")
    const val LOG_TIMESTAMP_LINE = "[yyyy-MM-dd_HH:mm:ss_SSS]"

    // ====================
    // 扩展方法
    // ====================
    @JvmStatic
    @Synchronized
    fun getFormatter(pattern: String): SimpleDateFormat {
        return formatterCache.getOrPut(pattern) {
            SimpleDateFormat(pattern, Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
        }
    }
}