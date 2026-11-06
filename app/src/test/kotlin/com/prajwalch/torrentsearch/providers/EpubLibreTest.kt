package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

import io.mockk.coEvery
import io.mockk.mockk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubLibreTest {
    private val searchUrl = "https://epublibre.org/catalogo/index/0/nuevo/todos/sin/todos/quijote/ajax"
    private val detailsUrl = "https://epublibre.org/libro/detalle/4765"
    private val expectedMagnetUri =
        "magnet:?xt=urn:btih:2D361E98BF4AD38FF0C4F3B023CAAEEC096896A4" +
                "&dn=EPL_Don_Quijote_de_la_Mancha_(IV_CENTENARIO)" +
                "&tr=udp://tracker.opentrackr.org:1337/announce" +
                "&tr=udp://www.torrent.eu.org:451/announce" +
                "&tr=udp://exodus.desync.com:6969/announce" +
                "&tr=udp://p4p.arenabg.ch:1337/announce" +
                "&tr=udp://tracker.internetwarriors.net:1337/announce"

    private fun ajaxNetworkClient(url: String, json: String): NetworkClient {
        val networkClient = mockk<NetworkClient>()
        coEvery {
            networkClient.postJson(url, mapOf("X-Requested-With" to "XMLHttpRequest"))
        } returns Json.parseToJsonElement(json)
        return networkClient
    }

    @Test
    fun `search parses every book of the ajax catalogue html`() = runTest {
        val provider = EpubLibre(ajaxNetworkClient(searchUrl, fixture("epublibre/search.json")))

        val torrents = provider.search(query = "quijote", category = Category.Books)

        assertEquals(
            listOf(
                "Don Quijote de la Mancha (IV CENTENARIO) - Miguel de Cervantes Saavedra",
                "Viaje alrededor de El Quijote - Fernando del Paso",
                "Guía del lector del Quijote - Salvador de Madariaga",
            ),
            torrents.map { it.name },
        )
        val bookIds = listOf("4765", "25964", "85131")
        val expectedDetailsPageUrls = bookIds.map { "https://epublibre.org/libro/detalle/$it" }
        assertEquals(expectedDetailsPageUrls, torrents.map { it.detailsPageUrl })
        assertEquals(expectedDetailsPageUrls.map(MagnetUri::RequiresFetch), torrents.map { it.magnetUri })
        assertEquals(
            bookIds.map { TorrentUtils.createTorrentId(providerId = "epublibre", sourceId = it) },
            torrents.map { it.id },
        )
        assertTrue(torrents.all { it.category == Category.Books })
        assertTrue(torrents.all { it.providerName == "EpubLibre" })
        assertTrue(torrents.all { it.size == null && it.seeders == null && it.uploadDate == null })
    }

    @Test
    fun `search returns no results when the catalogue html has no books`() = runTest {
        val json = """{"contenido":"<div class=\"row\"><div class=\"span12 aviso\">No hay libros</div></div>"}"""
        val provider = EpubLibre(ajaxNetworkClient(searchUrl, json))

        assertEquals(emptyList<Any>(), provider.search(query = "quijote", category = Category.Books))
    }

    @Test
    fun `getDetails parses the book page`() = runTest {
        val provider = EpubLibre(fakeNetworkClient(detailsUrl to fixture("epublibre/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Don Quijote de la Mancha (IV CENTENARIO)", details.name)
        assertEquals(expectedMagnetUri, details.magnetUri)
        assertEquals("2d361e98bf4ad38ff0c4f3b023caaeec096896a4", details.infoHash)
        assertEquals(Category.Books, details.category)
        assertEquals("https://images.epublibre.org/libros/4765.jpg", details.posterUrl)
        // Serialised (pretty-printed) inner html of the first div.detalle, captured
        // from the pre-migration Jsoup implementation so output stays identical.
        assertEquals(fixture("epublibre/details_description.html"), details.description)
    }

    @Test
    fun `getDetails returns null when the page has no magnet link`() = runTest {
        val provider = EpubLibre(
            fakeNetworkClient(detailsUrl to "<html><body><div id=\"titulo_libro\">Libro</div></body></html>"),
        )

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri returns the magnet link of the book page`() = runTest {
        val provider = EpubLibre(fakeNetworkClient(detailsUrl to fixture("epublibre/details.html")))

        assertEquals(expectedMagnetUri, provider.getMagnetUri(detailsUrl))
    }
}
