package com.prajwalch.torrentsearch.di

import com.prajwalch.torrentsearch.ui.bookmarks.BookmarksViewModel
import com.prajwalch.torrentsearch.ui.browse.BrowseViewModel
import com.prajwalch.torrentsearch.ui.home.HomeViewModel
import com.prajwalch.torrentsearch.ui.main.MainViewModel
import com.prajwalch.torrentsearch.ui.search.SearchViewModel
import com.prajwalch.torrentsearch.ui.searchhistory.SearchHistoryViewModel
import com.prajwalch.torrentsearch.ui.settings.SettingsViewModel
import com.prajwalch.torrentsearch.ui.settings.defaultcategory.DefaultCategoryViewModel
import com.prajwalch.torrentsearch.ui.settings.defaultsortoptions.DefaultSortOptionsViewModel
import com.prajwalch.torrentsearch.ui.settings.searchproviders.SearchProvidersViewModel
import com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.TorznabConfigViewModel
import com.prajwalch.torrentsearch.ui.torrentdetails.TorrentDetailsViewModel

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        BookmarksViewModel(
            bookmarkRepository = get(),
            settingsRepository = get(),
            torrentFileDownloader = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        BrowseViewModel(
            searchProvidersGateway = get(),
            connectivityChecker = get(),
            settingsRepository = get(),
            savedStateHandle = get(),
            bookmarkRepository = get(),
            viewedTorrentRepository = get(),
            torrentFileDownloader = get(),
        )
    }
    viewModel {
        HomeViewModel(
            searchHistoryRepository = get(),
            settingsRepository = get(),
            searchProvidersManager = get(),
        )
    }
    viewModel { MainViewModel(settingsRepository = get()) }
    viewModel {
        SearchViewModel(
            searchProvidersGateway = get(),
            bookmarkRepository = get(),
            searchHistoryRepository = get(),
            settingsRepository = get(),
            viewedTorrentRepository = get(),
            connectivityChecker = get(),
            torrentFileDownloader = get(),
            savedStateHandle = get(),
        )
    }
    viewModel { SearchHistoryViewModel(searchHistoryRepository = get()) }
    viewModel {
        SettingsViewModel(
            settingsRepository = get(),
            searchProvidersManager = get(),
            viewedTorrentRepository = get(),
        )
    }
    viewModel { DefaultCategoryViewModel(settingsRepository = get()) }
    viewModel { DefaultSortOptionsViewModel(settingsRepository = get()) }
    viewModel {
        SearchProvidersViewModel(
            searchProvidersManager = get(),
            settingsRepository = get(),
        )
    }
    viewModel {
        TorznabConfigViewModel(
            searchProvidersManager = get(),
            networkClient = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        TorrentDetailsViewModel(
            searchProvidersGateway = get(),
            torrentFileDownloader = get(),
            connectivityChecker = get(),
            settingsRepository = get(),
            savedStateHandle = get(),
        )
    }
}
