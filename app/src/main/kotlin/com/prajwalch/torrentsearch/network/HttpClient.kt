package com.prajwalch.torrentsearch.network

import android.util.Log
import android.webkit.CookieManager
import androidx.core.net.toUri
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.http.parseServerSetCookieHeader
import io.ktor.http.renderSetCookieHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class CloudflareChallengeException(url: String) :
    Exception("Cloudflare challenge encountered [url=$url]")

/** Primary object for making network request. */
object HttpClient {
    private const val TAG = "TorrentSearchHttpClient"

    /** Maximum number of retries a client performs when a request fails. */
    private const val MAX_RETRIES = 3

    /**
     * Time period in which a client should process an HTTP call:
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

    /**
     * The default user-agent for WebView and every other request.
     */
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36"

    /** The underlying client. */
    private val innerClient by lazy { createClient() }

    /** Creates and configures the inner/underlying http client. */
    private fun createClient() = HttpClient(OkHttp) {
        install(UserAgent) { agent = USER_AGENT }
        install(HttpCookies) { storage = PersistentCookieStorage() }
        install(HttpCache)
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
        Log.d(TAG, "get $url")
        return getResponse(url, headers).bodyAsText()
    }

    suspend fun getResponse(url: String, headers: Map<String, String> = emptyMap()): HttpResponse {
        Log.d(TAG, "getResponse $url")

        val response = innerClient.get(urlString = url) {
            headers.forEach { (key, value) -> header(key, value) }
        }

        if (isResponseChallenged(response)) {
            throw CloudflareChallengeException(url)
        } else {
            return response
        }
    }

    /**
     * Makes a GET request and returns the response parsed as JSON or `null`
     * if parsing fails.
     */
    suspend fun getJson(url: String, headers: Map<String, String> = emptyMap()): JsonElement? {
        Log.d(TAG, "getJson $url")

        val content = getResponse(url, headers).bodyAsText()
        if (content.isEmpty()) {
            Log.d(TAG, "Received empty body")
            return null
        }

        Log.d(TAG, "Attempting to parse content as Json")
        return parseJson(content)
    }

    /**
     * Makes a POST request with the given JSON payload and returns the
     * response parsed as JSON or `null` if parsing fails or response is empty.
     */
    suspend fun postJson(url: String, payload: JsonElement): JsonElement? {
        Log.d(TAG, "postJson $url")

        val response = post(url = url, payload = payload)
        if (response.isEmpty()) {
            return null
        }

        return parseJson(response)
    }

    suspend fun submitForm(url: String, formData: Map<String, String>): String? {
        return innerClient.submitForm(
            url = url,
            formParameters = parameters {
                for ((key, value) in formData) append(key, value)
            },
        ).bodyAsText()
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
     * Parses and returns the given string as JSON or `null` if parsing fails.
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

    suspend fun isUrlChallenged(url: String): Boolean {
        return innerClient.get(url).let(::isResponseChallenged)
    }

    private fun isResponseChallenged(response: HttpResponse): Boolean {
        // cf-mitigated is a reliable way to check challenged page.
        // https://developers.cloudflare.com/cloudflare-challenges/challenge-types/challenge-pages/detect-response/
        return response.headers.contains("cf-mitigated", "challenge") ||
                response.status in setOf(
            HttpStatusCode.Forbidden,
            HttpStatusCode.ServiceUnavailable,
        )
    }

    fun getCookie(url: String): String? {
        return CookieManager.getInstance().getCookie(url)
    }

    fun removeCookie(url: String) {
        Log.i(TAG, "Removing cookie of $url")

        val cookieManager = CookieManager.getInstance()
        val cookies: String? = cookieManager.getCookie(url)

        if (cookies == null) {
            Log.i(TAG, "Cookie not found")
            return
        }

        for (cookie in cookies.split(";")) {
            val key = cookie.substringBefore("=").trim()
            Log.i(TAG, "Removing $key")

            cookieManager.setCookie(url, createExpiredCookie(key, url))
        }

        cookieManager.flush()
    }

    private fun createExpiredCookie(key: String, url: String): String {
        val domain = url.toUri().host!!.let { ".$it" }

        // NOTE: This works only for cf_clearance cookie, which is fine for us.
        return "$key=; Domain=$domain; Max-Age=0; Path=/; " +
                "SameSite=None; HttpOnly; Secure; Partitioned"
    }

    fun removeAllCookies() {
        Log.i(TAG, "Removing all cookies")
        CookieManager.getInstance().removeAllCookies { removed ->
            if (removed) {
                Log.i(TAG, "Cookies removed successfully")
            } else {
                Log.e(TAG, "Remove failed")
            }
        }
    }
}

private class PersistentCookieStorage : CookiesStorage {
    private val cookieManager = CookieManager.getInstance()

    override suspend fun get(requestUrl: Url): List<Cookie> {
        val cookies = cookieManager.getCookie(requestUrl.toString()) ?: return emptyList()
        return cookies.split(";").map(::parseServerSetCookieHeader)
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        cookieManager.setCookie(requestUrl.toString(), renderSetCookieHeader(cookie))
    }

    override fun close() {}
}

/** Represents either a success or failure response. */
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