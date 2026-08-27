package io.core.engine.location

import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import androidx.core.location.LocationListenerCompat
import io.core.appCtx
import io.core.engine.location.LocationLogger.debug
import io.core.engine.location.LocationLogger.error
import io.core.common.util.extensions.locationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationTracker(
    private val config: LocationConfig = LocationConfig()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val geocoder by lazy(LazyThreadSafetyMode.NONE) {
        Geocoder(appCtx, Locale.getDefault())
    }

    private val sharedProvider by lazy { LocationProvider(config) }

    private var periodicJob: Job? = null
    private var realtimeListener: LocationListenerCompat? = null
    private var lastLocation: Location? = null
    private var realtimeTimeoutJob: Job? = null
    private var locationUpdateReceived = false

    init {
        LocationLogger.setDebugEnabled(config.debug)
    }

    fun startPeriodicUpdates(
        intervalMs: Long = config.periodicIntervalMs,
        providers: List<String> = config.providers,
        callback: OnLocationCallback
    ) {
        stopPeriodicUpdates()

        periodicJob = scope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                try {
                    val detail = sharedProvider.retrieveLocation(providers.toTypedArray())
                    callback.onLocationRetrieved(detail)
                } catch (t: Throwable) {
                    val failure = t.toFailure()
                    val errorMsg = if (failure.code == LocationFailure.Code.PROVIDER_DISABLED) {
                        "位置提供者不可用，请检查系统定位设置"
                    } else {
                        failure.message ?: "获取位置信息失败"
                    }
                    callback.onLocationFailed(failure.copy(message = errorMsg))
                }
                delay(intervalMs)
            }
        }
        debug("已启动周期性位置更新，间隔: ${intervalMs}ms")
    }

    fun stopPeriodicUpdates() {
        periodicJob?.cancel()
        periodicJob = null
        debug("已停止周期性位置更新")
    }

    @SuppressLint("MissingPermission")
    fun startRealtimeTracking(
        minTimeMs: Long = config.minTimeChange,
        minDistanceM: Float = config.minDistanceChange,
        providers: List<String> = config.providers,
        callback: OnLocationCallback
    ) {
        stopRealtimeTracking()
        locationUpdateReceived = false
        realtimeTimeoutJob?.cancel()

        realtimeTimeoutJob = scope.launch(Dispatchers.Main.immediate) {
            delay(config.realtimeTrackingTimeoutMs)
            if (!locationUpdateReceived) {
                error("实时位置跟踪超时，${config.realtimeTrackingTimeoutMs}ms内未收到位置更新")
                callback.onLocationFailed(
                    LocationFailure(
                    LocationFailure.Code.TIMEOUT,
                    "实时位置跟踪超时"
                )
                )
                stopRealtimeTracking()
            }
        }

        realtimeListener = LocationListenerCompat { newLocation ->
            realtimeTimeoutJob?.cancel()
            locationUpdateReceived = true

            val last = lastLocation
            if (last != null && newLocation.time - last.time < config.minTimeChange) {
                debug("时间间隔过短，忽略更新")
                return@LocationListenerCompat
            }

            if (last != null && newLocation.distanceTo(last) < config.minDistanceChange) {
                debug("位置变化过小，忽略更新")
                return@LocationListenerCompat
            }

            val newPriority = config.providerPriority[newLocation.provider] ?: 0
            if (last != null) {
                val lastPriority = config.providerPriority[last.provider] ?: 0
                if (newPriority < lastPriority && newLocation.distanceTo(last) < config.minDistanceChange) {
                    debug("低优先级Provider，忽略更新")
                    return@LocationListenerCompat
                }
            }

            lastLocation = newLocation
            scope.launch(Dispatchers.IO) {
                try {
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

        var providersRegistered = 0

        providers.forEach { provider ->
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    minTimeMs,
                    minDistanceM,
                    realtimeListener!!
                )
                providersRegistered++
                debug("已启动实时位置跟踪: $provider (minTime: $minTimeMs ms, minDistance: $minDistanceM m)")
            } catch (e: SecurityException) {
                callback.onLocationFailed(
                    LocationFailure(
                        LocationFailure.Code.PERMISSION_DENIED,
                        e.message
                    )
                )
            } catch (e: IllegalArgumentException) {
                error("Provider $provider 不可用: ${e.message}")
            }
        }

        if (providersRegistered == 0) {
            error("所有位置提供者均不可用，无法启动实时位置跟踪")
            callback.onLocationFailed(
                LocationFailure(
                    LocationFailure.Code.PROVIDER_DISABLED,
                    "所有位置提供者均不可用"
                )
            )
            stopRealtimeTracking()
        }
    }

    fun stopRealtimeTracking() {
        realtimeListener?.let {
            locationManager.removeUpdates(it)
            realtimeListener = null
            debug("已停止实时位置跟踪")
        }
        realtimeTimeoutJob?.cancel()
        realtimeTimeoutJob = null
    }

    fun cancel() {
        stopPeriodicUpdates()
        stopRealtimeTracking()
        scope.cancel()
    }

    private fun Throwable.toFailure(): LocationFailure = when (this) {
        is java.io.IOException -> LocationFailure(
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
