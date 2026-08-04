package com.prajwalch.torrentsearch.network

import android.util.Log
import android.webkit.CookieManager

import androidx.core.net.toUri
import com.prajwalch.torrentsearch.data.repository.SettingsRepository

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
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
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.http.parseServerSetCookieHeader
import io.ktor.http.renderSetCookieHeader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

import javax.inject.Inject
import javax.inject.Singleton

class CloudflareChallengeException(url: String) :
    Exception("Cloudflare challenge encountered [url=$url]")

@Singleton
class NetworkClient @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    companion object {
        private const val LOG_TAG = "NetworkClient"

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

        fun getCookie(url: String): String? {
            return CookieManager.getInstance().getCookie(url)
        }

        fun removeCookie(url: String) {
            Log.i(LOG_TAG, "Removing cookie of $url")

            val cookieManager = CookieManager.getInstance()
            val cookies: String? = cookieManager.getCookie(url)

            if (cookies == null) {
                Log.i(LOG_TAG, "Cookie not found")
                return
            }

            for (cookie in cookies.split(";")) {
                val key = cookie.substringBefore("=").trim()
                Log.i(LOG_TAG, "Removing $key")

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
            Log.i(LOG_TAG, "Removing all cookies")
            CookieManager.getInstance().removeAllCookies { removed ->
                if (removed) {
                    Log.i(LOG_TAG, "Cookies removed successfully")
                } else {
                    Log.e(LOG_TAG, "Remove failed")
                }
            }
        }
    }

    private val baseKtorClient = HttpClient(OkHttp) {
        engine { dns = DynamicDns(settingsRepository.dohProvider) }
        install(UserAgent) { agent = USER_AGENT }
        install(HttpCookies) { storage = PersistentCookieStorage() }
    }

    private val ktorClient = baseKtorClient.config {
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = MAX_RETRIES)
            retryOnException(
                maxRetries = MAX_RETRIES,
                retryOnTimeout = true
            )
            exponentialDelay()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
    }

    val coilKtorClient = baseKtorClient.config {
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            retryOnException(
                maxRetries = 2,
                retryOnTimeout = true
            )
            exponentialDelay()
        }
        install(HttpTimeout)
    }

    /**
     * Makes a GET request and returns the response parsed as JSON or `null`
     * if parsing fails.
     */
    suspend fun getJson(url: String, headers: Map<String, String> = emptyMap()): JsonElement? {
        Log.d(LOG_TAG, "getJson()")

        val rawText = getText(url, headers)
        if (rawText.isEmpty()) {
            Log.d(LOG_TAG, "Received empty body")
            return null
        }

        Log.d(LOG_TAG, "Attempting to parse content as Json")
        return parseJson(rawText)
    }

    /**
     * Makes a GET request and returns the response as raw text.
     */
    suspend fun getText(url: String, headers: Map<String, String> = emptyMap()): String {
        Log.d(LOG_TAG, "getText()")
        return get(url, headers).bodyAsText()
    }

    /**
     * Makes a GET request and returns the response.
     */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse {
        Log.d(LOG_TAG, "get() [url=$url, headers=$headers]")

        val response = ktorClient.get(urlString = url) {
            headers.forEach { (key, value) -> header(key, value) }
        }

        if (isResponseChallenged(response)) {
            Log.w(LOG_TAG, "Response is challenged by Cloudflare")
            throw CloudflareChallengeException(url)
        } else {
            return response
        }
    }

    /**
     * Makes a POST request with the given JSON payload and returns the
     * response parsed as JSON or `null` if parsing fails or response is empty.
     */
    suspend fun postJson(url: String, payload: JsonElement): JsonElement? {
        Log.d(LOG_TAG, "postJson() [url=$url, payload=$payload]")

        val payloadString = payload.toString()
        val response = ktorClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payloadString)
        }

        if (response.contentLength() == 0L) {
            return null
        }

        return parseJson(response.bodyAsText())
    }

    /**
     * Parses and returns the given string as JSON or `null` if parsing fails.
     */
    private suspend fun parseJson(jsonString: String) = withContext(Dispatchers.Default) {
        Log.d(LOG_TAG, "parseJson()")

        try {
            val json = Json.parseToJsonElement(jsonString)
            Log.d(LOG_TAG, "Parse succeed")
            json
        } catch (e: IllegalArgumentException) {
            Log.e(LOG_TAG, "Given string is not a valid Json", e)
            null
        }
    }

    /**
     * Makes a request containing form parameters encoded using the `x-www-form-urlencoded format`.
     */
    suspend fun submitForm(url: String, formData: Map<String, String>): String {
        val response = ktorClient.submitForm(
            url = url,
            formParameters = parameters {
                formData.forEach { (key, value) ->
                    append(key, value)
                }
            },
        )
        return response.bodyAsText()
    }

    suspend fun isUrlChallenged(url: String): Boolean {
        return ktorClient.get(url).let(::isResponseChallenged)
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