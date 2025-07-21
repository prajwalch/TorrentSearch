package com.prajwalch.torrentsearch.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.prajwalch.torrentsearch.database.entities.BookmarkedTorrent
import com.prajwalch.torrentsearch.database.entities.SearchHistory

import kotlinx.coroutines.flow.Flow

@Dao
interface DatabaseDao {
    // Search history operations.
    @Query("SELECT * from search_history ORDER by id DESC")
    fun searchHistories(): Flow<List<SearchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: SearchHistory)

    @Delete
    suspend fun delete(searchHistory: SearchHistory)

    @Query("DELETE from search_history")
    suspend fun clearSearchHistory()

    // Bookmark/favourite operations.
    @Query("SELECT * FROM bookmarks ORDER by id DESC")
    fun bookmarkedTorrents(): Flow<List<BookmarkedTorrent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmarkedTorrent: BookmarkedTorrent)

    @Delete
    suspend fun delete(bookmarkedTorrent: BookmarkedTorrent)
}