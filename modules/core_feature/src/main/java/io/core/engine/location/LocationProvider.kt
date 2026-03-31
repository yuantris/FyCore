package io.core.engine.location

import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import androidx.core.location.LocationListenerCompat
import io.core.appCtx
import io.core.engine.location.LocationLogger.debug
import io.core.engine.location.LocationLogger.error
import io.core.common.util.extensions.cool.TimeoutCancellationException
import io.core.common.util.extensions.locationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resumeWithException

class LocationProvider(
    private val config: LocationConfig = LocationConfig()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val geocoder by lazy(LazyThreadSafetyMode.NONE) {
        Geocoder(appCtx, Locale.getDefault())
    }

    init {
        LocationLogger.setDebugEnabled(config.debug)
    }

    @Throws(IOException::class, TimeoutCancellationException::class, CancellationException::class)
    suspend fun retrieveLocation(
        providers: Array<String> = config.providers.toTypedArray()
    ): LocationDetail = coroutineScope {
        var lastError: Throwable? = null
        repeat(config.maxRetryCount + 1) { attempt ->
            debug("Attempt ${attempt + 1}/${config.maxRetryCount + 1} to get location")
            for (provider in providers) {
                try {
                    return@coroutineScope requestOnce(provider)
                } catch (e: Throwable) {
                    lastError = e
                    error("Provider $provider failed: ${e.message}")
                }
            }
        }
        throw lastError ?: IOException("Unknown error")
    }

    fun execute(
        callback: OnLocationCallback,
        providers: Array<String> = config.providers.toTypedArray()
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

    fun cancel() = scope.cancel()

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
            } catch (e: SecurityException) {
                cont.resumeWithException(e)
                return@suspendCancellableCoroutine
            } catch (e: IllegalArgumentException) {
                cont.resumeWithException(e)
                return@suspendCancellableCoroutine
            }

            scope.launch {
                var elapsedTime = 0L
                while (elapsedTime < config.timeoutMs) {
                    delay(1000L)
                    elapsedTime += 1000L
                    if (!done.get()) {
                        debug("Waiting for location update from $provider. Elapsed time: $elapsedTime ms")
                    } else {
                        break
                    }
                }
                delay(config.timeoutMs)
                if (done.getAndSet(true).not() && cont.isActive) {
                    error("Location request timed out for provider $provider after ${config.timeoutMs}ms")
                    locationManager.removeUpdates(listener)
                    cont.resumeWithException(
                        TimeoutCancellationException("Location timeout")
                    )
                }
            }

            cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
        }

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
