package com.prajwalch.torrentsearch.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.prajwalch.torrentsearch.data.local.entities.SearchHistoryEntity

import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: SearchHistoryEntity)

    @Query("SELECT * from search_history ORDER by id DESC")
    fun observeAll(): Flow<List<SearchHistoryEntity>>

    @Delete
    suspend fun delete(searchHistory: SearchHistoryEntity)

    @Query("DELETE from search_history")
    suspend fun deleteAll()
}