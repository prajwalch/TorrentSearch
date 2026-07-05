package com.prajwalch.torrentsearch.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import com.prajwalch.torrentsearch.domain.model.Category

@Entity(
    tableName = "torznab_configs",
    indices = [Index("id", unique = true)],
)
data class TorznabConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val searchProviderName: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "apiKey")
    val apiKey: String,

    @ColumnInfo(name = "supported_categories")
    val supportedCategories: Set<Category>,
)