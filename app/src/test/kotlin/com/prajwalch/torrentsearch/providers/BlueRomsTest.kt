package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueRomsTest {
    private val searchUrl = "https://www.blueroms.ws/search?g=0&p=0&q=mario"
    private val detailsUrl = "https://www.blueroms.ws/game/gamecube/dance_dance_revolution_mario_mix"
    private val downloadToken =
        "gAAAAABqya9dG6uzzVZqH-RZfeAU08GVcLFkJhqKBmz_l22PehgroY1pAI-5nuxkDyogDMESbGAl0m9-BrzSuqlcnHL9ez" +
                "p0sy0yQtrH1B7I9dHScR3vXau4cnf1ZcA3NmWftzJYmnak5FITIxkqploDO-ve1AeaoUe4N6k1JGZBZAcMDLojzTf9WCgia" +
                "NBydKu_bhFXNDvOt6lzKkbCJwPrsLhJAn5D4Q%3D%3D"
    private val downloadUrl = "https://www.blueroms.ws/download/$downloadToken"
    private val expectedMagnetUri =
        "magnet:?xt=urn:btih:5abe078babce41fdf371c9b0a1bb7b8bf175db25" +
                "&xt=urn:btmh:1220e818934b90732a9078df294d88922eb89da9ceaae99caff27227d136c0d44f06" +
                "&dn=Dance Dance Revolution - Mario Mix (USA) (Rev 1)" +
                "&tr=udp://tracker.opentrackr.org:1337/announce" +
                "&tr=udp://open.stealth.si:80/announce" +
                "&tr=udp://tracker.torrent.eu.org:451/announce"

    @Test
    fun `search parses every result card`() = runTest {
        val provider = BlueRoms(fakeNetworkClient(searchUrl to fixture("blueroms/search.html")))

        val torrents = provider.search(query = "mario", category = Category.Games)

        // Name is "<game name> - <platform>", the platform being the element
        // following the "Platform:" label.
        assertEquals(
            listOf(
                "Dance Dance Revolution: Mario Mix - Nintendo GameCube",
                "Dr. Mario - Game Boy Advance",
                "Dr. Mario 64 - Nintendo 64",
            ),
            torrents.map { it.name },
        )
        // Size is the text node following the "Size:" label.
        assertEquals(listOf("227.0 MiB", "932.8 KiB", "2.9 MiB"), torrents.map { it.size })
        assertEquals(
            listOf(
                "https://www.blueroms.ws/game/gamecube/dance_dance_revolution_mario_mix",
                "https://www.blueroms.ws/game/gba/dr_mario",
                "https://www.blueroms.ws/game/n64/dr_mario_64",
            ),
            torrents.map { it.detailsPageUrl },
        )
        val downloadTokens = listOf(
            "gAAAAABqya9PV3sqy2r4OkQW0VaaKVGIj_uUEv3Niskle5QP7FM0SMAC2FY9YdtpYs1mSCBc2zCTGNJ1uMHSYjILpYYjGP17" +
                    "OWvE4Pek9Dms4vy0WWcQfWKL_DyUDYah6SpLCjAkVV-H4UefIXzMA9poZPdmnTf5wlx0xodUPAVEF0hHzxD2WSYCyHvwf-" +
                    "4r3Ktjk8QWRG6XYKOrDVn66A5SdMjUSaY7sw%3D%3D",
            "gAAAAABqya9PRbHenL6-_UpNqa0pugKPbsS94ZUjHJA1EKcJd4ZhJIwVD5PYeT-iXa5zytbzxVuGrHtZOGe3TMU3FX71yJVQ" +
                    "pF9nEWjyE4nCyzeHGys81b_VIiI6w1uduRA4RoFv9u6NEPL0exgZwyUV8l2tnjcbPOAubWNbgJUNHi4dshiYcAm85PSC29" +
                    "UeFNUBdWEHhDz9FSw_Vxehi07lkTpnO9JoyA%3D%3D",
            "gAAAAABqya9P6rzEjNw7Ud4x0UmxFxYkJeUhZxD9HqeAAyeGHMx25fqIfk0v4eWkqh2pS_VM3nHC0j6UkcSOAv2xBGfCgug3" +
                    "Emn9oo4DhRuT0byzjoBWB78jRPYyxtzNllE6TXRVoD-lswlbIFeAFQaff7y3nBgpYbN61WDJpgoBM6vZ6rBbk1Hw6KKlBr" +
                    "rO-uZCpbbrEnkySyVxtD3cOD1Tc4IaFCAWnA%3D%3D",
        )
        assertEquals(
            downloadTokens.map { MagnetUri.RequiresFetch("https://www.blueroms.ws/download/$it") },
            torrents.map { it.magnetUri },
        )
        assertEquals(
            downloadTokens.map { TorrentUtils.createTorrentId(providerId = "blueroms", sourceId = it) },
            torrents.map { it.id },
        )
        assertTrue(torrents.all { it.category == Category.Games })
        assertTrue(torrents.all { it.providerName == "BlueRoms" })
        assertTrue(torrents.all { it.seeders == null && it.peers == null && it.uploadDate == null })
    }

    @Test
    fun `search returns no results for a page without result cards`() = runTest {
        val provider = BlueRoms(
            fakeNetworkClient(searchUrl to "<html><body><h3>No results found</h3></body></html>"),
        )

        assertEquals(emptyList<Any>(), provider.search(query = "mario", category = Category.Games))
    }

    @Test
    fun `getMagnetUri decodes the base64 magnet link of the download page`() = runTest {
        val provider = BlueRoms(fakeNetworkClient(downloadUrl to fixture("blueroms/download.html")))

        assertEquals(expectedMagnetUri, provider.getMagnetUri(downloadUrl))
    }

    @Test
    fun `getMagnetUri fails when the download page has no magnet button`() = runTest {
        val provider = BlueRoms(fakeNetworkClient(downloadUrl to "<html><body></body></html>"))

        val failure = runCatching { provider.getMagnetUri(downloadUrl) }.exceptionOrNull()

        assertTrue(failure.toString(), failure is IllegalStateException)
    }

    @Test
    fun `getDetails parses the details page and its download page`() = runTest {
        val provider = BlueRoms(
            fakeNetworkClient(
                detailsUrl to fixture("blueroms/details.html"),
                downloadUrl to fixture("blueroms/download.html"),
            ),
        )

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        // Platform and size are the parent's own text after the label.
        assertEquals("Dance Dance Revolution: Mario Mix ISO - GameCube - Nintendo GameCube", details.name)
        assertEquals("227.0 MiB", details.size)
        assertEquals(expectedMagnetUri, details.magnetUri)
        assertEquals("5abe078babce41fdf371c9b0a1bb7b8bf175db25", details.infoHash)
        assertEquals("https://www.blueroms.ws/static/game/388/imgs/covers/cover1.jpg", details.posterUrl)
        // Only the thumbnails of the "Screenshots" panel, not the "Covers" panel.
        assertEquals(
            listOf(
                "https://www.blueroms.ws/static/game/388/imgs/screenshots/screenshot1.png",
                "https://www.blueroms.ws/static/game/388/imgs/screenshots/screenshot2.jpg",
                "https://www.blueroms.ws/static/game/388/imgs/screenshots/screenshot3.jpg",
            ),
            details.screenshotUrls,
        )
    }

    @Test
    fun `getDetails returns null when the page has no download link`() = runTest {
        val provider = BlueRoms(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }
}
