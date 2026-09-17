package com.prajwalch.torrentsearch.di

import com.prajwalch.torrentsearch.domain.SearchProviderManager
import com.prajwalch.torrentsearch.domain.TorrentFileDownloader
import com.prajwalch.torrentsearch.domain.TorrentQueryService

import org.koin.dsl.module

val domainModule = module {
    single {
        SearchProviderManager(
            builtinProviders = get(),
            torznabConfigRepository = get(),
            settingsRepository = get(),
            networkClient = get(),
        )
    }
    single {
        TorrentQueryService(
            searchProviderManager = get(),
            settingsRepository = get(),
        )
    }
    factory { TorrentFileDownloader(networkClient = get()) }
}