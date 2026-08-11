package com.prajwalch.torrentsearch.di

/** All the classic Koin DSL modules used across the application. */
val appModules = listOf(
    networkModule,
    dataStoreModule,
    databaseModule,
    builtinSearchProvidersModule,
    repositoryModule,
    domainModule,
)
