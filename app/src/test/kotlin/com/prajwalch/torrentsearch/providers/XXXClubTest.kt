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

/**
 * Fixtures are hand-written with neutral placeholder content; they mirror the
 * markup structure the selectors target.
 */
class XXXClubTest {
    private val searchUrl = "https://xxxclub.to/torrents/search/all/sample"
    private val detailsUrl = "https://xxxclub.to/torrents/details/6143820/Sample-Title-One-1080p"
    private val magnetUri = "magnet:?xt=urn:btih:0A1B2C3D4E5F60718293A4B5C6D7E8F901234567" +
            "&dn=Sample+Title+One+1080p&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce"

    @Test
    fun `search parses torrent rows and skips header and foreign links`() = runTest {
        val provider = XXXClub(fakeNetworkClient(searchUrl to fixture("xxxclub/search.html")))

        val torrents = provider.search(query = "sample", category = Category.All)

        assertEquals(listOf("Sample Title One 1080p", "Placeholder Clip Two & Friends"), torrents.map { it.name })
        assertEquals(listOf("1.21 GB", "356.4 MB"), torrents.map { it.size })
        assertEquals(listOf(57u, 0u), torrents.map { it.seeders })
        assertEquals(listOf(8u, 12u), torrents.map { it.peers })
        assertEquals(
            listOf(Instant.parse("2026-10-05T14:22:10Z"), Instant.parse("2026-10-04T03:05:59Z")),
            torrents.map { it.uploadDate },
        )
        val detailsPageUrls = listOf(
            detailsUrl,
            "https://xxxclub.to/torrents/details/6143811/Placeholder-Clip-Two-Tom-and-Jerry",
        )
        assertEquals(detailsPageUrls, torrents.map { it.detailsPageUrl })
        assertEquals(detailsPageUrls.map(MagnetUri::RequiresFetch), torrents.map { it.magnetUri })
        assertEquals(listOf(Category.Porn, Category.Porn), torrents.map { it.category })
    }

    @Test
    fun `search returns no results for a page without the results list`() = runTest {
        val provider = XXXClub(fakeNetworkClient(searchUrl to "<html><body><ul><li>x</li></ul></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "sample", category = Category.All))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = XXXClub(fakeNetworkClient(detailsUrl to fixture("xxxclub/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Sample Title One 1080p", details.name)
        assertEquals(magnetUri, details.magnetUri)
        assertEquals("0a1b2c3d4e5f60718293a4b5c6d7e8f901234567", details.infoHash)
        assertEquals("1.21 GB", details.size)
        assertEquals(57u, details.seeders)
        assertEquals(8u, details.peers)
        assertEquals(Instant.parse("2026-10-05T14:22:10Z"), details.uploadDate)
        assertEquals(Instant.parse("2026-10-10T09:41:00Z"), details.lastChecked)
        assertEquals("placeholder_uploader", details.uploader)
        assertEquals("https://xxxclub.to/download/6143820/Sample-Title-One-1080p.torrent", details.fileDownloadLink)
        assertEquals("https://img.example.com/posters/6143820.jpg", details.posterUrl)
        assertEquals(Category.Porn, details.category)
        assertEquals(
            "First line of the description.<br>Second line &amp; more.",
            details.description?.replace("\n", ""),
        )
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = XXXClub(fakeNetworkClient(detailsUrl to "<html><body><h1>Not found</h1></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri extracts the decoded magnet link`() = runTest {
        val provider = XXXClub(fakeNetworkClient(detailsUrl to fixture("xxxclub/details.html")))

        assertEquals(magnetUri, provider.getMagnetUri(detailsUrl))
    }

    @Test
    fun `getMagnetUri fails when the page has no magnet link`() = runTest {
        val provider = XXXClub(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        val result = runCatching { provider.getMagnetUri(detailsUrl) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
