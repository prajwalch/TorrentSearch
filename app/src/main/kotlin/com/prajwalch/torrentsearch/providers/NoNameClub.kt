package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUriState
import com.prajwalch.torrentsearch.domain.model.SearchProviderSafety
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.provider.LatestTorrentsProvider
import com.prajwalch.torrentsearch.provider.MagnetUriProvider
import com.prajwalch.torrentsearch.provider.SearchProvider
import com.prajwalch.torrentsearch.provider.SearchProviderId
import com.prajwalch.torrentsearch.provider.TopTorrentsProvider
import com.prajwalch.torrentsearch.provider.TorrentDetailsProvider
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

private object FormParamValues {
    const val CRITERIA_REGISTERED = 1
    const val CRITERIA_TOPIC_TITLE = 2
    const val CRITERIA_SEEDERS = 10
    // const val CRITERIA_PEERS = 11

    // const val ORDER_ASC = 1
    const val ORDER_DESC = 2

    const val DATE_ALL_TIME = -1
    const val DATE_TODAY = 1
    // const val DATE_LAST_3_DAYS = 3
    // const val DATE_LAST_WEEK = 7
    // const val DATE_LAST_2_WEEKS = 14
    // const val DATE_LAST_MONTH = 30
}


class NoNameClub(private val networkClient: NetworkClient) :
    SearchProvider,
    LatestTorrentsProvider,
    TopTorrentsProvider,
    MagnetUriProvider,
    TorrentDetailsProvider {
    override val id = "nonameclub"
    override val name = "NoNameClub"
    override val url = "https://nnmclub.to"
    override val supportedCategories = setOf(
        Category.Anime,
        Category.Apps,
        Category.Books,
        Category.Games,
        Category.Movies,
        Category.Music,
        Category.Series,
        Category.Other,
    )
    override val safety = SearchProviderSafety.Safe
    override val enabledByDefault = false

    private val categoryToIdMap = mapOf(
        Category.Anime to 615,
        Category.Apps to 512,
        Category.Books to 434,
        Category.Games to 410,
        Category.Movies to 216,
        Category.Music to 313,
        Category.Series to 768,
        Category.Other to 48,
    )
    private val resultsPageParser = NnmClubResultsPageParser(id, name)

    override suspend fun search(query: String, category: Category): List<Torrent> {
        val requestUrl = "$url/forum/tracker.php"
        val formData = buildRequestFormPayload(
            query = query,
            category = category,
            sortCriteria = FormParamValues.CRITERIA_TOPIC_TITLE,
            dateRange = FormParamValues.DATE_ALL_TIME,
        )
        val responseHtml = networkClient.submitForm(url = requestUrl, formData = formData)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    private fun buildRequestFormPayload(
        query: String?,
        category: Category,
        sortCriteria: Int,
        dateRange: Int,
    ): Map<String, String> = buildMap {
        put("f[]", categoryToIdMap[category]?.toString() ?: "-1")
        // Organize (Sort criteria)
        //
        // 1 = Registered
        // 2 = Topic title
        // 10 = Seeders
        // 11 = Peers/Leechers
        put("o", sortCriteria.toString())
        // Sort order
        //
        // 1 = Ascending
        // 2 = Descending
        put("s", FormParamValues.ORDER_DESC.toString())
        // Torrents for (Date)
        //
        // -1 = All time
        // 1 = Today
        // 3 = Last 3 days
        // 7 = Last week
        // 14 = Last 2 weeks
        // 30 = Last month
        // 90 = Last 3 months
        put("tm", dateRange.toString())
        put("shf", "1")
        put("ta", "-1")
        put("sns", "-1")
        put("sds", "4")
        // Search query
        // Blank when querying lastest/top torrents
        put("nm", query ?: "")
        put("submit", "Поиск")
    }

    override suspend fun getMagnetUri(sourceUrl: String): String {
        val detailsPageHtml = networkClient.getText(sourceUrl)
        return NnmClubDetailsPageParser.extractMagnetUri(detailsPageHtml)
            ?: error("Failed to retrieve magnet URI from '$sourceUrl'")
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val detailsPageHtml = networkClient.getText(detailsPageUrl)
        return NnmClubDetailsPageParser.parse(detailsPageHtml)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/forum/tracker.php"
        val requestPayload = buildRequestFormPayload(
            query = null,
            category = category,
            sortCriteria = FormParamValues.CRITERIA_REGISTERED,
            dateRange = FormParamValues.DATE_TODAY,
        )
        val responseHtml = networkClient.submitForm(requestUrl, requestPayload)

        return resultsPageParser.parse(responseHtml, requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/forum/tracker.php"
        val requestPayload = buildRequestFormPayload(
            query = null,
            category = category,
            sortCriteria = FormParamValues.CRITERIA_SEEDERS,
            dateRange = FormParamValues.DATE_ALL_TIME,
        )
        val responseHtml = networkClient.submitForm(requestUrl, requestPayload)

        return resultsPageParser.parse(responseHtml, requestUrl)
    }
}

private class NnmClubResultsPageParser(
    private val providerId: SearchProviderId,
    private val providerName: String,
) {
    private companion object {
        private const val LIST_ITEM =
            """table.forumline.tablesorter > tbody > tr:has(a[href^="viewtopic.php?t="]):has(a[href^="download.php?id="])"""
        private const val TORRENT_NAME = """a[href^="viewtopic.php?t="] > b"""
        private const val SIZE = "td:nth-child(6) > u"
        private const val SEEDERS = "td.seedmed > b"
        private const val PEERS = "td.leechmed > b"
        private const val UPLOAD_DATE = "td:last-child > u"
        private const val CATEGORY = """a[href^="tracker.php?f="]"""
        private const val FILE_DOWNLOAD_LINK = """a[href^="download.php?id="]"""
        private const val DETAILS_PAGE_URL = """a[href^="viewtopic.php?t="]"""
    }

    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href") ?: return null

        val torrentRemoteId = detailsPageUrl.substringAfterLast('=').takeIf { it.isNotBlank() }
        val torrentId = TorrentUtils.createTorrentId(
            providerId = providerId,
            sourceId = torrentRemoteId ?: detailsPageUrl,
        )

        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()?.let(FileSizeUtils::formatBytes)
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.toLongOrNull()
            ?.let(TorrentDateParser::epochSecondToInstant)
        val category = listItem.selectFirst(CATEGORY)
            ?.attr("href")
            ?.removePrefix("tracker.php?f=")
            ?.takeWhile { it != '&' }
            ?.let(::getCategoryFromId)
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")

        return Torrent(
            id = torrentId,
            name = torrentName,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = category,
            providerName = providerName,
            magnetUriState = MagnetUriState.FetchRequired(detailsPageUrl),
            fileDownloadLink = fileDownloadLink,
            descriptionPageUrl = detailsPageUrl,
        )
    }
}

private object NnmClubDetailsPageParser {
    private const val TORRENT_NAME = "a.maintitle"
    private const val SIZE_LABEL = "td:containsOwn(Размер:)"
    private const val UPLOAD_DATE_LABEL = "td:containsOwn(Зарегистрирован:)"
    private const val LAST_CHECKED_LABEL = "td:containsOwn(Проверка:)"
    private const val CATEGORY = "span.nav > a.nav:nth-child(2)"
    private const val UPLOADER = "span.genmed > b"
    private const val DESCRIPTION = "div.postbody"
    private const val POSTER_IMAGE = "var.postImg"
    private const val SCREENSHOT = "a.highslide"
    private const val MAGNET_URI = """a[href^="magnet:?xt="]"""

    suspend fun parse(detailsPageHtml: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val dom = Jsoup.parse(detailsPageHtml)

            val torrentName = dom.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = dom.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val size = dom.selectFirst(SIZE_LABEL)
                ?.nextElementSibling()
                ?.selectFirst("span:nth-child(1)")
                ?.ownText()
                ?.trim()
            val uploadDate = dom.selectFirst(UPLOAD_DATE_LABEL)
                ?.nextElementSibling()
                ?.ownText()
                ?.trim()
                ?.dropLastWhile { !it.isWhitespace() }
                ?.trim()
                ?.let(TorrentDateParser::convertRussianMonthToEnglish)
                ?.let { TorrentDateParser.parse(date = it, format = "dd MMM yyyy") }
            val lastChecked = dom.selectFirst(LAST_CHECKED_LABEL)
                ?.nextElementSibling()
                ?.ownText()
                ?.trim()
                ?.removePrefix("Оформление проверено модератором")
                ?.trim()
                ?.dropLastWhile { !it.isWhitespace() }
                ?.trim()
                ?.let(TorrentDateParser::convertRussianMonthToEnglish)
                ?.let { TorrentDateParser.parse(date = it, format = "dd MMM yyyy") }
            val category = dom.selectFirst(CATEGORY)
                ?.attr("href")
                ?.takeLastWhile { it != '=' }
                ?.let(::getCategoryFromId)
            val uploader = dom.selectFirst(UPLOADER)?.ownText()
            val description = dom.selectFirst(DESCRIPTION)?.html()
            val posterImageUrl = dom.selectFirst(POSTER_IMAGE)?.attr("title")
            val screenshotUrls = dom.select(SCREENSHOT).map { it.attr("href") }

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                name = torrentName,
                size = size,
                uploadDate = uploadDate,
                lastChecked = lastChecked,
                category = category,
                uploader = uploader,
                description = description,
                magnetUri = magnetUri,
                posterUrl = posterImageUrl,
                screenshotUrls = screenshotUrls,
            )
        }

    suspend fun extractMagnetUri(detailsPageHtml: String): String? =
        withContext(Dispatchers.Default) {
            Jsoup.parse(detailsPageHtml).selectFirst(MAGNET_URI)?.attr("href")
        }
}

