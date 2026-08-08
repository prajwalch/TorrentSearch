package com.prajwalch.torrentsearch.di

/** All the Koin modules used across the application. */
val appModules = listOf(
    networkModule,
    dataStoreModule,
    databaseModule,
    builtinSearchProvidersModule,
    repositoryModule,
    domainModule,
    viewModelModule,
)
