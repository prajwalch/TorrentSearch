package com.prajwalch.torrentsearch.torznab

import com.prajwalch.torrentsearch.domain.model.Category

object TorznabConstants {
    /**
     * Search provider (or indexer) custom category range start.
     *
     * Category IDs are divided into two groups i.e. predefined and custom.
     * Every client is required to use predefined category IDs whereas custom
     * varies for each indexer, and it also depends on the client implementation.
     *
     * Therefore, we only use predefined category range (**1000-8999**) to construct
     * category IDs from the given [Category] during request and to detect the
     * [Category] of a torrent using IDs available in the response we receive.
     *
     * See [API spec](https://torznab.github.io/spec-1.3-draft/external/newznab/api.html#predefined-categories).
     */
    const val CUSTOM_CATEGORY_RANGE_START = 100000
}