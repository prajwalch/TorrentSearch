package com.prajwalch.torrentsearch.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object HttpClient {
    private val innerClient by lazy {
        HttpClient(CIO) {
            install(HttpRequestRetry) {
                retryOnExceptionIf(maxRetries = 3) { request, cause ->
                    cause is HttpRequestTimeoutException
                            || cause is ConnectTimeoutException
                            || cause is SocketTimeoutException
                }
            }
        }
    }

    fun close() {
        innerClient.close()
    }

    /**
     * Runs the `block` and gracefully handles the http/network related exceptions
     * if the `block` throws, and returns the error friendly structure with the
     * result returned by the `block` in it.
     */
    suspend fun <T> withExceptionHandler(block: suspend () -> T): HttpClientResponse<T> {
        return try {
            val result = block()
            HttpClientResponse.Ok(result = result)
        } catch (_: HttpRequestTimeoutException) {
            HttpClientResponse.Error.HttpRequestTimeoutError
        } catch (_: ConnectTimeoutException) {
            HttpClientResponse.Error.ConnectTimeoutError
        } catch (_: SocketTimeoutException) {
            HttpClientResponse.Error.SocketTimeoutError
        } catch (_: ResponseException) {
            HttpClientResponse.Error.ResponseError
        } catch (_: IOException) {
            HttpClientResponse.Error.NetworkError
        } catch (e: Exception) {
            HttpClientResponse.Error.OtherError(source = e)
        }
    }

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String {
        return innerClient.get(urlString = url) {
            for ((key, value) in headers) header(key = key, value = value)
            timeout {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
        }.bodyAsText()
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

/** Represents a either success or failure response. */
sealed class HttpClientResponse<out T> {
    /* Represents a successful response. */
    data class Ok<T>(val result: T) : HttpClientResponse<T>()

    /** Represents a failure response. */
    sealed class Error : HttpClientResponse<Nothing>() {
        /** Network is not available. */
        object NetworkError : Error()

        /** Timeout during requesting and receiving response. */
        object HttpRequestTimeoutError : Error()

        /** Can't establish TCP connection. */
        object ConnectTimeoutError : Error()

        /** Timeout during data transfer. */
        object SocketTimeoutError : Error()

        /** 3xx, 4xx and 5xx responses. */
        object ResponseError : Error()

        /** Unhandled or unknown errors. */
        data class OtherError(val source: Exception) : Error()
    }
}