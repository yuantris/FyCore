package io.core.ui.helper.track.activity

import android.app.Activity
import io.core.utils.Preferences
import io.core.utils.extensions.cool.GSON
import io.core.utils.extensions.cool.fromJsonObject
import io.core.constant.TimePatterns
import java.util.Calendar
import java.util.Date

/**
 * 统计某一个Activity访问详情
 */
object TimeTracker {
    private val statsMap = mutableMapOf<String, TimeStats>()
    private const val STATS_PREFIX = "page_stats_"
    private val dateFormat = TimePatterns.getFormatter("yyyy-MM-dd")

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

    /**
     * 更新统计数据
     * @param activity 活动
     * @param duration 持续时间
     * @param dataId 数据项ID，可选，如果提供则同时更新数据项统计
     */
    fun updateStats(activity: Activity, duration: Long, dataId: String? = null) {
        val stats = getStats(activity::class.java)
        val currentTime = System.currentTimeMillis()
        val today = dateFormat.format(Date(currentTime))

        // 更新基本统计数据
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

        // 每月1号重置本月数�?
        if (isNewMonth(stats.lastVisitTime)) {
            stats.monthDuration = duration
        }

        // 如果提供了数据ID，则更新数据项统�?
        if (dataId != null) {
            // 获取或创建数据项统计
            val dataItemStats = stats.dataItemStats[dataId] ?: DataItemStats(dataId)

            // 更新数据项统�?
            dataItemStats.apply {
                totalDuration += duration
                todayDuration += duration
                lastVisitTime = currentTime
                visitCount++

                // 更新每日统计
                dailyStats[today] = (dailyStats[today] ?: 0) + duration
            }

            // 每天零点重置今日数据
            if (isNewDay(dataItemStats.lastVisitTime)) {
                dataItemStats.todayDuration = duration
            }

            // 保存数据项统�?
            stats.dataItemStats[dataId] = dataItemStats
        }

        saveStats(activity::class.java, stats)
    }

    /**
     * 获取特定数据项的统计数据
     * @param activityClass 活动�?
     * @param dataId 数据项ID
     * @return 数据项统计信息，如果不存在则返回新创建的统计信息
     */
    fun getDataItemStats(activityClass: Class<*>, dataId: String): DataItemStats {
        val stats = getStats(activityClass)
        return stats.dataItemStats[dataId] ?: DataItemStats(dataId)
    }

    /**
     * 获取特定数据项某一天的统计详情
     * @param activityClass 活动�?
     * @param dataId 数据项ID
     * @param date 日期
     * @return 该日期的停留时间
     */
    fun getDataItemDailyStats(activityClass: Class<*>, dataId: String, date: Date): Long {
        val dataItemStats = getDataItemStats(activityClass, dataId)
        val dateString = dateFormat.format(date)
        return dataItemStats.dailyStats[dateString] ?: 0
    }

    /**
     * 获取特定数据项某一天的统计详情（字符串日期格式：yyyy-MM-dd�?
     * @param activityClass 活动�?
     * @param dataId 数据项ID
     * @param dateString 日期字符�?
     * @return 该日期的停留时间
     */
    fun getDataItemDailyStats(activityClass: Class<*>, dataId: String, dateString: String): Long {
        val dataItemStats = getDataItemStats(activityClass, dataId)
        return dataItemStats.dailyStats[dateString] ?: 0
    }

    /**
     * 获取特定数据项所有日期的统计详情
     * @param activityClass 活动�?
     * @param dataId 数据项ID
     * @return 所有日期的停留时间映射
     */
    fun getDataItemAllDailyStats(activityClass: Class<*>, dataId: String): Map<String, Long> {
        val dataItemStats = getDataItemStats(activityClass, dataId)
        return dataItemStats.dailyStats.toMap()
    }

    /**
     * 获取活动中所有数据项的统计信�?
     * @param activityClass 活动�?
     * @return 所有数据项的统计信息映�?
     */
    fun getAllDataItemStats(activityClass: Class<*>): Map<String, DataItemStats> {
        val stats = getStats(activityClass)
        return stats.dataItemStats.toMap()
    }

    /**
     * 获取活动中某一天所有数据项的统计信�?
     * @param activityClass 活动�?
     * @param date 日期
     * @return 所有数据项在指定日期的停留时间映射 (数据ID -> 停留时间)
     */
    fun getDailyAllDataItemStats(activityClass: Class<*>, date: Date): Map<String, Long> {
        val stats = getStats(activityClass)
        val dateString = dateFormat.format(date)
        val result = mutableMapOf<String, Long>()

        stats.dataItemStats.forEach { (dataId, dataItemStats) ->
            val duration = dataItemStats.dailyStats[dateString] ?: 0
            result[dataId] = duration
        }

        return result
    }

    /**
     * 获取活动中某一天所有数据项的统计信息（字符串日期格式：yyyy-MM-dd�?
     * @param activityClass 活动�?
     * @param dateString 日期字符�?
     * @return 所有数据项在指定日期的停留时间映射 (数据ID -> 停留时间)
     */
    fun getDailyAllDataItemStats(activityClass: Class<*>, dateString: String): Map<String, Long> {
        val stats = getStats(activityClass)
        val result = mutableMapOf<String, Long>()

        stats.dataItemStats.forEach { (dataId, dataItemStats) ->
            val duration = dataItemStats.dailyStats[dateString] ?: 0
            result[dataId] = duration
        }

        return result
    }

    // 获取某一天的统计详情
    fun getDailyStats(activityClass: Class<*>, date: Date): Long {
        val stats = getStats(activityClass)
        val dateString = dateFormat.format(date)
        return stats.dailyStats[dateString] ?: 0
    }

    // 获取某一天的统计详情（字符串日期格式：yyyy-MM-dd�?
    fun getDailyStats(activityClass: Class<*>, dateString: String): Long {
        val stats = getStats(activityClass)
        return stats.dailyStats[dateString] ?: 0
    }

    // 获取所有日期的统计详情
    fun getAllDailyStats(activityClass: Class<*>): Map<String, Long> {
        val stats = getStats(activityClass)
        return stats.dailyStats.toMap()
    }

    // 新增保存单个统计�?
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