package com.prajwalch.torrentsearch.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.prajwalch.torrentsearch.data.database.entities.TorznabSearchProviderEntity

import kotlinx.coroutines.flow.Flow

@Dao
interface TorznabSearchProviderDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(searchProvider: TorznabSearchProviderEntity)

    @Query("SELECT * from torznab_search_providers")
    fun getAll(): Flow<List<TorznabSearchProviderEntity>>

    @Query("SELECT COUNT(id) from TORZNAB_SEARCH_PROVIDERS")
    fun getCount(): Flow<Int>

    @Delete
    suspend fun delete(searchProvider: TorznabSearchProviderEntity)
}