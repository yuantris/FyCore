package io.core.common.helper

import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.location.LocationListenerCompat
import io.core.appCtx
import io.core.common.util.extensions.locationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

object LocationHelper : CoroutineScope by MainScope() {

    private const val LOCATION_TIMEOUT = 3000L // 3秒超时
    private const val MAX_RETRY_COUNT = 2 // 最大重试次数

    private val geocoder by lazy { Geocoder(appCtx, Locale.getDefault()) }

    /**
     * 执行位置请求
     * @param callback 回调接口
     * @param providers 位置提供者数组
     * @param retryCount 当前重试次数(内部使用)
     */
    @JvmStatic
    @JvmOverloads
    fun execute(
        callback: OnLocationCallback,
        providers: Array<out String> = arrayOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ),
        retryCount: Int = 0
    ) {
        var currentIndex = 0
        val attemptNext = object : OnLocationCallback {
            override fun onLocationSuccess(address: Array<String>) {
                callback.onLocationSuccess(address)
            }

            override fun onFailed() {
                if (++currentIndex < providers.size) {
                    requestLocation(providers[currentIndex], this)
                } else if (retryCount < MAX_RETRY_COUNT) {
                    // 所有provider都失败后，延迟1秒重试
                    launch {
                        delay(1000)
                        execute(callback, providers, retryCount + 1)
                    }
                } else {
                    callback.onFailed()
                }
            }
        }
        requestLocation(providers[currentIndex], attemptNext)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocation(provider: String, callback: OnLocationCallback) {
        var callbackExecuted = false
        val lock = Any()

        val locationListener = object : LocationListenerCompat {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                synchronized(lock) {
                    if (callbackExecuted) return
                    callbackExecuted = true

                    launch(IO) {
                        try {
                            val addresses = geocoder.getFromLocation(
                                location.latitude,
                                location.longitude,
                                3
                            ) ?: emptyList()

                            withContext(Main) {
                                addresses.firstOrNull()?.let { address ->
                                    callback.onLocationSuccess(
                                        arrayOf(
                                            address.adminArea.orEmpty(),
                                            address.locality.orEmpty(),
                                            address.subLocality.orEmpty()
                                        )
                                    )
                                } ?: callback.onFailed()
                            }
                        } catch (e: IOException) {
                            withContext(Main) { callback.onFailed() }
                        }
                    }
                }
            }
        }

        locationManager.requestLocationUpdates(provider, 0L, 0f, locationListener)

        // 超时处理
        launch(IO) {
            delay(LOCATION_TIMEOUT)
            synchronized(lock) {
                if (!callbackExecuted) {
                    callbackExecuted = true
                    locationManager.removeUpdates(locationListener)
                    launch(Main) { callback.onFailed() }
                }
            }
        }
    }

    interface OnLocationCallback {
        fun onLocationSuccess(address: Array<String>)
        fun onFailed()
    }
}