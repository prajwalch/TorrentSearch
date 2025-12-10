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

@Entity(
    tableName = "torznab_configs",
    indices = [Index("id", unique = true)],
)
data class TorznabConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val searchProviderId: String,

    @ColumnInfo(name = "name")
    val searchProviderName: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "apiKey")
    val apiKey: String,

    @ColumnInfo(name = "category")
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
        id = this.searchProviderId,
        name = this.searchProviderName,
        url = this.url,
        specializedCategory = Category.valueOf(this.category),
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
        type = SearchProviderType.Torznab,
    )

fun List<TorznabConfigEntity>.toSearchProviderInfo(): List<SearchProviderInfo> =
    this.map { it.toSearchProviderInfo() }