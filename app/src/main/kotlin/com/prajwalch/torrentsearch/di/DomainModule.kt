package com.prajwalch.torrentsearch.di

import com.prajwalch.torrentsearch.domain.SearchProvidersGateway
import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.domain.TorrentFileDownloader

import org.koin.dsl.module

val domainModule = module {
    single {
        SearchProvidersManager(
            builtinProviders = get(),
            torznabConfigRepository = get(),
            settingsRepository = get(),
            networkClient = get(),
        )
    }
    single {
        SearchProvidersGateway(
            searchProvidersManager = get(),
            settingsRepository = get(),
        )
    }
    factory { TorrentFileDownloader(networkClient = get()) }
}
