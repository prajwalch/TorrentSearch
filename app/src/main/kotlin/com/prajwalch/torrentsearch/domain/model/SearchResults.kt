package com.prajwalch.torrentsearch.domain.model

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

data class SearchResults(
    val successes: PersistentList<Torrent> = persistentListOf(),
    val failures: PersistentList<SearchException> = persistentListOf(),
)

typealias PredicatesBuilder = PersistentList.Builder<(Torrent) -> Boolean>.() -> Unit

fun SearchResults.filterSuccesses(
    predicatesBuilder: PredicatesBuilder,
): SearchResults = with(this) {
    copy(successes = successes.filter(predicatesBuilder))
}

fun SearchResults.sortSuccessesWith(comparator: Comparator<Torrent>): SearchResults =
    with(this) { copy(successes = successes.sortedWithComparator(comparator)) }

fun SearchResults.addResult(result: Result<List<Torrent>>): SearchResults =
    result.fold(
        onSuccess = { this.addTorrents(it) },
        onFailure = { this.addFailure(it as SearchException) },
    )

private fun SearchResults.addTorrents(torrents: List<Torrent>): SearchResults =
    with(this) { copy(successes = successes.addAll(torrents)) }

private fun SearchResults.addFailure(failure: SearchException): SearchResults =
    with(this) { copy(failures = failures.add(failure)) }

fun PersistentList<Torrent>.filter(
    predicatesBuilder: PredicatesBuilder,
): PersistentList<Torrent> {
    val predicates = persistentListOf<(Torrent) -> Boolean>()
        .builder()
        .apply(predicatesBuilder)
        .build()

    if (predicates.isEmpty()) return this

    return this.mutate { torrents ->
        torrents.retainAll { torrent ->
            predicates.all { predicate -> predicate(torrent) }
        }
    }
}

fun PersistentList<Torrent>.sortedWithComparator(
    comparator: Comparator<Torrent>,
): PersistentList<Torrent> =
    this.mutate { it.sortWith(comparator) }