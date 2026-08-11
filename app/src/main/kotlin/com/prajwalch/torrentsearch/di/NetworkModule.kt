package com.prajwalch.torrentsearch.di

import android.net.ConnectivityManager

import androidx.core.content.getSystemService

import com.prajwalch.torrentsearch.network.ConnectivityChecker
import com.prajwalch.torrentsearch.network.NetworkClient

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val networkModule = module {
    single<ConnectivityManager> {
        androidContext().getSystemService<ConnectivityManager>()!!
    }
    single { ConnectivityChecker(connectivityManager = get()) }
    single { NetworkClient(settingsRepository = get()) }
}
