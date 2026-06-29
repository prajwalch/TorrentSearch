package com.prajwalch.torrentsearch.constant

import com.prajwalch.torrentsearch.BuildConfig

object TorrentSearchConstants {
    /** GitHub repository URL. */
    const val GITHUB_REPO_URL = "https://github.com/prajwalch/TorrentSearch"

    /** GitHub latest release page URL. */
    const val GITHUB_RELEASE_URL = "$GITHUB_REPO_URL/releases/tag/v${BuildConfig.VERSION_NAME}"

    /** GitHub wiki URL.*/
    const val GITHUB_WIKI_URL = "$GITHUB_REPO_URL/wiki"

    /** Torzanb search provider configuration guide wiki URL. */
    const val TORZNAB_HOW_TO_ADD_WIKI =
        "$GITHUB_WIKI_URL/How-to-add-and-configure-Torznab-search-provider"

    /** MIME type of logs file. */
    const val LOGS_FILE_TYPE = "text/plain"

    /** Name of the export file. */
    const val BOOKMARKS_EXPORT_FILE_NAME = "bookmarks.json"

    /** Export file type. */
    const val BOOKMARKS_EXPORT_FILE_TYPE = "application/json"

    /** Name of a crash logs file. */
    const val CRASH_LOGS_FILE_NAME = "torrentsearch_crash_logs.txt"

    /** Name of an application main logs file. */
    const val APP_LOGS_FILE_NAME = "torrentsearch_logs.txt"

    /** MIME type of torrent file. */
    const val MIME_TYPE_TORRENT = "application/x-bittorrent"
}