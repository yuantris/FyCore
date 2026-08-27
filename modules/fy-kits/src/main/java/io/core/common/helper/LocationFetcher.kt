@file:JvmName("LocationFetcher")

package io.core.common.helper

import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.location.LocationListenerCompat
import io.core.appCtx
import io.core.common.util.extensions.cool.TimeoutCancellationException
import io.core.common.util.extensions.locationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resumeWithException

/* ---------- 数据模型 ---------- */

data class LocationDetail(
    /** 实际触发回调的 provider 名称，如 GPS_PROVIDER / NETWORK_PROVIDER */
    val provider: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,               // 位置数据的水平精度（水平方向的不确定性）数值越小越精确 [3.0：高精度（如GPS在开阔环境） 15.0：中等精度（如城市中的网络定位） 50.0+：低精度（如室内WiFi定位）]
    val countryName: String? = null,
    val adminArea: String? = null,     // 省/直辖市
    val locality: String? = null,      // 市
    val subLocality: String? = null,   // 区/县
    val thoroughfare: String? = null,  // 街道
    val featureName: String? = null,   // 门牌或兴趣点
    val postalCode: String? = null,    // 邮编
    val fullAddress: String? = null    // getAddressLine(0)
)

data class LocationFailure(
    val code: Code,
    val message: String? = null
) {
    enum class Code {
        TIMEOUT,           // 超时
        IO_ERROR,          // Geocoder IO 异常
        EMPTY_RESULT,      // Geocoder 无结果
        PROVIDER_DISABLED, // 当前 provider 不可用
        PERMISSION_DENIED, // 未授予定位权限
        UNKNOWN            // 其他
    }
}

interface OnLocationCallback {
    fun onLocationRetrieved(detail: LocationDetail)
    fun onLocationFailed(failure: LocationFailure)
}

/* ---------- Helper 本体 ---------- */

object LocationFetcher {

    private const val LOCATION_TIMEOUT = 3_000L
    private const val MAX_RETRY_COUNT = 2
    // 实时跟踪超时常量
    private const val REALTIME_TRACKING_TIMEOUT = 15_000L // 15秒超时

