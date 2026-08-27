package io.core.engine.location

data class LocationDetail(
    /** 实际触发回调的 provider 名称，如 GPS_PROVIDER / NETWORK_PROVIDER */
    val provider: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val countryName: String? = null,
    val adminArea: String? = null,
    val locality: String? = null,
    val subLocality: String? = null,
    val thoroughfare: String? = null,
    val featureName: String? = null,
    val postalCode: String? = null,
    val fullAddress: String? = null
)

data class LocationFailure(
    val code: Code,
    val message: String? = null
) {
    enum class Code {
        TIMEOUT,
        IO_ERROR,
        EMPTY_RESULT,
        PROVIDER_DISABLED,
        PERMISSION_DENIED,
        UNKNOWN
    }
}

interface OnLocationCallback {
    fun onLocationRetrieved(detail: LocationDetail)
    fun onLocationFailed(failure: LocationFailure)
}
