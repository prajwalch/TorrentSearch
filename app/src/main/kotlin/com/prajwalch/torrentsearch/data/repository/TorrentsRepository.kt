package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.local.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.local.entities.toDomain
import com.prajwalch.torrentsearch.data.local.entities.toEntity
import com.prajwalch.torrentsearch.data.remote.TorrentsRemoteDataSource
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

import javax.inject.Inject

class TorrentsRepository @Inject constructor(
    private val bookmarkedTorrentDao: BookmarkedTorrentDao,
    private val remoteDataSource: TorrentsRemoteDataSource,
) {
    fun observeAllBookmarks(): Flow<List<Torrent>> {
        return bookmarkedTorrentDao.observeAll().map { it.toDomain() }
    }

    suspend fun bookmarkTorrent(torrent: Torrent) {
        bookmarkedTorrentDao.insert(bookmarkedTorrent = torrent.toEntity())
    }

    suspend fun deleteBookmarkedTorrent(torrent: Torrent) {
        bookmarkedTorrentDao.delete(bookmarkedTorrent = torrent.toEntity())
    }

    suspend fun deleteAllBookmarks() {
        bookmarkedTorrentDao.deleteAll()
    }

    fun getSearchResultsCache(): List<Torrent> {
        return remoteDataSource.getCache()
    }

    fun search(
        query: String,
        category: Category,
        searchProviders: List<SearchProvider>,
    ): Flow<Result<List<Torrent>>> {
        return remoteDataSource
            .searchTorrents(
                query = query,
                category = category,
                searchProviders = searchProviders,
            )
            .flowOn(Dispatchers.IO)
    }
}