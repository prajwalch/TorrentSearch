package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture

import kotlinx.coroutines.test.runTest

import java.time.Instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TorrentKittyTest {
    private val searchUrl = "https://torrentkitty.tv/search/ubuntu"
    private val detailsUrl = "https://torrentkitty.tv/information/3F9AAC158C7DE8DFCAB171EA58A17AABDF7FBC93"

    @Test
    fun `search parses every result row and skips the header row`() = runTest {
        val provider = TorrentKitty(fakeNetworkClient(searchUrl to fixture("torrentkitty/search.html")))

        val torrents = provider.search(query = "ubuntu", category = Category.Other)

        assertEquals(
            listOf(
                "ubuntu-26.04-live-server-amd64.iso",
                "ubuntu-24.10-desktop-amd64.iso",
                "ubuntu-22.04.5-live-server-amd64.iso",
                "ubuntu-24.04.1-live-server-amd64.iso",
            ),
            torrents.map { it.name },
        )
        // Sizes are upper-cased by the parser.
        assertEquals(listOf("223.04 KB", "432.62 KB", "163.4 KB", "212 KB"), torrents.map { it.size })
        assertEquals(
            listOf("2026-07-20", "2025-01-07", "2024-11-23", "2024-09-08")
                .map { Instant.parse("${it}T00:00:00Z") },
            torrents.map { it.uploadDate },
        )
        assertTrue(torrents.all { it.seeders == null && it.peers == null && it.category == null })
        assertTrue(torrents.all { it.providerName == "TorrentKitty" })
        assertEquals(4, torrents.map { it.id }.toSet().size)

        val torrent = torrents[1]
        assertEquals(
            "https://torrentkitty.tv/information/3F9AAC158C7DE8DFCAB171EA58A17AABDF7FBC93",
            torrent.detailsPageUrl,
        )
        assertEquals("https://rebrand.ly/rc1285a", torrent.fileDownloadLink)
        val magnetUri = (torrent.magnetUri as MagnetUri.Available).value
        assertTrue(
            magnetUri,
            magnetUri.startsWith(
                "magnet:?xt=urn:btih:3F9AAC158C7DE8DFCAB171EA58A17AABDF7FBC93" +
                        "&dn=ubuntu-24.10-desktop-amd64.iso&tr=http%3A%2F%2Ftracker.ktxp.com%3A6868%2Fannounce",
            ),
        )
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = TorrentKitty(fakeNetworkClient(searchUrl to "<html><body><p>No results</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "ubuntu", category = Category.Other))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = TorrentKitty(fakeNetworkClient(detailsUrl to fixture("torrentkitty/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Details for torrent: ubuntu-24.10-desktop-amd64.iso", details.name)
        assertEquals("3f9aac158c7de8dfcab171ea58a17aabdf7fbc93", details.infoHash)
        assertEquals("5.28 GB", details.size)
        assertEquals(Instant.parse("2024-10-10T00:00:00Z"), details.uploadDate)
        assertEquals("https://rebrand.ly/rc1285a", details.fileDownloadLink)
        assertEquals(
            "magnet:?xt=urn:btih:3F9AAC158C7DE8DFCAB171EA58A17AABDF7FBC93&dn=ubuntu-24.10-desktop-amd64.iso" +
                    "&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80" +
                    "&tr=udp%3A%2F%2Ftracker.ccc.de%3A80&tr=https%3A%2F%2Ftorrent.ubuntu.com%2Fannounce",
            details.magnetUri,
        )
    }

    @Test
    fun `getDetails returns null when the page has no magnet link`() = runTest {
        val provider = TorrentKitty(
            fakeNetworkClient(detailsUrl to "<html><body><h2>Torrent not found</h2></body></html>"),
        )

        assertNull(provider.getDetails(detailsUrl))
    }
}
