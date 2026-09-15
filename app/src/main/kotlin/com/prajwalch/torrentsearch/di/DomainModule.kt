package com.prajwalch.torrentsearch.di

import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.domain.TorrentFileDownloader
import com.prajwalch.torrentsearch.domain.TorrentQueryService

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
        TorrentQueryService(
            searchProvidersManager = get(),
            settingsRepository = get(),
        )
    }
    factory { TorrentFileDownloader(networkClient = get()) }
}