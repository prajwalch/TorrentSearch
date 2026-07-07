package com.prajwalch.torrentsearch.domain.model

import com.prajwalch.torrentsearch.filter.TorrentFilter

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

typealias FiltersBuilder = MutableList<TorrentFilter>.() -> Unit

data class SearchResults(
    val torrents: PersistentList<Torrent> = persistentListOf(),
    val errors: PersistentList<SearchProviderError> = persistentListOf(),
) {
    fun filterTorrents(filtersBuilder: FiltersBuilder): SearchResults =
        copy(torrents = torrents.filterIfAll(filtersBuilder))

    fun takeNTorrents(count: Int): SearchResults =
        copy(torrents = torrents.takeN(count))

    fun sortTorrentsWith(comparator: Comparator<Torrent>): SearchResults =
        copy(torrents = torrents.sortedWithComparator(comparator))

    fun addResult(result: SearchProviderResult<List<Torrent>>): SearchResults =
        result.fold(onSuccess = { addTorrents(it) }, onError = { addError(it) })

    private fun addTorrents(torrents: List<Torrent>): SearchResults =
        copy(torrents = this.torrents.addingAll(torrents))

    private fun addError(error: SearchProviderError): SearchResults =
        copy(errors = errors.adding(error))
}

fun PersistentList<Torrent>.filterIfAll(filtersBuilder: FiltersBuilder): PersistentList<Torrent> {
    val predicates = buildList(filtersBuilder)
    if (predicates.isEmpty()) return this

    return this.mutate { torrents ->
        torrents.retainAll { torrent ->
            predicates.all { predicate -> predicate(torrent) }
        }
    }
}

fun PersistentList<Torrent>.takeN(count: Int): PersistentList<Torrent> =
    this.mutate { torrents -> torrents.subList(count, torrents.size).clear() }

fun PersistentList<Torrent>.sortedWithComparator(
    comparator: Comparator<Torrent>,
): PersistentList<Torrent> = this.mutate { it.sortWith(comparator) }