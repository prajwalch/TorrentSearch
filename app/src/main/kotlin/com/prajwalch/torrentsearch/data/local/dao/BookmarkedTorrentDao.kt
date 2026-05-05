package com.prajwalch.torrentsearch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.prajwalch.torrentsearch.data.local.entities.BookmarkedTorrentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkedTorrentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmarkedTorrent: BookmarkedTorrentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmarks(bookmarkedTorrents: List<BookmarkedTorrentEntity>)

    @Query("SELECT * FROM bookmarks ORDER by id DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedTorrentEntity>>

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE from bookmarks")
    suspend fun deleteAllBookmarks()
}