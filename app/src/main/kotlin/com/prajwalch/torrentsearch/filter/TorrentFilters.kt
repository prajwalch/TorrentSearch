package com.prajwalch.torrentsearch.filter

import com.prajwalch.torrentsearch.domain.model.Category

import java.text.Normalizer

object TorrentFilters {
    private val WHITESPACE = Regex("\\s+")
    private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val APOSTROPHES = Regex("['’]")
    private val WORD_SEPARATORS = Regex("[._-]")

    fun isSfw(): TorrentFilter = { torrent -> !torrent.isNSFW }

    fun isAlive(): TorrentFilter = { torrent -> !torrent.isDead }

    fun notExcludedProvider(excludedProviders: Set<String>): TorrentFilter =
        { torrent -> torrent.providerName !in excludedProviders }

    fun notViewed(viewedTorrentIds: Set<String>): TorrentFilter =
        { torrent -> torrent.id !in viewedTorrentIds }

    fun matchesCategory(category: Category): TorrentFilter =
        { torrent -> category == torrent.category }

    fun matchesQuery(query: String): TorrentFilter {
        val words = query.split(' ', ignoreCase = true).filter { it.isNotBlank() }

        return { torrent ->
            words.any { word -> torrent.name.contains(word, ignoreCase = true) }
        }
    }

    /**
     * Keeps torrents whose name contains every word of the given query.
     *
     * The query is split on whitespace first, then each word and the name
     * are normalized the same way (see [normalizeForMatching]). Because of
     * that order, a word like `F.B.I` must appear as the phrase `f b i`.
     * A query without any usable word keeps every torrent.
     */
    fun matchesAllWords(query: String): TorrentFilter {
        val words = query
            .split(WHITESPACE)
            .map(::normalizeForMatching)
            .filter { it.isNotEmpty() }

        return { torrent ->
            val name = normalizeForMatching(torrent.name)
            words.all { word -> name.contains(word) }
        }
    }

    /**
     * Lowercases the given text, strips accents and apostrophes, treats
     * `.`, `_` and `-` as spaces, and collapses runs of whitespace.
     */
    private fun normalizeForMatching(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace(APOSTROPHES, "")
            .replace(WORD_SEPARATORS, " ")
            .replace(WHITESPACE, " ")
            .trim()
}
