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

class OxTorrentTest {
    private val searchUrl = "https://oxtorrent.co/recherche/logiciels/linux"
    private val detailsUrl = "https://oxtorrent.co/torrent/49529/slackware-linux-12-1"

    private val expectedMagnetUri = "magnet:?xt=urn:btih:4AFC68C3DE25522B4C39C4DBAFA67D152113A4BA" +
            "&tr=udp://eddie4.nl:6969/announce" +
            "&tr=udp://shadowshq.yi.org:6969/announce" +
            "&tr=udp://tracker.leechers-paradise.org:6969/announce" +
            "&tr=udp://tracker.pirateparty.gr:6969/announce" +
            "&tr=udp://tracker.coppersurfer.tk:6969/announce" +
            "&tr=udp://tracker.coppersurfer.tk:80/announce" +
            "&tr=udp://9.rarbg.com:2800/announce" +
            "&tr=udp://9.rarbg.me:2780/announce" +
            "&tr=udp://9.rarbg.to:2710/announce" +
            "&tr=udp://p4p.arenabg.com:1337/announce" +
            "&tr=udp://public.popcorn-tracker.org:6969/announce" +
            "&tr=udp://tracker.vanitycore.co:6969/announce" +
            "&tr=udp://open.stealth.si:80/announce" +
            "&tr=udp://tracker.zer0day.to:1337/announce" +
            "&tr=udp://tracker.opentrackr.org:1337/announce" +
            "&tr=udp://tracker.internetwarriors.net:1337/announce" +
            "&tr=udp://ipv4.tracker.harry.lu:80/announce" +
            "&tr=udp://explodie.org:6969/announce" +
            "&tr=http://inferno.demonoid.ph:3389/announce"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = OxTorrent(fakeNetworkClient(searchUrl to fixture("oxtorrent/search.html")))

        val torrents = provider.search(query = "linux", category = Category.Apps)

        assertEquals(
            listOf(
                "Nero linux V3.5",
                "Slackware Linux 12.1",
                // Raw '>' in the anchor text must be kept as text.
                "Distribution Linux -> Mandriva MLO 2009.1",
                "Linux File Systems for Windows",
                "Damn Small Linux 3.4 Virtual Machine",
            ),
            torrents.map { it.name },
        )
        assertEquals(
            listOf("67.3 MB", "3.8 GB", "719.0 MB", "49.1 MB", "48.8 MB"),
            torrents.map { it.size },
        )
        // Counts follow an <img> inside the cell; only the own text counts.
        assertEquals(listOf(1u, 3u, 2u, 20u, 2u), torrents.map { it.seeders })
        assertEquals(listOf(0u, 3u, 1u, 0u, 0u), torrents.map { it.peers })
        assertEquals(
            listOf(
                "https://oxtorrent.co/torrent/49303/nero-linux-v3-5",
                "https://oxtorrent.co/torrent/49529/slackware-linux-12-1",
                "https://oxtorrent.co/torrent/49264/distribution-linux-mandriva-mlo-2009-1",
                "https://oxtorrent.co/torrent/87168/linux-file-systems-for-windows",
                "https://oxtorrent.co/torrent/49510/damn-small-linux-3-4-virtual-machine",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            torrents.map { MagnetUri.RequiresFetch(it.detailsPageUrl!!) },
            torrents.map { it.magnetUri },
        )
        // The row icon class is "Logiciels" (capitalised), which is not a known slug.
        assertTrue(torrents.all { it.category == Category.Other })
        assertTrue(torrents.all { it.providerName == "OxTorrent" })
        assertEquals(5, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = OxTorrent(fakeNetworkClient(searchUrl to "<html><body><p>Aucun résultat</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "linux", category = Category.Apps))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = OxTorrent(fakeNetworkClient(detailsUrl to fixture("oxtorrent/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Slackware Linux 12.1", details.name)
        assertEquals(expectedMagnetUri, details.magnetUri)
        assertEquals("4afc68c3de25522b4c39c4dbafa67d152113a4ba", details.infoHash)
        assertEquals("3.8GB", details.size)
        assertEquals(3u, details.seeders)
        assertEquals(3u, details.peers)
        assertEquals(Instant.parse("2018-08-30T00:00:00Z"), details.uploadDate)
        assertEquals(Category.Apps, details.category)
        assertEquals("<p>Logiciel complet</p>", details.description)
        assertEquals("https://oxtorrent.co/pictures/slackware-linux-12-1.", details.posterUrl)
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = OxTorrent(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri extracts the magnet link from the details page`() = runTest {
        val provider = OxTorrent(fakeNetworkClient(detailsUrl to fixture("oxtorrent/details.html")))

        assertEquals(expectedMagnetUri, provider.getMagnetUri(detailsUrl))
    }

    @Test
    fun `getMagnetUri fails when the page has no magnet link`() = runTest {
        val provider = OxTorrent(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        val result = runCatching { provider.getMagnetUri(detailsUrl) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
