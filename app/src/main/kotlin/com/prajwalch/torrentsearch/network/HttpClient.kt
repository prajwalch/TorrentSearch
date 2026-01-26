package com.prajwalch.torrentsearch.network

import android.util.Log

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** Primary object for making network request. */
object HttpClient {
    private const val TAG = "TorrentSearchHttpClient"

    /** Maximum number of retries a client performs when a request fails. */
    private const val MAX_RETRIES = 3

    /**
     * Time period in which a client should process a HTTP call:
     * from sending a request to receiving a response.
     */
    private const val REQUEST_TIMEOUT_MS = 20_000L

    /**
     * Time period in which a client should establish a connection with a
     * server.
     */
    private const val CONNECT_TIMEOUT_MS = 10_000L

    /**
     * Maximum time of inactivity between two data packets when exchanging
     * data with a server.
     */
    private const val SOCKET_TIMEOUT_MS = 15_000L

    /** The underlying client. */
    private val innerClient by lazy { createClient() }

    /** Creates and configures the inner/underlying http client. */
    private fun createClient() = HttpClient(OkHttp) {
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = MAX_RETRIES)
            retryOnException(maxRetries = MAX_RETRIES, retryOnTimeout = true)
            exponentialDelay()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
        install(HttpCache)
    }

    /** Completely closes the connection. */
    fun close() {
        Log.d(TAG, "close")
        innerClient.close()
    }

    /**
     * Runs the `block` and gracefully handles the http/network related exceptions
     * if the `block` throws, and returns the error friendly structure with the
     * result returned by the `block` in it.
     */
    @Deprecated(message = "This function is deprecated, use manual try/catch block.")
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

    /**
     * Makes a GET request and returns the response as raw text.
     */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String {
        Log.d(TAG, "get")

        return innerClient.get(urlString = url) {
            headers.forEach { (key, value) -> header(key = key, value = value) }
        }.bodyAsText()
    }

    suspend fun getResponse(url: String, headers: Map<String, String> = emptyMap()): HttpResponse {
        Log.d(TAG, "getResponse")

        return innerClient.get(urlString = url) {
            headers.forEach { (key, value) -> header(key = key, value = value) }
        }
    }

    /**
     * Makes a GET request and returns the response parsed as Json or `null`
     * if parsing fails.
     */
    suspend fun getJson(url: String): JsonElement? {
        Log.d(TAG, "getJson")

        val response = getResponse(url)
        if (response.contentType() != ContentType.Application.Json) {
            Log.d(TAG, "Received non-Json content")
            return null
        }

        val content = response.bodyAsText()
        if (content.isEmpty()) {
            Log.d(TAG, "Received empty body")
            return null
        }

        Log.d(TAG, "Attempting to parse content as Json")
        return parseJson(content)
    }

    /**
     * Makes a POST request with the given Json payload and returns the
     * response parsed as Json or `null` if parsing fails or response is empty.
     */
    suspend fun postJson(url: String, payload: JsonElement): JsonElement? {
        Log.d(TAG, "postJson")

        val response = post(url = url, payload = payload)
        if (response.isEmpty()) {
            return null
        }

        return parseJson(response)
    }

    /**
     * Makes a POST request with the given JSON payload and returns the response
     * as raw text.
     */
    private suspend fun post(url: String, payload: JsonElement): String {
        Log.d(TAG, "post")

        val payloadString = payload.toString()
        return innerClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payloadString)
        }.bodyAsText()
    }

    /**
     * Parses and returns the given string as Json or `null` if parsing fails.
     */
    private suspend fun parseJson(jsonString: String) = withContext(Dispatchers.Default) {
        Log.d(TAG, "parseJson")

        try {
            val json = Json.parseToJsonElement(jsonString)
            Log.d(TAG, "Parse succeed")
            json
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Given string is not a valid Json", e)
            null
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