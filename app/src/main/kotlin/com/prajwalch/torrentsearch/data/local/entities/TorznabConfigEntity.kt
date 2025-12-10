package com.prajwalch.torrentsearch.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.TorznabConfig
import com.prajwalch.torrentsearch.providers.SearchProviderInfo
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviderType

import java.util.UUID

@Entity(
    tableName = "torznab_configs",
    indices = [Index("id", unique = true)],
)
data class TorznabConfigEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "name")
    val searchProviderName: String,
    val url: String,
    val apiKey: String,
    val category: String,
)

fun TorznabConfigEntity.toDomain() =
    TorznabConfig(
        searchProviderName = this.searchProviderName,
        url = this.url,
        apiKey = this.apiKey,
        category = Category.valueOf(this.category),
    )

fun TorznabConfigEntity.toSearchProviderInfo() =
    SearchProviderInfo(
        id = this.id,
        name = this.searchProviderName,
        url = this.url,
        specializedCategory = Category.valueOf(this.category),
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
        type = SearchProviderType.Torznab,
    )

fun List<TorznabConfigEntity>.toSearchProviderInfo(): List<SearchProviderInfo> =
    this.map { it.toSearchProviderInfo() }