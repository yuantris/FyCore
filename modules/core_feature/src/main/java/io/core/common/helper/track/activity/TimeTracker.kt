package io.core.common.helper.track.activity

import android.app.Activity
import io.core.common.util.Preferences
import io.core.common.util.extensions.cool.GSON
import io.core.common.util.extensions.cool.fromJsonObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 统计某一个Activity访问详情
 */
object TimeTracker {
    private val statsMap = mutableMapOf<String, TimeStats>()
    private const val STATS_PREFIX = "page_stats_"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadAllStats()
    }

    // 获取页面统计数据
    fun getStats(activityClass: Class<*>): TimeStats {
        val key = STATS_PREFIX + activityClass.name
        return statsMap[key] ?: run {
            val stats = loadStats(activityClass) ?: TimeStats()
            statsMap[key] = stats
            stats
        }
    }

    // 更新统计数据
    fun updateStats(activity: Activity, duration: Long) {
        val stats = getStats(activity::class.java)
        val currentTime = System.currentTimeMillis()
        val today = dateFormat.format(Date(currentTime))

        stats.apply {
            totalDuration += duration
            todayDuration += duration
            weekDuration += duration
            monthDuration += duration
            lastVisitTime = currentTime
            visitCount++

            // 更新每日统计
            dailyStats[today] = (dailyStats[today] ?: 0) + duration
        }

        // 每天零点重置今日数据
        if (isNewDay(stats.lastVisitTime)) {
            stats.todayDuration = duration
        }

        // 每周一重置本周数据
        if (isNewWeek(stats.lastVisitTime)) {
            stats.weekDuration = duration
        }

        // 每月1号重置本月数据
        if (isNewMonth(stats.lastVisitTime)) {
            stats.monthDuration = duration
        }

        saveStats(activity::class.java, stats)
    }

    // 获取某一天的统计详情
    fun getDailyStats(activityClass: Class<*>, date: Date): Long {
        val stats = getStats(activityClass)
        val dateString = dateFormat.format(date)
        return stats.dailyStats[dateString] ?: 0
    }

    // 获取某一天的统计详情（字符串日期格式：yyyy-MM-dd）
    fun getDailyStats(activityClass: Class<*>, dateString: String): Long {
        val stats = getStats(activityClass)
        return stats.dailyStats[dateString] ?: 0
    }

    // 获取所有日期的统计详情
    fun getAllDailyStats(activityClass: Class<*>): Map<String, Long> {
        val stats = getStats(activityClass)
        return stats.dailyStats.toMap()
    }

    // 新增保存单个统计项
    private fun saveStats(activityClass: Class<*>, stats: TimeStats) {
        Preferences.putValue("${STATS_PREFIX}${activityClass.name}", GSON.toJson(stats))
    }

    // 新增加载所有统计项
    private fun loadAllStats() {
        val allPrefs = Preferences.sp.all
        allPrefs.keys.filter { it.startsWith(STATS_PREFIX) }.forEach { key ->
            allPrefs[key]?.toString()?.let { json ->
                runCatching {
                    statsMap[key] = GSON.fromJsonObject<TimeStats>(json).getOrThrow()
                }
            }
        }
    }

    // 新增单独加载方法
    private fun loadStats(activityClass: Class<*>): TimeStats? {
        return Preferences.sp.getString("${STATS_PREFIX}${activityClass.name}", null)
            ?.let { GSON.fromJsonObject<TimeStats>(it).getOrNull() }
    }

    private fun isNewDay(lastTime: Long): Boolean {
        val calNow = Calendar.getInstance()
        val calLast = Calendar.getInstance().apply { timeInMillis = lastTime }
        return calNow[Calendar.DAY_OF_YEAR] != calLast[Calendar.DAY_OF_YEAR]
    }

    private fun isNewWeek(lastTime: Long): Boolean {
        val calNow = Calendar.getInstance()
        val calLast = Calendar.getInstance().apply { timeInMillis = lastTime }
        return calNow[Calendar.WEEK_OF_YEAR] != calLast[Calendar.WEEK_OF_YEAR]
    }

    private fun isNewMonth(lastTime: Long): Boolean {
        val calNow = Calendar.getInstance()
        val calLast = Calendar.getInstance().apply { timeInMillis = lastTime }
        return calNow[Calendar.MONTH] != calLast[Calendar.MONTH]
    }
}