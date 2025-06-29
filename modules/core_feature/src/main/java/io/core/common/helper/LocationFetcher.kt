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
import kotlinx.coroutines.*
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
    val accuracy: Float,
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
    private const val MAX_RETRY_COUNT = 1
    private val DEFAULT_PROVIDERS = arrayOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )
    /**
     * 调试日志开关
     * true: 输出详细调试日志
     * false: 仅输出关键错误日志
     */
    @JvmField
    var DEBUG = false  // 可通过外部动态修改

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
        repeat(MAX_RETRY_COUNT + 1) {attempt ->
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
    @JvmStatic fun cancel() = scope.cancel()

    /* ---------- 内部实现 ---------- */

    @Suppress("DEPRECATION")
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
