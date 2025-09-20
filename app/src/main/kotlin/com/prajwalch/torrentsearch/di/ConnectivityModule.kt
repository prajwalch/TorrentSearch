package com.prajwalch.torrentsearch.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService

import com.prajwalch.torrentsearch.network.ConnectivityChecker

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ConnectivityModule {
    @Provides
    fun provideConnectivityChecker(
        connectivityManager: ConnectivityManager,
    ): ConnectivityChecker = ConnectivityChecker(
        connectivityManager = connectivityManager
    )

    @Provides
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager =
        context.getSystemService<ConnectivityManager>()!!
}