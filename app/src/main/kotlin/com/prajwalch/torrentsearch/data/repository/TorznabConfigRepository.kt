package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.local.dao.TorznabConfigDao
import com.prajwalch.torrentsearch.data.local.entities.TorznabConfigEntity
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.TorznabConfig
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.map

import java.util.UUID
import javax.inject.Inject

class TorznabConfigRepository @Inject constructor(
    private val dao: TorznabConfigDao,
) {
    suspend fun createConfig(
        searchProviderName: String,
        url: String,
        apiKey: String,
        supportedCategories: Set<Category>,
    ) {
        val configId = UUID.randomUUID().toString()
        val configEntity = TorznabConfigEntity(
            id = configId,
            searchProviderName = searchProviderName,
            url = url.trimEnd { it == '/' },
            apiKey = apiKey,
            supportedCategories = supportedCategories,
        )
        dao.insertConfig(entity = configEntity)
    }

    fun getAllConfigs(): Flow<List<TorznabConfig>> {
        return dao.getAllConfigs().map { it.toDomain() }
    }

    suspend fun getCurrentConfigsByIds(ids: Set<String>): List<TorznabConfig> {
        return dao.getCurrentConfigsByIds(ids).map { it.toDomain() }
    }

    suspend fun getConfigIds(): List<String> {
        return dao.getConfigsId()
    }

    suspend fun findConfigById(id: String): TorznabConfig? {
        return dao.findConfigById(id)?.toDomain()
    }

    fun getConfigsCount(): Flow<Int> {
        return dao.getConfigsCount()
    }

    suspend fun updateConfig(
        id: String,
        searchProviderName: String,
        url: String,
        apiKey: String,
        supportedCategories: Set<Category>,
    ) {
        val configEntity = TorznabConfigEntity(
            id = id,
            searchProviderName = searchProviderName,
            url = url,
            apiKey = apiKey,
            supportedCategories = supportedCategories,
        )
        dao.updateConfig(entity = configEntity)
    }

    suspend fun deleteConfigById(id: String) {
        dao.deleteConfigById(id)
    }
}

private fun TorznabConfigEntity.toDomain() =
    TorznabConfig(
        id = this.id,
        searchProviderName = this.searchProviderName,
        url = this.url,
        apiKey = this.apiKey,
        supportedCategories = this.supportedCategories,
    )

private fun List<TorznabConfigEntity>.toDomain() = this.map { it.toDomain() }