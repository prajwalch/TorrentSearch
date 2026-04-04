package com.prajwalch.torrentsearch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.prajwalch.torrentsearch.data.local.entities.ViewedTorrentEntity

import kotlinx.coroutines.flow.Flow

@Dao
interface ViewedTorrentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViewedTorrent(entity: ViewedTorrentEntity)

    @Query("SELECT id FROM viewed_torrents")
    fun getAllViewedHashes(): Flow<List<String>>

    @Query("DELETE FROM viewed_torrents")
    suspend fun deleteAllViewedTorrents()
}