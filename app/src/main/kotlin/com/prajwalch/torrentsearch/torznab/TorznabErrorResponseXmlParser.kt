package com.prajwalch.torrentsearch.torznab

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.parser.Parser

/**
 * An XML parser for the error response.
 */
object TorznabErrorResponseXmlParser {
    suspend fun parse(xml: String): Int = withContext(Dispatchers.Default) {
        Jsoup.parse(xml, Parser.xmlParser())
            .selectFirst("error")
            ?.attr("code")
            ?.toInt()
            ?: error("Error code not available")
    }
}