package com.prajwalch.torrentsearch.domain

import com.prajwalch.torrentsearch.models.Torrent

/** Filters the NSFW torrents from the given list and returns a new list. */
class FilterNSFWTorrentsUseCase(private val torrents: List<Torrent>) {
    operator fun invoke(): List<Torrent> {
        return torrents.filter { torrent ->
            // Torrent with no category is also NSFW.
            val categoryIsNullOrNSFW = torrent.category?.isNSFW ?: true
            !categoryIsNullOrNSFW
        }
    }
}