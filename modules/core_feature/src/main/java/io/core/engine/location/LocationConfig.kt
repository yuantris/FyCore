package io.core.engine.location

import android.location.LocationManager

data class LocationConfig(
    /** 位置请求超时时间 (毫秒) */
    val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    /** 最大重试次数 */
    val maxRetryCount: Int = DEFAULT_MAX_RETRY_COUNT,
    /** 实时跟踪超时时间 (毫秒) */
    val realtimeTrackingTimeoutMs: Long = DEFAULT_REALTIME_TRACKING_TIMEOUT_MS,
    /** 最小位置变化距离 (米) */
    val minDistanceChange: Float = DEFAULT_MIN_DISTANCE_CHANGE,
    /** 最小时间间隔 (毫秒) */
    val minTimeChange: Long = DEFAULT_MIN_TIME_CHANGE,
    /** 周期更新间隔 (毫秒) */
    val periodicIntervalMs: Long = DEFAULT_PERIODIC_INTERVAL_MS,
    /** 位置提供者列表 */
    val providers: List<String> = DEFAULT_PROVIDERS,
    /** 是否开启调试日志 */
    val debug: Boolean = false,
    /** Provider 优先级映射 */
    val providerPriority: Map<String, Int> = DEFAULT_PROVIDER_PRIORITY
) {
    companion object {
        const val DEFAULT_TIMEOUT_MS = 3_000L
        const val DEFAULT_MAX_RETRY_COUNT = 2
        const val DEFAULT_REALTIME_TRACKING_TIMEOUT_MS = 15_000L
        const val DEFAULT_MIN_DISTANCE_CHANGE = 5f
        const val DEFAULT_MIN_TIME_CHANGE = 1_000L
        const val DEFAULT_PERIODIC_INTERVAL_MS = 10_000L

        val DEFAULT_PROVIDERS = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        val DEFAULT_PROVIDER_PRIORITY = mapOf(
            LocationManager.GPS_PROVIDER to 3,
            LocationManager.NETWORK_PROVIDER to 2,
            LocationManager.PASSIVE_PROVIDER to 1
        )
    }

    class Builder {
        private var timeoutMs: Long = DEFAULT_TIMEOUT_MS
        private var maxRetryCount: Int = DEFAULT_MAX_RETRY_COUNT
        private var realtimeTrackingTimeoutMs: Long = DEFAULT_REALTIME_TRACKING_TIMEOUT_MS
        private var minDistanceChange: Float = DEFAULT_MIN_DISTANCE_CHANGE
        private var minTimeChange: Long = DEFAULT_MIN_TIME_CHANGE
        private var periodicIntervalMs: Long = DEFAULT_PERIODIC_INTERVAL_MS
        private var providers: List<String> = DEFAULT_PROVIDERS
        private var debug: Boolean = false
        private var providerPriority: Map<String, Int> = DEFAULT_PROVIDER_PRIORITY

        fun setTimeoutMs(ms: Long) = apply { this.timeoutMs = ms }
        fun setMaxRetryCount(count: Int) = apply { this.maxRetryCount = count }
        fun setRealtimeTrackingTimeoutMs(ms: Long) = apply { this.realtimeTrackingTimeoutMs = ms }
        fun setMinDistanceChange(meters: Float) = apply { this.minDistanceChange = meters }
        fun setMinTimeChange(ms: Long) = apply { this.minTimeChange = ms }
        fun setPeriodicIntervalMs(ms: Long) = apply { this.periodicIntervalMs = ms }
        fun setProviders(providers: List<String>) = apply { this.providers = providers }
        fun setDebug(debug: Boolean) = apply { this.debug = debug }
        fun setProviderPriority(priority: Map<String, Int>) = apply { this.providerPriority = priority }

        fun build() = LocationConfig(
            timeoutMs = timeoutMs,
            maxRetryCount = maxRetryCount,
            realtimeTrackingTimeoutMs = realtimeTrackingTimeoutMs,
            minDistanceChange = minDistanceChange,
            minTimeChange = minTimeChange,
            periodicIntervalMs = periodicIntervalMs,
            providers = providers,
            debug = debug,
            providerPriority = providerPriority
        )
    }
}
