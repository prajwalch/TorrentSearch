package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture

import io.mockk.coEvery
import io.mockk.mockk

import kotlinx.coroutines.test.runTest

import java.time.Instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoNameClubTest {
    private val trackerUrl = "https://nnmclub.to/forum/tracker.php"
    private val detailsUrl = "https://nnmclub.to/forum/viewtopic.php?t=1877323"
    private val magnetUri = "magnet:?xt=urn:btih:5388429463745727BEA06CB190D33D591FAE04B4"

    /** Serves [html] for the tracker search form submission. */
    private fun searchFormClient(html: String): NetworkClient {
        val networkClient = mockk<NetworkClient>()
        coEvery { networkClient.submitForm(trackerUrl, any()) } returns html
        return networkClient
    }

    @Test
    fun `search parses every result row`() = runTest {
        val provider = NoNameClub(searchFormClient(fixture("nonameclub/search.html")))

        val torrents = provider.search(query = "linux", category = Category.All)

        assertEquals(
            listOf(
                "фильм об истории GNU Linux / Revolution OS (2002)",
                "г.Томск ОГУ \"РЦРО\" | Основы операционной системы GNU/Linux [Апрель 2007]",
                "Ядро Linux 3.0.0 [x86/x64] (deb)",
                "Ядро Linux 2.6.39.4 [x86/x64] (deb)",
            ),
            torrents.map { it.name },
        )
        // Sizes are formatted from the byte count in the hidden <u> element.
        assertEquals(listOf("701.37 MB", "6.77 GB", "81.83 MB", "74.77 MB"), torrents.map { it.size })
        assertEquals(listOf(0u, 1u, 0u, 0u), torrents.map { it.seeders })
        assertEquals(listOf(0u, 0u, 0u, 0u), torrents.map { it.peers })
        assertEquals(
            listOf(1179047897L, 1215593714L, 1311355400L, 1313528079L).map(Instant::ofEpochSecond),
            torrents.map { it.uploadDate },
        )
        // Forum ids 669 and 564 are unmapped, 463 (IT video courses) maps to series.
        assertEquals(
            listOf(Category.Other, Category.Series, Category.Other, Category.Other),
            torrents.map { it.category },
        )
        assertEquals(
            listOf(12581, 90564, 366643, 376634).map { "https://nnmclub.to/forum/viewtopic.php?t=$it" },
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(11726, 89003, 332963, 341190).map { "https://nnmclub.to/forum/download.php?id=$it" },
            torrents.map { it.fileDownloadLink },
        )
        assertEquals(torrents.map { MagnetUri.RequiresFetch(it.detailsPageUrl!!) }, torrents.map { it.magnetUri })
        assertTrue(torrents.all { it.providerName == "NoNameClub" })
        assertEquals(4, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for the not found page`() = runTest {
        val notFoundHtml = """
            <table class="forumline tablesorter"><thead><tr><th>Topic</th></tr></thead>
            <tbody></tbody>
            <tr><td class="row1" colspan="11"><span class="gen">Не найдено</span></td></tr>
            </table>
        """.trimIndent()
        val provider = NoNameClub(searchFormClient(notFoundHtml))

        assertEquals(emptyList<Any>(), provider.search(query = "linux", category = Category.All))
    }

    @Test
    fun `getDetails parses the topic page`() = runTest {
        val provider = NoNameClub(fakeNetworkClient(detailsUrl to fixture("nonameclub/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals(
            "Специалист, Сергей Русанов | Linux. Уровень 1. Основы администрирования (2025) PCRec [RU]",
            details.name,
        )
        assertEquals("5388429463745727bea06cb190d33d591fae04b4", details.infoHash)
        assertEquals(magnetUri, details.magnetUri)
        // "&nbsp;4.47&nbsp;GB&nbsp;" in the markup.
        assertEquals("4.47 GB", details.size)
        // "20 Июн 2026 10:12:15" and "Оформление проверено модератором 20 Июн 2026 12:37:36".
        assertEquals(Instant.parse("2026-06-20T00:00:00Z"), details.uploadDate)
        assertEquals(Instant.parse("2026-06-20T00:00:00Z"), details.lastChecked)
        assertEquals(Category.Series, details.category)
        assertEquals("Rphoenxkgn", details.uploader)
        assertNull(details.fileDownloadLink)
        assertEquals(
            "https://nnmstatic.win/forum/image.php?link=https://i.ibb.co/KxYbYzvX/03-best-r-courses.png",
            details.posterUrl,
        )
        assertEquals(
            listOf(
                "https://i.ibb.co/BVZZ05ws/vlcsnap-2026-06-20-12h10m58s693.png",
                "https://i.ibb.co/bR7DvbNd/vlcsnap-2026-06-20-12h10m54s124.png",
            ),
            details.screenshotUrls,
        )
        // Only the first post (the release description) is used.
        assertEquals(
            "<span style=\"text-align: center; display: block;\"><span style=\"color: #006699\"><span style=\"font-size: 20px; line-height: normal\"><span style=\"font-weight: bold\">Специалист, Сергей Русанов | Linux. Уровень 1. Основы администрирования (2025) PCRec [RU] </span></span></span></span>\n" +
            "<hr>\n" +
            "<var class=\"postImg postImgAligned img-right\" title=\"https://nnmstatic.win/forum/image.php?link=https://i.ibb.co/KxYbYzvX/03-best-r-courses.png\"> </var><span style=\"font-weight: bold\">Автор:</span> Сергей Русанов\n" +
            "<br>\n" +
            "<span style=\"font-weight: bold\">Производство:</span> <a href=\"https://www.specialist.ru/course/yun1-b\" rel=\"nofollow noopener noreferrer\" class=\"postLink\">Специалист</a>\n" +
            "<br>\n" +
            "<span style=\"font-weight: bold\">Жанр:</span> Видеокурс\n" +
            "<br>\n" +
            "<br>\n" +
            "<span style=\"font-weight: bold\">Описание:</span>\n" +
            "<br>\n" +
            "Этот курс предоставит Вам основные знания о операционной системе Linux.\n" +
            "<br>\n" +
            "<br>\n" +
            "<span style=\"font-weight: bold\">Продолжительность:</span> 15:14:22\n" +
            "<br>\n" +
            "<span style=\"font-weight: bold\">Качество видео:</span> PCRec\n" +
            "<br>\n" +
            "<span style=\"font-weight: bold\">Язык озвучки:</span> Русский\n" +
            "<div class=\"hide spoiler-wrap\">\n" +
            " <div class=\"hide spoiler-body\" title=\"Содержимое:\">\n" +
            "  <span style=\"font-weight: bold\">Модуль 1. Введение в операционную систему GNU/Linux (1 ак. ч.)\n" +
            "   <br></span>Дистрибутивы\n" +
            "  <br>\n" +
            "  <span style=\"font-weight: bold\">Модуль 2. Установка (1 ак. ч.)\n" +
            "   <br></span>Обзор вариантов\n" +
            "  <br>\n" +
            "  Этапы установки\n" +
            " </div>\n" +
            "</div>\n" +
            "<div class=\"clear\"></div>\n" +
            "<center>\n" +
            " <span style=\"font-weight: bold\">Скриншоты:</span>\n" +
            " <br>\n" +
            " <a href=\"https://i.ibb.co/BVZZ05ws/vlcsnap-2026-06-20-12h10m58s693.png\" style=\"text-decoration:underline #3ab991;cursor:zoom-in\" class=\"highslide\" onclick=\"return hs.expand(this,{slideshowGroup: '13049498'})\" rel=\"nofollow noopener noreferrer\"><var class=\"postImg\" title=\"https://nnmstatic.win/forum/image.php?link=https://i.ibb.co/rGbYtqKp/thumb-e9eb6e6e8ae862af89f9dc1145977712.png\"> </var></a> <a href=\"https://i.ibb.co/bR7DvbNd/vlcsnap-2026-06-20-12h10m54s124.png\" style=\"text-decoration:underline #3ab991;cursor:zoom-in\" class=\"highslide\" onclick=\"return hs.expand(this,{slideshowGroup: '13049498'})\" rel=\"nofollow noopener noreferrer\"><var class=\"postImg\" title=\"https://nnmstatic.win/forum/image.php?link=https://i.ibb.co/spJ1JBQH/thumb-e58cb55bc1c1a75753bb9d5c26eae682.png\"> </var></a>\n" +
            " <br>\n" +
            "</center>\n" +
            "<br>\n" +
            "<span style=\"font-size: 11px; line-height: normal\"><span style=\"font-weight: bold\">Время раздачи:</span> по возможности (до появления первых 3-5 скачавших)</span>",
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the topic has no magnet link`() = runTest {
        val html = """<html><body><a class="maintitle" href="viewtopic.php?t=1">Тема</a></body></html>"""
        val provider = NoNameClub(fakeNetworkClient(detailsUrl to html))

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri extracts the magnet link from the topic page`() = runTest {
        val provider = NoNameClub(fakeNetworkClient(detailsUrl to fixture("nonameclub/details.html")))

        assertEquals(magnetUri, provider.getMagnetUri(detailsUrl))
    }
}
