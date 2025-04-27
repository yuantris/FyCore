@file:Suppress("MissingPermission")

package io.core.common.helper.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission
import io.core.appCtx
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first

/**
 * 简化版网络状态监控
 */
class NetworkMonitor private constructor() {

    private val connectivityManager by lazy {
        appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val _networkState = MutableStateFlow(getCurrentState())
    val networkState = _networkState.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _networkState.value = capabilities.toNetworkState(network)
        }

        override fun onLost(network: Network) {
            _networkState.value = NetworkState.Disconnected
        }
    }

    @RequiresPermission("android.permission.ACCESS_NETWORK_STATE")
    fun start() {
        connectivityManager.registerDefaultNetworkCallback(callback)
        _networkState.value = getCurrentState()
    }

    fun stop() {
        connectivityManager.unregisterNetworkCallback(callback)
    }

    fun getCurrentState(): NetworkState {
        return connectivityManager.activeNetwork?.let { network ->
            connectivityManager.getNetworkCapabilities(network)
                ?.toNetworkState(network)
        } ?: NetworkState.Disconnected
    }

    /**
     * 等待网络满足条件
     * @param condition 等待的条件，默认为等待任何网络连接
     */
    suspend fun awaitNetwork(condition: (NetworkState) -> Boolean = { it.isConnected }): Boolean {
        if (condition(getCurrentState())) return true
        return networkState
            .dropWhile { !condition(it) }
            .first()
            .let { true }
    }

    companion object {

        @Volatile
        private var instance: NetworkMonitor? = null

        @RequiresPermission("android.permission.ACCESS_NETWORK_STATE")
        fun initialize(): NetworkMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkMonitor().also {
                    instance = it
                    it.start()
                }
            }
        }

        fun get(): NetworkMonitor {
            return instance ?: throw IllegalStateException(
                "NetworkMonitor not initialized. Call initialize() first."
            )
        }
    }
}

/**
 * 网络状态
 */
sealed class NetworkState {
    abstract val isConnected: Boolean
    abstract val isWifi: Boolean
    abstract val isCellular: Boolean

    data class Connected(
        override val isWifi: Boolean,
        override val isCellular: Boolean
    ) : NetworkState() {
        override val isConnected: Boolean = true
    }

    object Disconnected : NetworkState() {
        override val isConnected: Boolean = false
        override val isWifi: Boolean = false
        override val isCellular: Boolean = false
    }
}

/**
 * NetworkCapabilities扩展函数
 */
private fun NetworkCapabilities.toNetworkState(network: Network): NetworkState {
    return if (hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
        NetworkState.Connected(
            isWifi = hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
            isCellular = hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        )
    } else {
        NetworkState.Disconnected
    }
}

/**
 * 全局便捷访问方法
 */
val currentNetworkState get() = NetworkMonitor.get().networkState
fun getCurrentNetworkState() = NetworkMonitor.get().getCurrentState()

/**
 * 全局等待网络连接
 * @param condition 等待的条件，默认为等待任何网络连接
 */
suspend fun awaitNetwork(condition: (NetworkState) -> Boolean = { it.isConnected }): Boolean {
    return NetworkMonitor.get().awaitNetwork(condition)
}