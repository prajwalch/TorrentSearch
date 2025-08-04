package com.prajwalch.torrentsearch.extensions

import com.prajwalch.torrentsearch.data.SortCriteria
import com.prajwalch.torrentsearch.data.SortOrder
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.prettySizeToBytes

/** Sorts the torrents based on the given criteria and order. */
fun List<Torrent>.customSort(criteria: SortCriteria, order: SortOrder): List<Torrent> {
    val comparator: Comparator<Torrent> = when (criteria) {
        SortCriteria.Name -> compareBy { it.name }
        SortCriteria.Seeders -> compareBy { it.seeders }
        SortCriteria.Peers -> compareBy { it.peers }
        SortCriteria.FileSize -> compareBy { prettySizeToBytes(it.size) }
        SortCriteria.Date -> compareBy { it.uploadDate }
    }

    return if (order == SortOrder.Ascending) {
        sortedWith(comparator)
    } else {
        sortedWith(comparator.reversed())
    }
}

/**
 * Filters the NSFW torrents if the NSFW mode is not enabled otherwise returns
 * as is.
 */
fun List<Torrent>.filterNSFW(isNSFWModeEnabled: Boolean) =
    if (isNSFWModeEnabled) {
        this
    } else {
        filter { !it.isNSFW() }
    }