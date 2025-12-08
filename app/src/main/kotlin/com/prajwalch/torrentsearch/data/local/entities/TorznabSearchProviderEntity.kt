package com.prajwalch.torrentsearch.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderInfo
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviderType
import com.prajwalch.torrentsearch.providers.TorznabSearchProviderConfig

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
)

fun TorznabSearchProviderEntity.toSearchProviderInfo() =
    SearchProviderInfo(
        id = this.id,
        name = this.name,
        url = this.url,
        specializedCategory = Category.valueOf(this.category),
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
        type = SearchProviderType.Torznab,
    )

fun TorznabSearchProviderEntity.toTorznabConfig() =
    TorznabSearchProviderConfig(
        id = this.id,
        name = this.name,
        url = this.url,
        apiKey = this.apiKey,
        category = Category.valueOf(this.category),
        enabledByDefault = false,
    )

fun TorznabSearchProviderConfig.toEntity() =
    TorznabSearchProviderEntity(
        name = this.name,
        url = this.url,
        apiKey = this.apiKey,
        category = this.category.name,
    )

fun List<TorznabSearchProviderEntity>.toSearchProviderInfo(): List<SearchProviderInfo> =
    this.map { it.toSearchProviderInfo() }