package com.prajwalch.torrentsearch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import com.prajwalch.torrentsearch.data.local.entities.TorznabConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TorznabConfigDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConfig(entity: TorznabConfigEntity)

    @Query("SELECT * FROM torznab_configs")
    fun getAllConfigs(): Flow<List<TorznabConfigEntity>>

    @Query("SELECT * FROM torznab_configs WHERE id IN (:ids)")
    suspend fun getCurrentConfigsByIds(ids: Set<String>): List<TorznabConfigEntity>

    @Query("SELECT id FROM torznab_configs")
    suspend fun getConfigsId(): List<String>

    @Query("SELECT COUNT(id) FROM torznab_configs")
    fun getConfigsCount(): Flow<Int>

    @Query("SELECT * FROM torznab_configs WHERE id=:id")
    suspend fun findConfigById(id: String): TorznabConfigEntity?

    @Update
    suspend fun updateConfig(entity: TorznabConfigEntity)

    @Query("DELETE FROM torznab_configs WHERE id=:id")
    suspend fun deleteConfigById(id: String)
}