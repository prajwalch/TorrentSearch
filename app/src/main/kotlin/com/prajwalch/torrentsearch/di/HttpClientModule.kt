package com.prajwalch.torrentsearch.di

import com.prajwalch.torrentsearch.network.HttpClient

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HttpClientModule {
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient
}