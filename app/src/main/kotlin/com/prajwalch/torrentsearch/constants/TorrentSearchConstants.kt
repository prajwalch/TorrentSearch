package com.prajwalch.torrentsearch.constants

import com.prajwalch.torrentsearch.BuildConfig

object TorrentSearchConstants {
    /** Github repository URL. */
    const val GITHUB_REPO_URL = "https://github.com/prajwalch/TorrentSearch"

    /** Github latest release page URL. */
    const val GITHUB_RELEASE_URL = "$GITHUB_REPO_URL/releases/tag/v${BuildConfig.VERSION_NAME}"

    /** Github wiki URL.*/
    const val GITHUB_WIKI_URL = "$GITHUB_REPO_URL/wiki"

    /** Torzanb search provider configuration guide wiki URL. */
    const val TORZNAB_HOW_TO_ADD_WIKI =
        "$GITHUB_WIKI_URL/How-to-add-and-configure-Torznab-search-provider"

    const val CRASH_LOGS_FILE_NAME = "torrentsearch_crash_logs.txt"
    const val CRASH_LOGS_FILE_TYPE = "text/plain"

    const val SEARCH_ERROR_LOGS_FILE_NAME = "torrentsearch_search_error_logs.txt"
    const val SEARCH_ERROR_LOGS_FILE_TYPE = "text/plain"
}