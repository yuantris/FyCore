package io.core.base.constant

import java.util.concurrent.TimeUnit

/**
 * 文件大小和时间格式化工具类
 * 提供文件大小格式化、持续时间格式化和相关单位转换功能
 */
object FileSize {
    // region 常量定义
    /** 1KB的字节数 */
    private const val BYTES_IN_KB = 1024L

    /** 1MB的字节数 */
    private const val BYTES_IN_MB = BYTES_IN_KB * 1024L

    /** 1GB的字节数 */
    private const val BYTES_IN_GB = BYTES_IN_MB * 1024L

    /** 1TB的字节数 */
    private const val BYTES_IN_TB = BYTES_IN_GB * 1024L
    // endregion

    // region 公共API
    /**
     * 格式化文件大小
     * @param size 文件大小（字节）
     * @param withUnit 是否显示单位（默认true）
     * @param unitStyle 单位显示风格（默认标准大写）
     * @param precision 小数精度（默认2位）
     * @param minUnit 最小显示单位（默认自动选择）
     */
    @JvmStatic
    @JvmOverloads
    fun format(
        size: Long,
        withUnit: Boolean = true,
        unitStyle: SizeUnitStyle = SizeUnitStyle.StandardUpper,
        precision: Int = 2,
        minUnit: SizeUnit = SizeUnit.BYTE
    ): String {
        require(precision >= 0) { "Precision must be non-negative" }

        val (divisor, unit) = determineAppropriateUnit(size, minUnit)
        val formattedValue = "%.${precision}f".format(size.toDouble() / divisor)

        return if (withUnit) "$formattedValue${getUnitString(unit, unitStyle)}"
        else formattedValue
    }

    /**
     * 格式化时间间隔
     * @param millis 毫秒数
     * @param showMillis 是否显示毫秒（默认false）
     * @param compact 是否使用紧凑格式（无空格，默认false）
     * @param unitStyle 时间单位显示风格（默认短英文小写）
     */
    @JvmStatic
    @JvmOverloads
    fun formatDuration(
        millis: Long,
        showMillis: Boolean = false,
        compact: Boolean = false,
        unitStyle: TimeUnitStyle = TimeUnitStyle.SHORT_ENGLISH
    ): String {
        val components = calculateTimeComponents(millis, showMillis)
        return buildDurationString(components, compact, unitStyle)
    }

    /**
     * 使用自定义模式格式化时间
     * @param durationMs 时长（毫秒）
     * @param pattern 格式模式（支持HH-小时，mm-分钟，ss-秒，SSS-毫秒）
     */
    @JvmStatic
    fun formatDuration(durationMs: Long, pattern: String): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        val millis = durationMs % 1000

