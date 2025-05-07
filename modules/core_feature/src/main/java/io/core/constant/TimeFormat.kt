package io.core.constant

/**
 * 标记时间格式常量的实际显示模式
 * @property displayPattern 用于显示的格式示例
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class TimeFormat(val displayPattern: String)
