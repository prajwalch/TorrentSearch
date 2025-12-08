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
    suspend fun insert(entity: TorznabConfigEntity)

    @Query("SELECT * from torznab_configs")
    fun observeAll(): Flow<List<TorznabConfigEntity>>

    @Query("SELECT * from torznab_configs where id=:id")
    suspend fun findById(id: String): TorznabConfigEntity?

    @Query("SELECT COUNT(id) from torznab_configs")
    fun observeCount(): Flow<Int>

    @Update
    suspend fun update(entity: TorznabConfigEntity)

    @Query("DELETE from torznab_configs where id=:id")
    suspend fun deleteById(id: String)
}