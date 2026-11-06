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
import org.junit.Assert.assertTrue
import org.junit.Test

class BTDiggTest {
    private val searchUrl = "https://btdig.com/search?q=ubuntu"
    private val detailsUrl = "https://btdig.com/f4f4b2530901b9ec81ae9cbea124a21a82826bfa/ubuntu"

    private val trackers =
        "&tr=udp://tracker.openbittorrent.com:80&tr=udp://tracker.opentrackr.org:1337/announce"

    @Test
    fun `search parses every result`() = runTest {
        val provider = BTDigg(fakeNetworkClient(searchUrl to fixture("btdigg/search.html")))

        val before = Instant.now()
        val torrents = provider.search(query = "ubuntu", category = Category.Other)
        val after = Instant.now()

        // The highlighted <b>Ubuntu</b> is part of the name text.
        assertEquals(
            listOf("Ubuntu MATE ISO Archive (2025)", "the-linux-archives", "Персонал"),
            torrents.map { it.name },
        )
        assertEquals(listOf("181.26 GB", "70.88 GB", "1.32 GB"), torrents.map { it.size })
        assertEquals(
            listOf(
                "https://btdig.com/f4f4b2530901b9ec81ae9cbea124a21a82826bfa/ubuntu",
                "https://btdig.com/2dbfef6dcc85b1b8c6267aa38a328385d8fe02aa/ubuntu",
                "https://btdig.com/917626196685f4ca6b69ec44862ddd0883a56c2c/ubuntu",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(
                "magnet:?xt=urn:btih:f4f4b2530901b9ec81ae9cbea124a21a82826bfa" +
                        "&dn=Ubuntu+MATE+ISO+Archive+(2025)" + trackers,
                "magnet:?xt=urn:btih:2dbfef6dcc85b1b8c6267aa38a328385d8fe02aa" +
                        "&dn=the-linux-archives" + trackers,
                "magnet:?xt=urn:btih:917626196685f4ca6b69ec44862ddd0883a56c2c" +
                        "&dn=%D0%9F%D0%B5%D1%80%D1%81%D0%BE%D0%BD%D0%B0%D0%BB" + trackers,
            ).map(MagnetUri::Available),
            torrents.map { it.magnetUri },
        )
        // "found 5 months ago", "found 1 year ago", "found 5 years ago".
        listOf(150L, 365L, 5 * 365L).zip(torrents) { days, torrent ->
            val uploadDate = torrent.uploadDate
            assertNotNull(uploadDate)
            val age = Duration.ofDays(days)
            assertTrue(uploadDate!! in before.minus(age)..after.minus(age))
        }
        assertTrue(torrents.all { it.seeders == null && it.peers == null && it.category == null })
        assertTrue(torrents.all { it.providerName == "BTDigg" })
        assertEquals(3, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without results`() = runTest {
        val provider = BTDigg(
            fakeNetworkClient(searchUrl to "<html><body><div>No results found</div></body></html>"),
        )

        assertEquals(emptyList<Any>(), provider.search(query = "ubuntu", category = Category.Other))
    }

    @Test
    fun `getDetails parses the details table`() = runTest {
        val provider = BTDigg(fakeNetworkClient(detailsUrl to fixture("btdigg/details.html")))

        val before = Instant.now()
        val details = provider.getDetails(detailsUrl)
        val after = Instant.now()

        assertNotNull(details)
        details!!
        assertEquals("Ubuntu MATE ISO Archive (2025)", details.name)
        assertEquals("f4f4b2530901b9ec81ae9cbea124a21a82826bfa", details.infoHash)
        assertEquals("181.26 GB", details.size)
        assertEquals(
            "magnet:?xt=urn:btih:f4f4b2530901b9ec81ae9cbea124a21a82826bfa" +
                    "&dn=Ubuntu+MATE+ISO+Archive+(2025)" + trackers,
            details.magnetUri,
        )
        // "Age: 5 months".
        val age = Duration.ofDays(150)
        assertNotNull(details.uploadDate)
        assertTrue(details.uploadDate!! in before.minus(age)..after.minus(age))
    }

    @Test
    fun `getDetails returns null when the page has no details table`() = runTest {
        val provider = BTDigg(fakeNetworkClient(detailsUrl to "<html><body><p>Not found</p></body></html>"))

        assertEquals(null, provider.getDetails(detailsUrl))
    }
}
