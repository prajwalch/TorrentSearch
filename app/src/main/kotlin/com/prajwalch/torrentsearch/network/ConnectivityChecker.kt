package com.prajwalch.torrentsearch.network

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

import javax.inject.Inject

/** Provides internet connection status. */
class ConnectivityChecker @Inject constructor(
    private val connectivityManager: ConnectivityManager,
) {
    fun isInternetAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val activeNetworkCapabilities = connectivityManager
            .getNetworkCapabilities(activeNetwork)
            ?: return false

        val hasInternet = activeNetworkCapabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
        val isValidated = activeNetworkCapabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )

        return hasInternet && isValidated
    }
}