// Ids are taken from https://github.com/Jackett/Jackett/blob/master/src/Jackett.Common/Definitions/noname-club.yml#L18
private fun getCategoryFromId(id: String): Category = when (id) {
    // Forum
    "48", "925", "872" -> Category.Other

    // Everything for children and parents (mostly TV)
    "724", "725", "729", "731", "1345", "733", "1346", "1329", "1330",
    "1331", "1332", "1340", "658", "890", "1336", "1337", "1338", "1339",
    "660", "232", "734", "742", "735", "738", "967", "907", "739", "1109",
    "736", "737", "898", "935", "871", "973", "960", "1239", "740", "741",
        -> Category.Series

    // Programs / Apps
    "503", "504", "506", "763", "1335", "1241", "1023", "717", "509", "508",
    "510", "1254", "1042", "511", "916", "512", "561", "1284", "562", "513",
    "514", "515", "516", "517", "518", "519", "520", "521", "522", "523",
    "524", "532", "533", "535", "530", "529", "525", "526", "527", "545",
    "764", "765", "820", "552", "553", "554", "550", "549", "548",
        -> Category.Apps

    // Movies
    "216", "270", "218", "219", "954", "217", "1293", "1298", "318", "320",
    "677", "1177", "319", "678", "885", "908", "1310", "909", "910", "911",
    "912", "220", "221", "222", "882", "889", "224", "225", "226", "227",
    "1296", "891", "1299", "682", "694", "884", "1211", "693", "913", "228",
    "1150", "1311", "1313", "1312", "256", "257", "258", "883", "955", "905",
    "271", "1210", "264", "265", "272", "1262", "266", "1294",
        -> Category.Movies

    // TV / Series
    "1219", "1221", "1220", "722", "768", "1344", "779", "1288", "787", "1141",
    "777", "786", "776", "785", "775", "1265", "1242", "1140", "782", "773",
    "1142", "772", "771", "783", "1144", "804", "1290", "1300", "784", "774",
    "922", "770", "1320", "780", "781", "1322", "769", "799", "800", "791",
    "793", "794", "796", "795", "713", "706", "577", "894", "578", "580",
    "579", "953", "581", "806", "714", "761", "809", "924", "812", "576",
    "590", "591", "588", "589", "598", "652", "599", "959", "956", "597",
    "593", "594", "819", "595", "587", "584", "586", "585", "600", "596",
    "1295", "614", "603", "1308", "1309", "1206", "1194", "1062", "974",
    "609", "1263", "951", "975", "608", "607", "606", "750", "605", "604",
    "950", "610", "613", "612", "653", "654", "611", "656", "437", "466",
    "1319", "463", "958", "1223", "467", "464", "465", "1348", "469",
        -> Category.Series

    // Anime
    "615", "616", "617", "648", "619", "620", "623", "622", "621", "632",
    "624", "627", "626", "625", "644", "628", "635", "634", "638", "646",
    "645", "639", "640",
        -> Category.Anime

    // Books
    "432", "755", "481", "557", "442", "441", "875", "1176", "444", "443",
    "440", "1199", "433", "447", "445", "817", "818", "434", "1349", "957",
    "931", "1152", "455", "453", "1063", "452", "451", "449", "1153", "1347",
    "482", "483", "484", "1343", "438", "485", "473", "472", "471", "895",
    "470", "896", "480", "439", "477", "476", "475", "474", "886", "478",
    "486", "490", "657", "489", "488", "487", "1198", "1227", "893", "491",
    "767", "299", "887", "301", "1334", "300", "1341", "492", "558", "1173",
    "1174", "1171", "662", "1175", "1172", "933", "815", "1170", "398", "816",
        -> Category.Books

    // Music / Audio
    "313", "1291", "680", "1149", "429", "681", "330", "1256", "1285", "370",
    "1260", "371", "1261", "375", "1259", "374", "1257", "373", "1258", "372",
    "1160", "876", "1255", "376", "326", "1352", "359", "358", "1353", "1188",
    "1189", "328", "1370", "1180", "1181", "364", "363", "1179", "879", "322",
    "1350", "962", "333", "1356", "965", "336", "1362", "337", "338", "1351",
    "963", "334", "1357", "961", "332", "325", "1354", "1355", "1165", "1166",
    "1168", "1167", "1162", "352", "1164", "1163", "1161", "353", "324", "1327",
    "1328", "1325", "1326", "1365", "1366", "1323", "1324", "976", "346", "1363",
    "977", "345", "349", "1243", "347", "979", "673", "671", "1224", "1225",
    "1367", "980", "672", "1316", "1317", "1364", "981", "344", "1368", "983",
    "984", "982", "348", "674", "323", "1369", "1187", "339", "1186", "340",
    "1185", "341", "329", "1361", "369", "368", "1218", "365", "1217", "366",
    "1215", "1216", "1213", "367", "331", "1358", "1157", "711", "1159", "378",
    "1359", "1158", "379", "380", "1178", "1360", "361", "360", "327", "1184",
    "824", "1182", "354", "877", "1183", "1190", "917", "1096", "1097", "1095",
        -> Category.Music

    // Games
    "410", "411", "412", "1008", "415", "746", "428", "1009", "413", "414",
    "1010", "1012", "1014", "416", "1013", "1015", "268", "1016", "1041",
    "1018", "1017", "972", "971", "970", "969", "968", "1146", "418", "1061",
    "1060", "1059", "1058", "1057", "1056", "1054", "1053", "1052", "1051",
    "1050", "1049", "1048", "1047", "1046", "1045", "1044", "382", "390",
    "387", "388", "1264", "1318", "385", "386", "848", "1321", "383", "384",
    "1292", "389", "391", "417", "1193", "1192",
        -> Category.Games

    else -> Category.Other
}