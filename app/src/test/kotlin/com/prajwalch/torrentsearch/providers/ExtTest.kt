package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot

import kotlinx.coroutines.test.runTest

import java.security.MessageDigest
import java.time.Instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The search fixture is a real capture; the details page is behind a
 * Cloudflare challenge, so its fixture is hand-written from the selectors.
 */
class ExtTest {
    private val searchUrl = "https://ext.to/browse/?cat=1&q=2026"
    private val detailsUrl = "https://ext.to/mutiny-2026-1080p-webrip-x264-5-1-yts-yify-21403167/"
    private val magnetFormUrl = "https://ext.to/ajax/getTorrentMagnet.php"
    private val magnetUri = "magnet:?xt=urn:btih:9F3C1B2A4D5E6F708192A3B4C5D6E7F801234567&dn=Mutiny"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = Ext(fakeNetworkClient(searchUrl to fixture("ext/search.html")))

        val torrents = provider.search(query = "2026", category = Category.Movies)

        assertEquals(
            listOf(
                "Obsession.2026.1080p.AMZN.WEB-DL.DDP5.1.H264.MP4-BTM",
                "Mutiny (2026) 1080p WEBRip x264 5.1 YTS YIFY",
                "Minions & Monsters (2026) 1080p WEBRip x264 5.1 YTS YIFY",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("4.96 GB", "1.75 GB", "1.71 GB"), torrents.map { it.size })
        assertEquals(listOf(5972u, 4864u, 1818u), torrents.map { it.seeders })
        assertEquals(listOf(3686u, 774u, 242u), torrents.map { it.peers })
        assertEquals(
            listOf("2026-06-29T00:00:00Z", "2026-08-19T00:00:00Z", "2026-08-11T00:00:00Z").map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        assertTrue(torrents.all { it.category == Category.Movies })
        val detailsPageUrls = listOf(
            "https://ext.to/obsession-2026-1080p-amzn-web-dl-ddp5-1-h264-mp4-btm-20625912/",
            detailsUrl,
            "https://ext.to/minions-monsters-2026-1080p-webrip-x264-5-1-yts-yify-21232725/",
        )
        assertEquals(detailsPageUrls, torrents.map { it.detailsPageUrl })
        assertEquals(detailsPageUrls.map(MagnetUri::RequiresFetch), torrents.map { it.magnetUri })
        assertEquals(3, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = Ext(fakeNetworkClient(searchUrl to "<html><body><table></table></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "2026", category = Category.Movies))
    }

    @Test
    fun `getDetails parses the page and requests the magnet with tokens from the page`() = runTest {
        val networkClient = fakeNetworkClient(detailsUrl to fixture("ext/details.html"))
        val formData = slot<Map<String, String>>()
        coEvery { networkClient.submitForm(magnetFormUrl, capture(formData)) } returns """{"url":"$magnetUri"}"""
        val provider = Ext(networkClient)

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Mutiny (2026) 1080p WEBRip x264 5.1 YTS YIFY", details.name)
        assertEquals(magnetUri, details.magnetUri)
        assertEquals("9f3c1b2a4d5e6f708192a3b4c5d6e7f801234567", details.infoHash)
        assertEquals("1.75 GB", details.size)
        assertEquals(4864u, details.seeders)
        assertEquals(774u, details.peers)
        assertEquals(Instant.parse("2026-08-19T00:00:00Z"), details.uploadDate)
        assertEquals(Category.Movies, details.category)
        assertEquals("YTS", details.uploader)
        assertTrue(details.lastChecked!! < Instant.now())
        assertEquals(
            "A disgraced naval officer must retake his ship after the crew turns against him.",
            details.description,
        )
        assertEquals("https://ext.to/static/img/posters/21403167.jpg", details.posterUrl)

        assertMagnetFormUsesPageTokens(formData.captured)
    }

    @Test
    fun `getDetails returns null without requesting a magnet when the session token is missing`() = runTest {
        val html = fixture("ext/details.html").replace("""<meta name="csrf-token" content="a81f0c9e2b7d4c55" />""", "")
        val networkClient = fakeNetworkClient(detailsUrl to html)
        val provider = Ext(networkClient)

        assertNull(provider.getDetails(detailsUrl))
        coVerify(exactly = 0) { networkClient.submitForm(any(), any()) }
    }

    @Test
    fun `getMagnetUri requests the magnet with tokens from the page`() = runTest {
        val networkClient = fakeNetworkClient(detailsUrl to fixture("ext/details.html"))
        val formData = slot<Map<String, String>>()
        coEvery { networkClient.submitForm(magnetFormUrl, capture(formData)) } returns """{"url":"$magnetUri"}"""
        val provider = Ext(networkClient)

        assertEquals(magnetUri, provider.getMagnetUri(detailsUrl))
        assertMagnetFormUsesPageTokens(formData.captured)
    }

    /** Torrent ID, session ID and the `<script>` page token must all come from the details page. */
    private fun assertMagnetFormUsesPageTokens(formData: Map<String, String>) {
        assertEquals("21403167", formData["torrent_id"])
        assertEquals("a81f0c9e2b7d4c55", formData["sessid"])
        assertEquals("magnet", formData["download_type"])

        val timestamp = formData.getValue("timestamp")
        val expectedHmac = MessageDigest.getInstance("SHA-256")
            .digest("21403167|$timestamp|5d0f8e3b9a7c41e2b6f0d9a8c7e6b5a4".toByteArray())
            .toHexString()
        assertEquals(expectedHmac, formData["hmac"])
    }
}
