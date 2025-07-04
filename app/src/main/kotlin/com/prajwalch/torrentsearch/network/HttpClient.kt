package com.prajwalch.torrentsearch.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object HttpClient {
    private val innerClient by lazy {
        HttpClient(CIO)
    }

    fun close() {
        innerClient.close()
    }

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): String {
        return innerClient
            .get(urlString = url) {
                for ((key, value) in headers) header(key = key, value = value)
            }
            .bodyAsText()
    }

    suspend fun getJson(url: String): JsonElement? {
        val response = get(url)

        if (response.isEmpty()) {
            return null
        }

        return try {
            Json.parseToJsonElement(response)
        } catch (_: SerializationException) {
            null
        }
    }

    suspend fun isInternetAvailable(): Boolean {
        return try {
            val response = innerClient.get("https://clients3.google.com/generate_204")
            response.status.value == 204
        } catch (_: Exception) {
            false
        }
    }
}