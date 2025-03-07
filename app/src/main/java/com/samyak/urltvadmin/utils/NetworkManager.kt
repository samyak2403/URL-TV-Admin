package com.samyak.urltvadmin.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NetworkManager(private val context: Context) {

    enum class ConnectionStatus {
        CONNECTED_EXCELLENT, CONNECTED_GOOD, CONNECTED_POOR, DISCONNECTED, CHECKING
    }

    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            checkConnectionQuality(network)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            checkConnectionQuality(network)
        }

        override fun onLost(network: Network) {
            _connectionStatus.postValue(ConnectionStatus.DISCONNECTED)
        }
    }

    init {
        _connectionStatus.value = ConnectionStatus.CHECKING
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun checkConnectionQuality(network: Network) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    // WiFi connection is typically excellent
                    _connectionStatus.postValue(ConnectionStatus.CONNECTED_EXCELLENT)
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    // Check cellular signal strength (simplified)
                    val signalStrength = capabilities.signalStrength
                    if (signalStrength != null && signalStrength > 70) {
                        _connectionStatus.postValue(ConnectionStatus.CONNECTED_EXCELLENT)
                    } else if (signalStrength != null && signalStrength > 40) {
                        _connectionStatus.postValue(ConnectionStatus.CONNECTED_GOOD)
                    } else {
                        _connectionStatus.postValue(ConnectionStatus.CONNECTED_POOR)
                    }
                } else {
                    // Other connection types (Bluetooth, Ethernet, etc.)
                    _connectionStatus.postValue(ConnectionStatus.CONNECTED_GOOD)
                }
            } else {
                _connectionStatus.postValue(ConnectionStatus.DISCONNECTED)
            }
        } else {
            // For older Android versions, just report connected
            _connectionStatus.postValue(ConnectionStatus.CONNECTED_GOOD)
        }
    }

    fun checkInitialConnectionStatus() {
        _connectionStatus.value = ConnectionStatus.CHECKING
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            if (network != null) {
                checkConnectionQuality(network)
            } else {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnected) {
                _connectionStatus.value = ConnectionStatus.CONNECTED_GOOD
            } else {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        }
    }

    fun unregisterNetworkCallback() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Network callback was not registered or already unregistered
        }
    }
} 