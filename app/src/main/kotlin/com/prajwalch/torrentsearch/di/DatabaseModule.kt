package com.prajwalch.torrentsearch.di

import com.prajwalch.torrentsearch.data.local.TorrentSearchDatabase
import com.prajwalch.torrentsearch.data.local.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.local.dao.SearchHistoryDao
import com.prajwalch.torrentsearch.data.local.dao.TorznabConfigDao
import com.prajwalch.torrentsearch.data.local.dao.ViewedTorrentDao

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single<TorrentSearchDatabase> { TorrentSearchDatabase.getInstance(androidContext()) }

    single<BookmarkedTorrentDao> { get<TorrentSearchDatabase>().bookmarkedTorrentDao() }
    single<SearchHistoryDao> { get<TorrentSearchDatabase>().searchHistoryDao() }
    single<TorznabConfigDao> { get<TorrentSearchDatabase>().torznabConfigDao() }
    single<ViewedTorrentDao> { get<TorrentSearchDatabase>().viewedTorrentDao() }
}
