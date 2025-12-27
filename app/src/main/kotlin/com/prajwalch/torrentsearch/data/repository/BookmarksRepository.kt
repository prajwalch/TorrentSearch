package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.local.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.local.entities.toDomain
import com.prajwalch.torrentsearch.data.local.entities.toEntity
import com.prajwalch.torrentsearch.domain.models.Torrent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject

class BookmarksRepository @Inject constructor(private val dao: BookmarkedTorrentDao) {
    fun observeAllBookmarks(): Flow<List<Torrent>> {
        return dao.observeAll().map { it.toDomain() }
    }

    suspend fun bookmarkTorrent(torrent: Torrent) {
        dao.insert(bookmarkedTorrent = torrent.toEntity())
    }

    suspend fun deleteBookmarkedTorrent(torrent: Torrent) {
        dao.delete(bookmarkedTorrent = torrent.toEntity())
    }

    suspend fun deleteAllBookmarks() {
        dao.deleteAll()
    }
}