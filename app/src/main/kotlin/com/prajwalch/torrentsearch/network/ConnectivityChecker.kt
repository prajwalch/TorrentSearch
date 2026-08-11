package com.prajwalch.torrentsearch.network

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Provides internet connection status. */
class ConnectivityChecker(
    private val connectivityManager: ConnectivityManager,
) {
    suspend fun isInternetAvailable(): Boolean = withContext(Dispatchers.IO) {
        val activeNetwork = connectivityManager.activeNetwork
        val activeNetworkCapabilities = connectivityManager
            .getNetworkCapabilities(activeNetwork)
            ?: return@withContext false

        val hasInternet = activeNetworkCapabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
        val isValidated = activeNetworkCapabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )

        hasInternet && isValidated
    }
}