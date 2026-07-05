package com.prajwalch.torrentsearch.torznab

import android.util.Log
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.TorznabConnectionCheckResult
import com.prajwalch.torrentsearch.network.HttpClient
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParserException
import java.net.ConnectException
import java.net.UnknownHostException

object TorznabUtils {
    private const val LOG_TAG = "TorznabUtils"
    private const val HTTP_STATUS_OK = 200
    private const val HTTP_STATUS_NOT_AUTHORIZED = 401
    private const val XML_DECLARATION = """<?xml version="1.0" encoding="UTF-8"?>"""

    fun normalizeApiUrl(url: String) =
        url.trimEnd('/').let { if (it.endsWith("api")) it else "${it}/api" }

    suspend fun getSupportedCategories(apiUrl: String, apiKey: String): Set<Category>? {
        Log.d(LOG_TAG, "Fetching capabilities from $apiUrl")

        val normalizedApiUrl = normalizeApiUrl(apiUrl)
        val requestUrl = "$normalizedApiUrl?t=${TorznabFunctions.CAPS}&apikey=$apiKey"
        val capabilitiesResponseXml = HttpClient.get(requestUrl)
        Log.d(LOG_TAG, "Capabilities fetch succeed")

        return withContext(Dispatchers.Default) {
            val capabilitiesXmlParser = TorznabCapabilitiesXmlParser()

            try {
                Log.d(LOG_TAG, "Attempting to parse capabilities")

                val capabilities = capabilitiesXmlParser.parse(xml = capabilitiesResponseXml)
                Log.d(LOG_TAG, "Capabilities parse succeed")

                capabilities.supportedCategories
            } catch (e: XmlPullParserException) {
                Log.e(LOG_TAG, "Capabilities parse failed", e)
                null
            }
        }
    }

    suspend fun checkConnection(apiUrl: String, apiKey: String): TorznabConnectionCheckResult =
        withContext(Dispatchers.IO) {
            Log.i(LOG_TAG, "Checking connection")

            val normalizedApiUrl = normalizeApiUrl(apiUrl)
            val requestUrl = "$normalizedApiUrl?t=${TorznabFunctions.CAPS}&apikey=$apiKey"

            val response = try {
                Log.d(LOG_TAG, "Attempting to fetch capabilities")
                HttpClient.getResponse(url = requestUrl)
            } catch (e: UnknownHostException) {
                Log.e(LOG_TAG, "Failed to resolve host IP address", e)
                return@withContext TorznabConnectionCheckResult.ConnectionFailed
            } catch (e: ConnectException) {
                Log.e(LOG_TAG, "Failed to establish a connection to host", e)
                return@withContext TorznabConnectionCheckResult.ConnectionFailed
            }

            Log.d(LOG_TAG, "Capabilities fetch succeed")

            val responseStatusCode = response.status.value

            // Some client returns 401 instead of returning 200 with error page
            // when invalid API key is given.
            //
            // For example: Prowlarr returns 401 but Jackett return 200
            // with proper error response with code following the spec.
            if (responseStatusCode == HTTP_STATUS_NOT_AUTHORIZED) {
                return@withContext TorznabConnectionCheckResult.InvalidApiKey
            }

            if (responseStatusCode != HTTP_STATUS_OK) {
                Log.d(LOG_TAG, "Received unexpected HTTP status code $responseStatusCode")
                return@withContext TorznabConnectionCheckResult.UnexpectedError
            }

            val responseXml = response.bodyAsText()
            val responseXmlWoDeclaration = responseXml.removePrefix(XML_DECLARATION).trimStart()

            if (responseXmlWoDeclaration.startsWith("<caps>")) {
                return@withContext TorznabConnectionCheckResult.ConnectionEstablished
            }

            if (!responseXmlWoDeclaration.startsWith("<error code=")) {
                val startTag = responseXmlWoDeclaration.takeWhile { it != '>' }
                Log.d(LOG_TAG, "Response starts with unexpected tag $startTag")

                return@withContext TorznabConnectionCheckResult.UnexpectedError
            }

            val errorResponseXmlParser = TorznabErrorResponseXmlParser()
            when (val errorCode = errorResponseXmlParser.parse(xml = responseXml)) {
                in 100..199 -> TorznabConnectionCheckResult.InvalidApiKey
                in 200..299 -> TorznabConnectionCheckResult.ApplicationError(errorCode)
                else -> TorznabConnectionCheckResult.UnexpectedResponse(errorCode)
            }
        }
}