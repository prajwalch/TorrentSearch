package com.prajwalch.torrentsearch

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class HttpClient {
    private val innerClient = HttpClient(CIO)

    suspend fun getJson(url: String): JsonElement {
        val response = innerClient.get(url).bodyAsText()
        println("url=$url, response=$response")
        return Json.parseToJsonElement(response)
    }
}
