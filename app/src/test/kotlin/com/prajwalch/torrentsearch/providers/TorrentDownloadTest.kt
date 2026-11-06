package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture

import kotlinx.coroutines.test.runTest

import java.time.Duration
import java.time.Instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TorrentDownloadTest {
    private val searchUrl = "https://torrentdownload.info/search?q=ubuntu"
    private val detailsUrl =
        "https://torrentdownload.info/A1425E0D6630336CDD9FB320F3FFF1030098975A/Ubuntu-10-04-LTS-x64"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = TorrentDownload(fakeNetworkClient(searchUrl to fixture("torrentdownload/search.html")))

        val torrents = provider.search(query = "ubuntu", category = Category.All)

        // The "Fast Links" ad rows have no category span and are skipped.
        assertEquals(
            listOf(
                "Ubuntu 10 04 LTS x64",
                "ubuntu 16 04 2 desktop amd64 iso",
                "Ubuntu 12 10 Desktop",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("697.57 MB", "1.45 GB", "445.31 MB"), torrents.map { it.size })
        // Thousands separators are stripped.
        assertEquals(listOf(4341u, 866u, 665u), torrents.map { it.seeders })
        assertEquals(listOf(2956u, 204u, 26u), torrents.map { it.peers })
        // " » Applications" etc.
        assertEquals(
            listOf(Category.Apps, Category.Other, Category.Movies),
            torrents.map { it.category },
        )
        assertEquals(
            listOf(
                "https://torrentdownload.info/A1425E0D6630336CDD9FB320F3FFF1030098975A/Ubuntu-10-04-LTS-x64",
                "https://torrentdownload.info/DA775E4AAF5635EF72583A391977E5ED6F14617E/ubuntu-16-04-2-desktop-amd64-iso",
                "https://torrentdownload.info/E2C8E1C1CD9C6B67CCB6B70C20BB88F6EAB3F694/Ubuntu-12-10-Desktop",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(
                "a1425e0d6630336cdd9fb320f3fff1030098975a",
                "da775e4aaf5635ef72583a391977e5ed6f14617e",
                "e2c8e1c1cd9c6b67ccb6b70c20bb88f6eab3f694",
            ),
            torrents.map {
                (it.magnetUri as MagnetUri.Available).value
                    .removePrefix("magnet:?xt=urn:btih:")
                    .substringBefore("&tr=")
            },
        )
        // "1 Year+" is treated as one year ago.
        val now = Instant.now()
        torrents.forEach { torrent ->
            val uploadDate = torrent.uploadDate!!
            assertTrue(
                uploadDate.toString(),
                uploadDate in now.minus(Duration.ofDays(366))..now.minus(Duration.ofDays(364)),
            )
        }
        assertTrue(torrents.all { it.providerName == "TorrentDownload" })
        assertEquals(3, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for the no results page`() = runTest {
        val searchUrl = "https://torrentdownload.info/search?q=qzxjvwqkplm"
        val provider = TorrentDownload(fakeNetworkClient(searchUrl to fixture("torrentdownload/no-results.html")))

        assertEquals(emptyList<Any>(), provider.search(query = "qzxjvwqkplm", category = Category.All))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = TorrentDownload(fakeNetworkClient(detailsUrl to fixture("torrentdownload/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Ubuntu 10 04 LTS x64", details.name)
        assertEquals("a1425e0d6630336cdd9fb320f3fff1030098975a", details.infoHash)
        assertTrue(
            details.magnetUri,
            details.magnetUri.startsWith(
                "magnet:?xt=urn:btih:A1425E0D6630336CDD9FB320F3FFF1030098975A&dn=" +
                        "&tr=udp%3A%2F%2Ftracker.torrent.eu.org%3A451%2Fannounce" +
                        "&tr=http%3A%2F%2Ftracker.ipv6tracker.ru%3A80%2Fannounce",
            ),
        )
        assertTrue(
            details.magnetUri,
            details.magnetUri.endsWith("&tr=http%3A%2F%2Ftracker.moxing.party%3A6969%2Fannounce"),
        )
        assertEquals(1112, details.magnetUri.length)
        assertEquals("697.57 MB", details.size)
        assertEquals(4341u, details.seeders)
        assertEquals(2956u, details.peers)
        assertEquals(Instant.parse("2011-03-31T00:00:00Z"), details.uploadDate)
        assertEquals(Category.Apps, details.category)
        // The page links to "https://itorrents.net/...", which the
        // `https://itorrent.net/` prefix selector does not match.
        assertNull(details.fileDownloadLink)
    }

    @Test
    fun `getDetails returns null when the page has no torrent info table`() = runTest {
        val provider = TorrentDownload(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }
}