    private val DEFAULT_PROVIDERS = arrayOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )

    // 周期性任务相关变量
    private var periodicJob: Job? = null
    private var periodicIntervalMs = 10_000L // 默认10秒

    // 实时监听相关变量
    private var realtimeListener: LocationListenerCompat? = null
    // 位置过滤参数
    private var lastLocation: Location? = null
    private const val MIN_DISTANCE_CHANGE = 5f // 最小位置变化（米）
    private const val MIN_TIME_CHANGE = 1000L // 最小时间间隔（毫秒）
    private val PROVIDER_PRIORITY = mapOf(
        LocationManager.GPS_PROVIDER to 3,
        LocationManager.NETWORK_PROVIDER to 2,
        LocationManager.PASSIVE_PROVIDER to 1
    )
    // 实时跟踪超时相关变量
    private var realtimeTimeoutJob: Job? = null
    private var locationUpdateReceived = false

    /**
     * 调试日志开关
     * true: 输出详细调试日志
     * false: 仅输出关键错误日志
     */
    @JvmField
    var DEBUG = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val geocoder by lazy(LazyThreadSafetyMode.NONE) {
        Geocoder(appCtx, Locale.getDefault())
    }

    /* ===== Kotlin suspend 版 ===== */
    @Throws(IOException::class, TimeoutCancellationException::class, CancellationException::class)
    suspend fun retrieveLocation(
        providers: Array<out String> = DEFAULT_PROVIDERS
    ): LocationDetail = coroutineScope {
        var lastError: Throwable? = null
        repeat(MAX_RETRY_COUNT + 1) { attempt ->
            logDebug("Attempt ${attempt + 1}/${MAX_RETRY_COUNT + 1} to get location")
            for (provider in providers) {
                try {
                    return@coroutineScope requestOnce(provider)
                } catch (e: Throwable) {
                    lastError = e
                    logError("Provider $provider failed: ${e.message}")
                }
            }
        }
        throw lastError ?: IOException("Unknown error")
    }

    /* ===== Java / Kotlin 回调 版 ===== */
    @JvmOverloads
    @JvmStatic
    fun execute(
        callback: OnLocationCallback,
        providers: Array<out String> = DEFAULT_PROVIDERS
    ) {
        scope.launch {
            try {
                val detail = retrieveLocation(providers)
                callback.onLocationRetrieved(detail)
            } catch (t: Throwable) {
                callback.onLocationFailed(t.toFailure())
            }
        }
    }

    /* ===== 取消全部请求 ===== */
    @JvmStatic
    fun cancel() = scope.cancel()

    /* ===== 周期性位置更新 ===== */
    /**
     * 开始周期性获取位置信息
     * @param intervalMs 获取间隔时间(毫秒)，默认10秒
     * @param providers 位置提供者数组
     * @param callback 位置回调接口
     */
    @JvmOverloads
    @JvmStatic
    fun startPeriodicLocationUpdates(
        intervalMs: Long = 10_000L,
        providers: Array<out String> = DEFAULT_PROVIDERS,
        callback: OnLocationCallback
    ) {
        // 先停止可能存在的周期性任务
        stopPeriodicLocationUpdates()
        periodicIntervalMs = intervalMs

        // 启动新的周期性任务
        periodicJob = scope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                try {
                    val detail = retrieveLocation(providers)
                    callback.onLocationRetrieved(detail)
                } catch (t: Throwable) {
                    val failure = t.toFailure()
                    // 增强错误信息
                    val errorMsg = if (failure.code == LocationFailure.Code.PROVIDER_DISABLED) {
                        "位置提供者不可用，请检查系统定位设置"
                    } else {
                        failure.message ?: "获取位置信息失败"
                    }
                    callback.onLocationFailed(failure.copy(message = errorMsg))
                }
                // 等待指定间隔后再执行下一次获取
                delay(periodicIntervalMs)
            }
        }
        logDebug("已启动周期性位置更新，间隔: ${intervalMs}ms")
    }

    /**
     * 停止周期性位置更新
     */
    @JvmStatic
    fun stopPeriodicLocationUpdates() {
        periodicJob?.cancel()
        periodicJob = null
        logDebug("已停止周期性位置更新")
    }

    /* ===== 实时位置更新 ===== */
    /**
     * 启动实时位置跟踪（位置变化时立即更新）
     * @param minTimeMs 最小时间间隔(毫秒)，默认2000ms
     * @param minDistanceM 最小距离变化(米)，默认5米
     * @param providers 位置提供者数组
     * @param callback 位置回调接口
     */
    @JvmStatic
    @JvmOverloads
    @SuppressLint("MissingPermission")
    fun startRealtimeLocationTracking(
        minTimeMs: Long = 2000L,
        minDistanceM: Float = 5f,
        providers: Array<out String> = DEFAULT_PROVIDERS,
        callback: OnLocationCallback
    ) {
        stopRealtimeLocationTracking() // 先停止已有监听
        locationUpdateReceived = false
        realtimeTimeoutJob?.cancel()

        // 启动超时检查协程
        realtimeTimeoutJob = scope.launch(Dispatchers.Main.immediate) {
            delay(REALTIME_TRACKING_TIMEOUT)
            if (!locationUpdateReceived) {
                logError("实时位置跟踪超时，${REALTIME_TRACKING_TIMEOUT}ms内未收到位置更新")
                callback.onLocationFailed(LocationFailure(
                    LocationFailure.Code.TIMEOUT,
                    "实时位置跟踪超时"
                ))
                stopRealtimeLocationTracking()
            }
        }

        realtimeListener = LocationListenerCompat { newLocation ->
            // 收到位置更新，取消超时任务
            realtimeTimeoutJob?.cancel()
            locationUpdateReceived = true

            // 时间过滤
            val last = lastLocation
            if (last != null && newLocation.time - last.time < MIN_TIME_CHANGE) {
                logDebug("时间间隔过短，忽略更新")
                return@LocationListenerCompat
            }

            // 距离过滤
            if (last != null && newLocation.distanceTo(last) < MIN_DISTANCE_CHANGE) {
                logDebug("位置变化过小，忽略更新")
                return@LocationListenerCompat
            }

            // Provider优先级过滤
            val newPriority = PROVIDER_PRIORITY[newLocation.provider] ?: 0
            if (last != null) {
                val lastPriority = PROVIDER_PRIORITY[last.provider] ?: 0
                if (newPriority < lastPriority && newLocation.distanceTo(last) < MIN_DISTANCE_CHANGE) {
                    logDebug("低优先级Provider，忽略更新")
                    return@LocationListenerCompat
                }
            }

            // 通过过滤，更新位置并处理
            lastLocation = newLocation
            scope.launch(Dispatchers.IO) {
                try {
                    // 复用现有地理编码逻辑
                    val addrList =
                        geocoder.getFromLocation(newLocation.latitude, newLocation.longitude, 3)
                    val addr =
                        addrList?.firstOrNull() ?: throw NoSuchElementException("Geocoder empty")
                    val detail = LocationDetail(
                        provider = newLocation.provider ?: "unknown",
                        latitude = newLocation.latitude,
                        longitude = newLocation.longitude,
                        accuracy = newLocation.accuracy,
                        countryName = addr.countryName,
                        adminArea = addr.adminArea,
                        locality = addr.locality,
                        subLocality = addr.subLocality,
                        thoroughfare = addr.thoroughfare,
                        featureName = addr.featureName,
                        postalCode = addr.postalCode,
                        fullAddress = addr.getAddressLine(0)
                    )
                    withContext(Dispatchers.Main.immediate) {
                        callback.onLocationRetrieved(detail)
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main.immediate) {
                        callback.onLocationFailed(e.toFailure())
                    }
                }
            }
        }

        // 统计成功注册的provider数量
        var providersRegistered = 0

        // 注册位置监听器
        providers.forEach { provider ->
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    minTimeMs,
                    minDistanceM,
                    realtimeListener!!
                )
                providersRegistered++
                logDebug("已启动实时位置跟踪: $provider (minTime: $minTimeMs ms, minDistance: $minDistanceM m)")
            } catch (e: SecurityException) {
                callback.onLocationFailed(
                    LocationFailure(
                        LocationFailure.Code.PERMISSION_DENIED,
                        e.message
                    )
                )
            } catch (e: IllegalArgumentException) {
                logError("Provider $provider 不可用: ${e.message}")
            }
        }

        // 检查是否有provider成功注册
        if (providersRegistered == 0) {
            logError("所有位置提供者均不可用，无法启动实时位置跟踪")
            callback.onLocationFailed(
                LocationFailure(
                    LocationFailure.Code.PROVIDER_DISABLED,
                    "所有位置提供者均不可用"
                )
            )
            stopRealtimeLocationTracking()
        }
    }

    /**
     * 停止实时位置跟踪
     */
    @JvmStatic
    fun stopRealtimeLocationTracking() {
        realtimeListener?.let {
            locationManager.removeUpdates(it)
            realtimeListener = null
            logDebug("已停止实时位置跟踪")
        }
        // 取消超时任务
        realtimeTimeoutJob?.cancel()
        realtimeTimeoutJob = null
    }

    /* ---------- 内部实现 ---------- */

    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun requestOnce(provider: String): LocationDetail =
        suspendCancellableCoroutine { cont ->
            val done = AtomicBoolean(false)

            val listener = object : LocationListenerCompat {
                override fun onLocationChanged(location: Location) {
                    if (done.getAndSet(true)) return
                    scope.launch(Dispatchers.IO) {
                        try {
                            val addrList = geocoder.getFromLocation(
                                location.latitude, location.longitude, 3
                            )
                            val addr = addrList?.firstOrNull()
                                ?: throw NoSuchElementException("Geocoder empty")
                            val detail = LocationDetail(
                                provider = provider,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                countryName = addr.countryName,
                                adminArea = addr.adminArea,
                                locality = addr.locality,
                                subLocality = addr.subLocality,
                                thoroughfare = addr.thoroughfare,
                                featureName = addr.featureName,
                                postalCode = addr.postalCode,
                                fullAddress = addr.getAddressLine(0)
                            )
                            withContext(Dispatchers.Main.immediate) {
                                if (cont.isActive) cont.resume(detail) {}
                            }
                        } catch (e: Throwable) {
                            withContext(Dispatchers.Main.immediate) {
                                if (cont.isActive) cont.resumeWithException(e)
                            }
                        }
                    }
                    locationManager.removeUpdates(this)
                }
            }

            try {
                locationManager.requestLocationUpdates(
                    provider, 0L, 0f, listener
                )
            } catch (e: SecurityException) {  // 未授权
                cont.resumeWithException(e)
                return@suspendCancellableCoroutine
            } catch (e: IllegalArgumentException) { // provider 不可用
                cont.resumeWithException(e)
                return@suspendCancellableCoroutine
            }

            scope.launch {
                var elapsedTime = 0L
                while (elapsedTime < LOCATION_TIMEOUT) {
                    delay(1000L)
                    elapsedTime += 1000L
                    if (!done.get()) {
                        logDebug("Waiting for location update from $provider. Elapsed time: $elapsedTime ms")
                    } else {
                        break
                    }
                }
                delay(LOCATION_TIMEOUT)
                if (done.getAndSet(true).not() && cont.isActive) {
                    logError("Location request timed out for provider $provider after ${LOCATION_TIMEOUT}ms")
                    locationManager.removeUpdates(listener)
                    cont.resumeWithException(
                        TimeoutCancellationException("Location timeout")
                    )
                }
            }

            cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
        }

    /**
     * 调试日志输出（仅DEBUG=true时生效）
     */
    private fun logDebug(message: String) {
        if (DEBUG) {
            Log.d("LocationHelper", message)
        }
    }

    /**
     * 错误日志输出（始终生效）
     */
    private fun logError(message: String) {
        Log.e("LocationHelper", message)
    }

    /* ---------- Throwable -> LocationFailure ---------- */

    private fun Throwable.toFailure(): LocationFailure = when (this) {
        is TimeoutCancellationException -> LocationFailure(
            LocationFailure.Code.TIMEOUT, message
        )

        is IOException -> LocationFailure(
            LocationFailure.Code.IO_ERROR, message
        )

        is SecurityException -> LocationFailure(
            LocationFailure.Code.PERMISSION_DENIED, message
        )

        is NoSuchElementException -> LocationFailure(
            LocationFailure.Code.EMPTY_RESULT, message
        )

        is IllegalArgumentException -> LocationFailure(
            LocationFailure.Code.PROVIDER_DISABLED, message
        )

        else -> LocationFailure(LocationFailure.Code.UNKNOWN, message ?: toString())
    }
}
