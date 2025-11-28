package com.prajwalch.torrentsearch.extensions

import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOrder
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.createSortComparator

/** Sorts the torrents based on the given criteria and order. */
fun List<Torrent>.customSort(criteria: SortCriteria, order: SortOrder) =
    this.sortedWith(comparator = createSortComparator(criteria = criteria, order = order))