package io.core.ui.helper.track.activity

import androidx.annotation.Keep

/**
 * 数据项统计信�?
 */
@Keep
data class DataItemStats(
    // 数据项ID
    val dataId: String,
    // 总停留时�?毫秒)
    var totalDuration: Long = 0,
    // 今日停留时间
    var todayDuration: Long = 0,
    // 首次访问时间
    var firstVisitTime: Long = System.currentTimeMillis(),
    // 最后访问时�?
    var lastVisitTime: Long = System.currentTimeMillis(),
    // 访问次数
    var visitCount: Int = 0,
    // 每日统计数据 (日期字符�?-> 停留时间)
    var dailyStats: MutableMap<String, Long> = mutableMapOf()
)