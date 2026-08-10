package com.prajwalch.torrentsearch.di

import org.koin.ksp.generated.module

/** All the Koin modules used across the application. */
val appModules = listOf(
    networkModule,
    dataStoreModule,
    databaseModule,
    builtinSearchProvidersModule,
    repositoryModule,
    domainModule,
    ViewModelAnnotationsModule().module,
)
