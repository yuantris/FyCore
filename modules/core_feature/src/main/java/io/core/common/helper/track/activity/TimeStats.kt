package io.core.common.helper.track.activity

/**
 * 统计某一个Activity访问详情
 */
data class TimeStats(
    // 总停留时间(毫秒)
    var totalDuration: Long = 0,
    // 今日停留时间
    var todayDuration: Long = 0,
    // 本周停留时间
    var weekDuration: Long = 0,
    // 本月停留时间
    var monthDuration: Long = 0,
    // 首次访问时间
    var firstVisitTime: Long = System.currentTimeMillis(),
    // 最后访问时间
    var lastVisitTime: Long = System.currentTimeMillis(),
    // 访问次数
    var visitCount: Int = 0,
    // 每日统计数据 (日期字符串 -> 停留时间)
    var dailyStats: MutableMap<String, Long> = mutableMapOf()
)