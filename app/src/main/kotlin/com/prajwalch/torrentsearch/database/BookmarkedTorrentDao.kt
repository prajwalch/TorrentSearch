package com.prajwalch.torrentsearch.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prajwalch.torrentsearch.database.entities.BookmarkedTorrent
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkedTorrentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmarkedTorrent: BookmarkedTorrent)

    @Query("SELECT * FROM bookmarks ORDER by id DESC")
    fun getAll(): Flow<List<BookmarkedTorrent>>

    @Delete
    suspend fun delete(bookmarkedTorrent: BookmarkedTorrent)
}