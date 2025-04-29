package io.core.common.helper.track

import android.app.Activity
import io.core.common.util.Preferences
import io.core.common.util.extensions.cool.GSON
import io.core.common.util.extensions.cool.fromJsonObject
import java.util.Calendar

/**
 * 统计某一个Activity访问详情
 */
object TimeTracker {
    private val statsMap = mutableMapOf<String, TimeStats>()
    private const val STATS_PREFIX = "page_stats_"

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
        stats.apply {
            totalDuration += duration
            todayDuration += duration
            weekDuration += duration
            monthDuration += duration
            lastVisitTime = System.currentTimeMillis()
            visitCount++
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