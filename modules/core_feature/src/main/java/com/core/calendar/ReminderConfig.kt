package com.core.calendar

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 提醒配置数据类
 */
@Parcelize
data class ReminderConfig(
    val type: ReminderType,
    val method: ReminderMethod = ReminderMethod.ALERT,
    val minutes: Int // 提前多少分钟提醒
) : Parcelable {
    
    companion object {
        // 常用提醒时间预设
        @JvmStatic fun immediately() = ReminderConfig(ReminderType.ONCE, ReminderMethod.ALERT, 0)
        @JvmStatic fun before5Minutes() = ReminderConfig(ReminderType.ONCE, ReminderMethod.ALERT, 5)
        @JvmStatic fun before15Minutes() = ReminderConfig(ReminderType.ONCE, ReminderMethod.ALERT, 15)
        @JvmStatic fun before30Minutes() = ReminderConfig(ReminderType.ONCE, ReminderMethod.ALERT, 30)
        @JvmStatic fun before1Hour() = ReminderConfig(ReminderType.ONCE, ReminderMethod.ALERT, 60)
        @JvmStatic fun before1Day() = ReminderConfig(ReminderType.ONCE, ReminderMethod.ALERT, 1440)
    }
}

/**
 * 提醒类型
 */
enum class ReminderType {
    ONCE,           // 一次性提醒
    RECURRING       // 重复提醒（跟随事件重复规则）
}

/**
 * 提醒方式
 */
enum class ReminderMethod(@JvmField val value: Int) {
    DEFAULT(0),     // 系统默认
    ALERT(1),       // 弹窗提醒
    EMAIL(2),       // 邮件提醒
    SMS(3);         // 短信提醒（需要权限）
    
    companion object {
        @JvmStatic
        fun fromValue(value: Int): ReminderMethod = values().find { it.value == value } ?: DEFAULT
    }
}