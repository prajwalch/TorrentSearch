package com.prajwalch.torrentsearch.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "torznab_search_providers",
    indices = [Index("id", unique = true)],
)
data class TorznabSearchProviderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val apiKey: String,
)