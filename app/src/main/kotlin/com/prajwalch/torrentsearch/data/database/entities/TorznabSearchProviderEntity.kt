package com.prajwalch.torrentsearch.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import java.util.UUID

@Entity(
    tableName = "torznab_search_providers",
    indices = [Index("id", unique = true)],
)
data class TorznabSearchProviderEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val apiKey: String,
    val category: String,
    // Null indicates safe status, otherwise it's unsafe.
    val unsafeReason: String?,
)