package com.prajwalch.torrentsearch.domain.models

import kotlinx.collections.immutable.ImmutableList

data class SearchResults(
    val successes: ImmutableList<Torrent>,
    val failures: ImmutableList<SearchException>,
)