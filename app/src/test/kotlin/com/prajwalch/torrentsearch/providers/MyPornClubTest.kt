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

// Fixtures are hand-written with neutral placeholder content that mirrors the
// site's markup structure.
class MyPornClubTest {
    private val searchUrl = "https://myporn.club/s/sample/seeders"
    private val detailsUrl = "https://myporn.club/t/a1b2c3d4e5"
    private val magnetUri =
        "magnet:?xt=urn:btih:9F86D081884C7D659A2FEAA0C55AD015A3BF4F1B" +
                "&dn=Sample+Clip+One&tr=udp%3A%2F%2Ftracker.example.org%3A1337%2Fannounce"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = MyPornClub(fakeNetworkClient(searchUrl to fixture("mypornclub/search.html")))

        val before = Instant.now()
        val torrents = provider.search(query = "sample", category = Category.Porn)

        assertEquals(
            listOf("Sample Clip One & Friends", "Placeholder Scene Two", "Test Video Three"),
            torrents.map { it.name },
        )
        assertEquals(listOf("1.25 GB", "700.5 MB", "4.02 GB"), torrents.map { it.size })
        assertEquals(listOf(42u, 0u, 3u), torrents.map { it.seeders })
        assertEquals(listOf(7u, 12u, 0u), torrents.map { it.peers })
        assertEquals(
            listOf(
                "https://myporn.club/t/a1b2c3d4e5?from=search",
                "https://myporn.club/t/f6e5d4c3b2",
                "https://myporn.club/t/0099aabbcc?from=search&page=1",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(torrents.map { MagnetUri.RequiresFetch(it.detailsPageUrl!!) }, torrents.map { it.magnetUri })
        // Relative upload dates: "3 days ago", "5 hours ago", "1 Year+".
        val expectedAges = listOf(Duration.ofDays(3), Duration.ofHours(5), Duration.ofDays(365))
        expectedAges.zip(torrents.map { it.uploadDate }).forEach { (age, uploadDate) ->
            assertNotNull(uploadDate)
            val expected = before.minus(age)
            assertTrue(
                "$uploadDate is not ~$age ago",
                Duration.between(expected, uploadDate).abs() < Duration.ofMinutes(1),
            )
        }
        assertTrue(torrents.all { it.category == Category.Porn })
        assertTrue(torrents.all { it.providerName == "MyPornClub" })
        assertEquals(3, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without the torrents list`() = runTest {
        val provider = MyPornClub(fakeNetworkClient(searchUrl to "<html><body><p>Nothing found</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "sample", category = Category.Porn))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = MyPornClub(fakeNetworkClient(detailsUrl to fixture("mypornclub/details.html")))

        val before = Instant.now()
        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        // Tags (#...) following the name are dropped.
        assertEquals("Sample Clip One & Friends", details.name)
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b", details.infoHash)
        assertEquals(magnetUri, details.magnetUri)
        assertEquals("1.25 GB", details.size)
        assertEquals(42u, details.seeders)
        assertEquals(7u, details.peers)
        assertEquals("placeholder_uploader", details.uploader)
        assertEquals(Category.Porn, details.category)
        assertEquals("https://myporn.club/download/a1b2c3d4e5.torrent", details.fileDownloadLink)
        // "[uploaded]: 3 days ago" and "[last checked]: 2 hours ago".
        assertTrue(
            "${details.uploadDate}",
            Duration.between(before.minus(Duration.ofDays(3)), details.uploadDate).abs() < Duration.ofMinutes(1),
        )
        assertTrue(
            "${details.lastChecked}",
            Duration.between(before.minus(Duration.ofHours(2)), details.lastChecked).abs() < Duration.ofMinutes(1),
        )
    }

    @Test
    fun `getDetails returns null when the page has no info hash`() = runTest {
        val provider = MyPornClub(
            fakeNetworkClient(detailsUrl to "<html><body><div class=\"torrent_text\">Sample</div></body></html>"),
        )

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri extracts the magnet link from the details page`() = runTest {
        val provider = MyPornClub(fakeNetworkClient(detailsUrl to fixture("mypornclub/details.html")))

        assertEquals(magnetUri, provider.getMagnetUri(detailsUrl))
    }
}
