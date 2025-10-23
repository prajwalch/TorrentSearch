package com.prajwalch.torrentsearch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import com.prajwalch.torrentsearch.data.local.entities.TorznabSearchProviderEntity

import kotlinx.coroutines.flow.Flow

@Dao
interface TorznabSearchProviderDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(searchProvider: TorznabSearchProviderEntity)

    @Query("SELECT * from torznab_search_providers")
    fun observeAll(): Flow<List<TorznabSearchProviderEntity>>

    @Query("SELECT * from torznab_search_providers where id=:id")
    suspend fun findById(id: String): TorznabSearchProviderEntity?

    @Query("SELECT COUNT(id) from TORZNAB_SEARCH_PROVIDERS")
    fun observeCount(): Flow<Int>

    @Update
    suspend fun update(searchProvider: TorznabSearchProviderEntity)

    @Query("DELETE from torznab_search_providers where id=:id")
    suspend fun deleteById(id: String)
}