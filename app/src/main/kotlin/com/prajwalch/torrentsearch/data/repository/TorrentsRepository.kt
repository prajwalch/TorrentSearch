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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import javax.inject.Inject

typealias SearchResult = Result<List<Torrent>>

class TorrentsRepository @Inject constructor(
    private val bookmarkedTorrentDao: BookmarkedTorrentDao,
    private val remoteDataSource: TorrentsRemoteDataSource,
) {
    private val mutex = Mutex()
    private val cache = mutableListOf<SearchResult>()

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

    fun search(
        query: String,
        category: Category,
        searchProviders: List<SearchProvider>,
    ): Flow<List<SearchResult>> = flow {
        clearCache()

        remoteDataSource
            .searchTorrents(
                query = query,
                category = category,
                searchProviders = searchProviders,
            )
            .collect { emit(addAndGetCache(it)) }
    }.flowOn(Dispatchers.IO)

    private suspend fun addAndGetCache(searchResult: SearchResult): List<SearchResult> {
        return mutex.withLock {
            cache.add(searchResult)
            cache.toList()
        }
    }

    private suspend fun clearCache() {
        mutex.withLock { cache.clear() }
    }
}