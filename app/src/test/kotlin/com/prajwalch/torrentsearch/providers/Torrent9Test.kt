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

class Torrent9Test {
    private val searchUrl = "https://www6.torrent9.to/search_torrent/logiciels/ubuntu.html"
    private val detailsUrl = "https://www6.torrent9.to/torrent/49504/super-ubuntu-2008-09"

    private val magnetUri = "magnet:?xt=urn:btih:A986AF00AD729F22076B3359F639F2EC6B3B8DF3" +
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
        val provider = Torrent9(fakeNetworkClient(searchUrl to fixture("torrent9/search.html")))

        val torrents = provider.search(query = "ubuntu", category = Category.Apps)

        // Name is the text of the whole link, including the highlighted <span>.
        assertEquals(
            listOf(
                "Ubuntu Ultimate Edition 1.4 DVD",
                "Super Ubuntu 2008.09",
                "Ubuntu 10.04 Desktop (32 bits)",
            ),
            torrents.map { it.name },
        )
        // French units: "1.9Go" -> "1.9 GB".
        assertEquals(listOf("1.9 GB", "872.0 MB", "700.4 MB"), torrents.map { it.size })
        // Seeders/peers cells contain trailing whitespace and an <img>.
        assertEquals(listOf(3u, 6u, 9u), torrents.map { it.seeders })
        assertEquals(listOf(3u, 1u, 2u), torrents.map { it.peers })
        assertTrue(torrents.all { it.uploadDate == Instant.parse("2018-08-30T00:00:00Z") })
        assertTrue(torrents.all { it.category == Category.Apps })
        assertTrue(torrents.all { it.providerName == "Torrent9" })
        assertEquals(3, torrents.map { it.id }.toSet().size)
        // Relative hrefs resolved against the page URL.
        assertEquals(
            listOf(
                "https://www6.torrent9.to/torrent/49542/ubuntu-ultimate-edition-1-4-dvd",
                "https://www6.torrent9.to/torrent/49504/super-ubuntu-2008-09",
                "https://www6.torrent9.to/torrent/48951/ubuntu-10-04-desktop-32-bits",
            ),
            torrents.map { (it.magnetUri as MagnetUri.RequiresFetch).url },
        )
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = Torrent9(fakeNetworkClient(searchUrl to "<html><body><p>Aucun torrent</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "ubuntu", category = Category.Apps))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = Torrent9(fakeNetworkClient(detailsUrl to fixture("torrent9/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Super Ubuntu 2008.09", details.name)
        assertEquals(magnetUri, details.magnetUri)
        assertEquals("a986af00ad729f22076b3359f639f2ec6b3b8df3", details.infoHash)
        assertEquals("872.0 MB", details.size)
        assertEquals(6u, details.seeders)
        assertEquals(1u, details.peers)
        assertEquals(Instant.parse("2018-08-30T00:00:00Z"), details.uploadDate)
        assertEquals(Category.Apps, details.category)
        assertEquals("https://www6.torrent9.to/pictures/super-ubuntu-2008-09.", details.posterUrl)
        // Pretty-printed html() collapses the newlines of the text node.
        assertEquals(
            "Super Ubuntu is just Ubuntu, but it includes: " +
                    "* System updates: all updates released until September 2008 " +
                    "* Updated Software: new versions of Brasero, Pidgin and Transmission " +
                    "* Better Multimedia Support: multimedia codecs, MPlayer and VLC " +
                    "* Better Internet experience: aMSN, aMule, Adobe Flash player and Opera " +
                    "* Support for more technologies: Autopackage, Java, Klik, Microsoft Office " +
                    "2007 file formats, SFS Technology, Wine and Zero Install " +
                    "* Graphical interfaces: compizconfig-settings-manager (Compiz), Gufw " +
                    "(Uncomplicated firewall), ndisgtk (NDISwrapper) and padevchooser (PulseAudio) " +
                    "* Utilities: Adobe Reader, Envy, Furius ISO Mount, GParted, NDISwrapper, " +
                    "Ubuntu Tweak and StartUp-Manager " +
                    "* Repositories: more repositories available",
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = Torrent9(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri extracts the magnet link from the details page`() = runTest {
        val provider = Torrent9(fakeNetworkClient(detailsUrl to fixture("torrent9/details.html")))

        assertEquals(magnetUri, provider.getMagnetUri(detailsUrl))
    }
}
