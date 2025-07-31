package com.prajwalch.torrentsearch.network

import android.util.Log

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** Primary object for making network request. */
object HttpClient {
    private const val TAG = "TorrentSearchHttpClient"
    private val innerClient by lazy { createClient() }

    /** Creates and configures the inner/underlying http client. */
    private fun createClient() = HttpClient(OkHttp) {
        install(HttpRequestRetry) {
            retryOnExceptionIf(maxRetries = 3) { _, cause ->
                when (cause) {
                    is HttpRequestTimeoutException -> true
                    is ConnectTimeoutException -> true
                    is SocketTimeoutException -> true
                    else -> false
                }
            }
        }
    }

    /** Completely closes the connection. */
    fun close() {
        Log.i(TAG, "Closing the connection")
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

    /**
     * Makes a GET request and returns the response as is.
     *
     * This function throws an exception therefore it is recommended to wrap
     * the calling function inside the [HttpClient.withExceptionHandler], as shown in the
     * code below.
     *
     * ```kotlin
     * suspend fun makeRequest(): SomeCustomData {
     *     val res = httpClient.get(...)
     *     // may parsing?
     *     return ...
     * }
     *
     * suspend fun fetchAllData(): List<SomeCustomData> {
     *     for (i in 1..5) {
     *         val response = httpClient.withExceptionHandler { makeRequest() }
     *         // Handle response gracefully.
     *     }
     *     ...
     * }
     * ```
     *
     * Note: The optional headers are currently being used by Eztv provider only.
     *       See its source code to understand why.
     */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String {
        Log.i(TAG, "Making a request to $url (headers=$headers)")
        return innerClient.get(urlString = url) {
            for ((key, value) in headers) header(key = key, value = value)
            timeout {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
        }.bodyAsText()
    }

    /**
     * Makes a GET request, parses the response body as Json and returns it,
     * if no any parsing error occurs, otherwise returns `null`.
     */
    suspend fun getJson(url: String): JsonElement? {
        val response = get(url)

        if (response.isEmpty()) {
            return null
        }

        Log.i(TAG, "Received a maybe json?: $response")
        Log.i(TAG, "Parsing it...")
        return parseJson(response)
    }

    /**
     * Makes a POST request with the given JSON payload and returns the response as raw text.
     */
    private suspend fun post(
        url: String,
        payload: JsonElement,
        headers: Map<String, String> = emptyMap(),
    ): String {
        val payloadString = payload.toString()
        Log.i(TAG, "Making POST request to $url with payload: $payloadString")
        return innerClient.post(url) {
            headers.forEach { (key, value) ->
                header(key, value)
            }
            timeout {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
            contentType(ContentType.Application.Json)
            setBody(payloadString)
        }.bodyAsText()
    }

    /**
     * Makes a POST request with the given JSON payload and returns the response parsed as Json.
     * Returns null if parsing fails or response is empty.
     */
    suspend fun postJson(
        url: String,
        payload: JsonElement,
        headers: Map<String, String> = emptyMap(),
    ): JsonElement? {
        val response = post(url = url, payload = payload, headers = headers)
        if (response.isEmpty()) {
            return null
        }
        Log.i(TAG, "Parsing POST response as JSON: $response")
        return parseJson(response)
    }

    /**
     * Parses teh given string as json and returns it, if parsing error
     * doesn't occurs, otherwise returns `null`.
     */
    private suspend fun parseJson(string: String) = withContext(Dispatchers.Default) {
        try {
            val json = Json.parseToJsonElement(string)
            Log.i(TAG, "Json parsed successfully")
            json
        } catch (e: SerializationException) {
            Log.e(TAG, "Json parsing failed, ${e.message}")
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