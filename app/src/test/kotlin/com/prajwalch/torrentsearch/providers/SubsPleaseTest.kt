package com.prajwalch.torrentsearch.providers

import android.net.Uri

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.testing.fixture

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

import java.time.Instant

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SubsPleaseTest {
    private val searchUrl = "https://subsplease.org/api?f=search&tz=$&s=frieren"
    private val showUrl = "https://subsplease.org/api/?f=show&tz=$&sid=620"
    private val detailsUrl = "https://subsplease.org/sousou-no-frieren?ep=28&res=1080"

    @Before
    fun setUp() {
        // android.net.Uri is stubbed on the JVM. The provider only reads the
        // query part of the magnet URI to find the "xl" (exact length) param.
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers {
            val uriString = firstArg<String>()
            mockk { every { query } returns uriString.substringAfter('?') }
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `search parses every download of every show`() = runTest {
        val networkClient = mockk<NetworkClient>()
        coEvery { networkClient.getJson(searchUrl, any()) } returns json("subsplease/search.json")
        val provider = SubsPlease(networkClient)

        val torrents = provider.search(query = "frieren", category = Category.Anime)

        assertEquals(
            listOf(
                "Sousou no Frieren - 01-28 [480p]",
                "Sousou no Frieren - 01-28 [720p]",
                "Sousou no Frieren - 01-28 [1080p]",
                "Sousou no Frieren S2 - 10 [480p]",
                "Sousou no Frieren S2 - 10 [720p]",
                "Sousou no Frieren S2 - 10 [1080p]",
            ),
            torrents.map { it.name },
        )
        assertEquals(
            listOf("10.43 GB", "20.05 GB", "39.30 GB", "369.34 MB", "710.77 MB", "1.36 GB"),
            torrents.map { it.size },
        )
        assertEquals(
            listOf(
                "2026-05-27T17:36:01Z",
                "2026-05-27T17:36:01Z",
                "2026-05-27T17:36:01Z",
                "2026-03-27T15:02:39Z",
                "2026-03-27T15:02:39Z",
                "2026-03-27T15:02:39Z",
            ).map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        assertEquals(
            listOf(
                "https://subsplease.org/sousou-no-frieren?ep=01-28&res=480",
                "https://subsplease.org/sousou-no-frieren?ep=01-28&res=720",
                "https://subsplease.org/sousou-no-frieren?ep=01-28&res=1080",
                "https://subsplease.org/sousou-no-frieren-s2?ep=10&res=480",
                "https://subsplease.org/sousou-no-frieren-s2?ep=10&res=720",
                "https://subsplease.org/sousou-no-frieren-s2?ep=10&res=1080",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(
                "RG4AGGQUMB4MGRUYN745JBZDJAOFWMNY",
                "T7E4KFRNHCBPFVBMECSNZHS3X75DIGRN",
                "DXVR3ULSUUBCR24KSSUVGXDNBEUXN3SE",
                "JS2UBQPDGNQW6IB2RGKIROFLNM6MBVIS",
                "XIMG2VNRTY4E7BOWCJXG6SGFCPOG5B5F",
                "M7TFKY5L64IYJGIKKTENHIIR4NE52ZZP",
            ),
            torrents.map {
                (it.magnetUri as MagnetUri.Available).value
                    .removePrefix("magnet:?xt=urn:btih:")
                    .substringBefore('&')
            },
        )
        assertTrue(torrents.all { it.category == Category.Anime })
        assertTrue(torrents.all { it.providerName == "SubsPlease" })
        assertEquals(6, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for an empty array response`() = runTest {
        val networkClient = mockk<NetworkClient>()
        coEvery { networkClient.getJson(searchUrl, any()) } returns Json.parseToJsonElement("[]")
        val provider = SubsPlease(networkClient)

        assertEquals(emptyList<Any>(), provider.search(query = "frieren", category = Category.Anime))
    }

    @Test
    fun `getDetails combines the show page with the show API response`() = runTest {
        val networkClient = mockk<NetworkClient>()
        coEvery { networkClient.getText(detailsUrl, any()) } returns fixture("subsplease/details.html")
        coEvery { networkClient.getJson(showUrl, any()) } returns json("subsplease/show.json")
        val provider = SubsPlease(networkClient)

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Sousou no Frieren - 28 [1080p]", details.name)
        assertTrue(
            details.magnetUri,
            details.magnetUri.startsWith(
                "magnet:?xt=urn:btih:QER5LYPJAXZQAUYKHNSLO4O6MNV6UD6A" +
                        "&dn=%5BSubsPlease%5D%20Sousou%20no%20Frieren%20-%2028%20%281080p%29%20%5B8BBBC28C%5D.mkv" +
                        "&xl=1490354395&tr=",
            ),
        )
        assertEquals("qer5lypjaxzqauykhnslo4o6mnv6ud6a", details.infoHash)
        assertEquals("1.39 GB", details.size)
        assertEquals(Instant.parse("2024-03-22T00:00:00Z"), details.uploadDate)
        assertEquals(Category.Anime, details.category)
        assertEquals("https://nyaa.si/view/1793092/torrent", details.fileDownloadLink)
        assertEquals("https://subsplease.org/wp-content/uploads/2023/09/138006.jpg", details.posterUrl)
        // Only the first synopsis paragraph is used.
        assertEquals(
            "The demon king has been defeated, and the victorious hero party returns home before " +
                    "disbanding. The four—mage Frieren, hero Himmel, priest Heiter, and warrior Eisen—" +
                    "reminisce about their decade-long journey as the moment to bid each other farewell " +
                    "arrives. But the passing of time is different for elves, thus Frieren witnesses her " +
                    "companions slowly pass away one by one.",
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the page has no release table`() = runTest {
        val networkClient = mockk<NetworkClient>()
        coEvery { networkClient.getText(detailsUrl, any()) } returns
                "<html><body><h1 class=\"entry-title\">Sousou no Frieren</h1></body></html>"
        val provider = SubsPlease(networkClient)

        assertNull(provider.getDetails(detailsUrl))
    }

    private fun json(path: String) = Json.parseToJsonElement(fixture(path))
}
