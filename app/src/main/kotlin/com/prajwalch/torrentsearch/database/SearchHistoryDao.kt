package com.prajwalch.torrentsearch.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prajwalch.torrentsearch.database.entities.SearchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: SearchHistory)

    @Query("SELECT * from search_history ORDER by id DESC")
    fun getAll(): Flow<List<SearchHistory>>

    @Delete
    suspend fun delete(searchHistory: SearchHistory)

    @Query("DELETE from search_history")
    suspend fun clearAll()
}