        return pattern
            .replace("HH", "%02d".format(hours))
            .replace("mm", "%02d".format(minutes))
            .replace("ss", "%02d".format(seconds))
            .replace("SSS", "%03d".format(millis))
    }

    /**
     * 将带单位的文件大小字符串转换为字节数
     * @param sizeStr 支持格式如 "1.5MB", "2GB", "500kb"
     * @throws NumberFormatException 当数字格式无效时抛出
     * @throws IllegalArgumentException 当输入格式或单位无效时抛出
     */
    @JvmStatic
    fun parseToBytes(sizeStr: String): Long {
        val trimmed = sizeStr.trim()
        require(trimmed.isNotEmpty()) { "Input string cannot be empty" }

        val matchResult = FILE_SIZE_REGEX.matchEntire(trimmed)
            ?: throw IllegalArgumentException("Invalid format: '$trimmed'")

        val (valueStr, unitStr) = matchResult.destructured
        val value = valueStr.toDoubleOrNull()
            ?: throw NumberFormatException("Invalid number: '$valueStr'")

        return (value * SizeUnit.fromString(unitStr).bytes).toLong()
    }
    // endregion

    // region 内部实现
    private val FILE_SIZE_REGEX =
        """^(\d+\.?\d*)\s*([KMGTP]?[Bb]?)$""".toRegex(RegexOption.IGNORE_CASE)

    /**
     * 确定最适合的显示单位和除数
     */
    private fun determineAppropriateUnit(size: Long, minUnit: SizeUnit): Pair<Long, SizeUnit> =
        when {
            size >= BYTES_IN_TB && minUnit.bytes <= BYTES_IN_TB -> BYTES_IN_TB to SizeUnit.TB
            size >= BYTES_IN_GB && minUnit.bytes <= BYTES_IN_GB -> BYTES_IN_GB to SizeUnit.GB
            size >= BYTES_IN_MB && minUnit.bytes <= BYTES_IN_MB -> BYTES_IN_MB to SizeUnit.MB
            size >= BYTES_IN_KB && minUnit.bytes <= BYTES_IN_KB -> BYTES_IN_KB to SizeUnit.KB
            else -> 1L to SizeUnit.BYTE
        }

    /**
     * 获取单位字符串
     */
    private fun getUnitString(unit: SizeUnit, style: SizeUnitStyle): String = when (style) {
        is SizeUnitStyle.StandardUpper -> unit.name
        is SizeUnitStyle.StandardLower -> unit.name.lowercase()
        is SizeUnitStyle.ShortUpper -> unit.name.first().toString()
        is SizeUnitStyle.ShortLower -> unit.name.first().lowercase()
    }

    /**
     * 计算时间组件
     */
    private data class TimeComponents(
        val hours: Long,
        val minutes: Long,
        val seconds: Long,
        val millis: Long,
        val showMillis: Boolean
    )

    private fun calculateTimeComponents(millis: Long, showMillis: Boolean): TimeComponents {
        return TimeComponents(
            hours = TimeUnit.MILLISECONDS.toHours(millis),
            minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60,
            seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60,
            millis = millis % 1000,
            showMillis = millis < 1000 || showMillis
        )
    }

    /**
     * 构建持续时间字符串
     */
    private fun buildDurationString(
        components: TimeComponents,
        compact: Boolean,
        unitStyle: TimeUnitStyle
    ): String {
        return buildString {
            if (components.hours > 0) {
                append(components.hours)
                append(formatTimeUnit("hour", components.hours > 1, unitStyle))
                if (!compact) append(" ")
            }
            if (components.minutes > 0 || components.hours > 0) {
                append(components.minutes)
                append(formatTimeUnit("minute", components.minutes > 1, unitStyle))
                if (!compact) append(" ")
            }
            append(components.seconds)
            append(formatTimeUnit("second", components.seconds > 1, unitStyle))
            if (components.showMillis && components.millis > 0) {
                if (!compact) append(" ")
                append(components.millis)
                append(formatTimeUnit("millis", components.millis > 1, unitStyle))
            }
        }
    }

    /**
     * 格式化时间单位
     */
    private fun formatTimeUnit(type: String, plural: Boolean, style: TimeUnitStyle): String {
        return when (style) {
            is TimeUnitStyle.English -> formatEnglishUnit(type, plural, style)
            is TimeUnitStyle.Chinese -> formatChineseUnit(type, style)
        }
    }

    private fun formatEnglishUnit(
        type: String,
        plural: Boolean,
        style: TimeUnitStyle.English
    ): String {
        val base = when (style.length) {
            TimeUnitStyle.UnitLength.SHORT -> when (type) {
                "hour" -> "h"
                "minute" -> "m"
                "second" -> "s"
                "millis" -> "ms"
                else -> ""
            }

            TimeUnitStyle.UnitLength.LONG -> when (type) {
                "hour" -> "hour"
                "minute" -> "min"
                "second" -> "sec"
                "millis" -> "ms"
                else -> ""
            }
        }
        val withPlural =
            if (plural && style.length == TimeUnitStyle.UnitLength.LONG) "${base}s" else base
        return applyTextCase(withPlural, style.unitCase)
    }

    private fun formatChineseUnit(type: String, style: TimeUnitStyle.Chinese): String {
        return when (style.length) {
            TimeUnitStyle.UnitLength.SHORT -> when (type) {
                "hour" -> "时"
                "minute" -> "分"
                "second" -> "秒"
                "millis" -> "毫秒"
                else -> ""
            }

            TimeUnitStyle.UnitLength.LONG -> when (type) {
                "hour" -> "小时"
                "minute" -> "分钟"
                "second" -> "秒"
                "millis" -> "毫秒"
                else -> ""
            }
        }
    }

    private fun applyTextCase(str: String, unitCase: TimeUnitStyle.UnitCase): String =
        when (unitCase) {
            TimeUnitStyle.UnitCase.UPPER -> str.uppercase()
            TimeUnitStyle.UnitCase.LOWER -> str.lowercase()
            TimeUnitStyle.UnitCase.CAPITAL -> str.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }
    // endregion

    // region 文件大小单位相关
    /**
     * 文件大小单位枚举
     * @property bytes 当前单位对应的字节数
     * @property abbreviations 支持的缩写集合（不区分大小写）
     */
    enum class SizeUnit(val bytes: Long, val abbreviations: Set<String>) {
        BYTE(1L, setOf("B", "")),
        KB(BYTES_IN_KB, setOf("K", "KB")),
        MB(BYTES_IN_MB, setOf("M", "MB")),
        GB(BYTES_IN_GB, setOf("G", "GB")),
        TB(BYTES_IN_TB, setOf("T", "TB"));

        companion object {
            // 延迟初始化的单位映射表（提升首次访问性能）
            private val unitMap by lazy {
                values().flatMap { unit ->
                    unit.abbreviations.map { it.uppercase() to unit }
                }.toMap()
            }

            /**
             * 从字符串解析单位
             * @param unitStr 单位字符串（如"KB"、"mb"）
             * @throws IllegalArgumentException 当单位不支持时抛出
             */
            fun fromString(unitStr: String): SizeUnit {
                return unitMap[unitStr.uppercase()]
                    ?: throw IllegalArgumentException("不支持的单位: '$unitStr'")
            }
        }
    }

    /**
     * 文件大小单位显示风格
     */
    sealed class SizeUnitStyle {
        /** 标准大写（如KB） */
        object StandardUpper : SizeUnitStyle()

        /** 标准小写（如kb） */
        object StandardLower : SizeUnitStyle()

        /** 简写大写（如K） */
        object ShortUpper : SizeUnitStyle()

        /** 简写小写（如k） */
        object ShortLower : SizeUnitStyle()
    }
    // endregion

    // region 时间单位相关
    /**
     * 时间单位显示风格
     */
    sealed class TimeUnitStyle {
        /** 英文风格 */
        sealed class English(
            open val unitCase: UnitCase,
            open val length: UnitLength
        ) : TimeUnitStyle() {
            /** 短格式英文（如h/m/s） */
            data class Short(
                override val unitCase: UnitCase = UnitCase.LOWER
            ) : English(unitCase, UnitLength.SHORT)

            /** 长格式英文（如hour/min/sec） */
            data class Long(
                override val unitCase: UnitCase = UnitCase.LOWER
            ) : English(unitCase, UnitLength.LONG)
        }

        /** 中文风格 */
        sealed class Chinese(
            open val length: UnitLength
        ) : TimeUnitStyle() {
            /** 短格式中文（如时/分/秒） */
            data class Short(
                override val length: UnitLength = UnitLength.SHORT
            ) : Chinese(length)

            /** 长格式中文（如小时/分钟/秒） */
            data class Long(
                override val length: UnitLength = UnitLength.LONG
            ) : Chinese(length)
        }

        /** 单位大小写风格 */
        enum class UnitCase { UPPER, LOWER, CAPITAL }

        /** 单位长度风格 */
        enum class UnitLength { SHORT, LONG }

        companion object {
            // 预设常用风格
            val SHORT_ENGLISH = English.Short()
            val LONG_ENGLISH = English.Long()
            val SHORT_CHINESE = Chinese.Short()
            val LONG_CHINESE = Chinese.Long()
        }
    }
    // endregion

    // region 扩展函数
    /**
     * Long扩展函数：格式化文件大小
     */
    fun Long.toFormattedFileSize(
        withUnit: Boolean = true,
        unitStyle: SizeUnitStyle = SizeUnitStyle.StandardUpper,
        precision: Int = 2,
        minUnit: SizeUnit = SizeUnit.BYTE
    ) = format(this, withUnit, unitStyle, precision, minUnit)

    /**
     * Long扩展函数：格式化时间间隔
     */
    fun Long.toFormattedDuration(
        showMillis: Boolean = false,
        compact: Boolean = false,
        unitStyle: TimeUnitStyle = TimeUnitStyle.SHORT_ENGLISH
    ) = formatDuration(this, showMillis, compact, unitStyle)

    /**
     * Long扩展函数：格式化时间格式字符串（默认mm:ss）
     */
    fun Long.toFormattedPattern(pattern: String = "mm:ss"): String {
        return formatDuration(this, pattern)
    }
    // endregion
}