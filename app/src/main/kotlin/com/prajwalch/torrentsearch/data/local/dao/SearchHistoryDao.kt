package com.prajwalch.torrentsearch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.prajwalch.torrentsearch.data.local.entities.SearchHistoryEntity

import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(searchHistory: SearchHistoryEntity)

    @Query("SELECT * from search_history ORDER by id DESC")
    fun getAllSearchHistories(): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE `query` LIKE '%' || :term || '%' ORDER BY id DESC")
    fun getSearchHistoriesByTerm(term: String): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history ORDER BY last_searched_at DESC LIMIT 5")
    fun getRecentSearchHistories(): Flow<List<SearchHistoryEntity>>

    @Query("DElETE from search_history where id=:id")
    suspend fun deleteSearchHistoryById(id: Long)

    @Query("DELETE from search_history")
    suspend fun deleteAllSearchHistories()
}