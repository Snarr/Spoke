package com.jacobsnarr.spoke.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectivityRepository(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOffline = MutableStateFlow(!hasInternetConnectivity())
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOffline.value = !hasInternetConnectivity()
            }

            override fun onLost(network: Network) {
                _isOffline.value = !hasInternetConnectivity()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                _isOffline.value =
                    !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }

    init {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    private fun hasInternetConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
