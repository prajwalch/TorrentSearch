package com.prajwalch.torrentsearch.di

import android.content.Context

import com.prajwalch.torrentsearch.data.database.TorrentSearchDatabase
import com.prajwalch.torrentsearch.data.database.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.database.dao.SearchHistoryDao
import com.prajwalch.torrentsearch.data.database.dao.TorznabSearchProviderDao

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TorrentSearchDatabase =
        TorrentSearchDatabase.getInstance(context)

    @Provides
    fun provideBookmarkedTorrentDao(database: TorrentSearchDatabase): BookmarkedTorrentDao =
        database.bookmarkedTorrentDao()

    @Provides
    fun provideSearchHistoryDao(database: TorrentSearchDatabase): SearchHistoryDao =
        database.searchHistoryDao()

    @Provides
    fun provideTorznabSearchProviderDao(database: TorrentSearchDatabase): TorznabSearchProviderDao =
        database.torznabSearchProviderDao()
}