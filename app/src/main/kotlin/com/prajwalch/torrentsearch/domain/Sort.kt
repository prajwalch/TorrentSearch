package com.prajwalch.torrentsearch.domain

import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.prettySizeToBytes

/** Results sort criteria. */
enum class SortCriteria {
    Name,
    Seeders,
    Peers,
    FileSize {
        override fun toString() = "File size"
    },
    Date;

    companion object {
        /** The default criteria. */
        val DEFAULT = Seeders
    }
}

/** Results sort order. */
enum class SortOrder {
    Ascending,
    Descending;

    /** Returns the opposite order. */
    fun opposite() = when (this) {
        Ascending -> Descending
        Descending -> Ascending
    }

    companion object {
        /** The default sort order. */
        val DEFAULT = Descending
    }
}

/** Sorts the given list of torrent based on the given criteria and order. */
class SortTorrentsUseCase(
    private val torrents: List<Torrent>,
    private val criteria: SortCriteria,
    private val order: SortOrder,
) {
    operator fun invoke(): List<Torrent> {
        val sortedResults = when (criteria) {
            SortCriteria.Name -> torrents.sortedBy { it.name }
            SortCriteria.Seeders -> torrents.sortedBy { it.seeders }
            SortCriteria.Peers -> torrents.sortedBy { it.peers }
            SortCriteria.FileSize -> torrents.sortedBy { prettySizeToBytes(it.size) }
            // FIXME: Sorting by date needs some fixes.
            SortCriteria.Date -> torrents.sortedBy { it.uploadDate }
        }

        return if (order == SortOrder.Ascending) sortedResults else sortedResults.reversed()
    }
}