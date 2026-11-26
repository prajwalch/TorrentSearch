package com.prajwalch.torrentsearch.filter

import com.prajwalch.torrentsearch.domain.model.Category

import java.text.Normalizer

object TorrentFilters {
    private val WHITESPACE = Regex("\\s+")
    private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val APOSTROPHES = Regex("['’]")
    private val WORD_SEPARATORS = Regex("[._-]")
    private val SEASON_EPISODE_WORD = Regex("s([0-9]+)e([0-9]+)")
    private val SEASON_X_EPISODE_WORD = Regex("([0-9]+)x([0-9]+)")

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
     *
     * An episode word such as `S02E01` or `2x01` also matches other
     * notations of the same episode (see [episodeNotationRegex]).
     */
    fun matchesAllWords(query: String): TorrentFilter {
        val wordMatchers = query
            .split(WHITESPACE)
            .map(::normalizeForMatching)
            .filter { it.isNotEmpty() }
            .map(::wordMatcher)

        return { torrent ->
            val name = normalizeForMatching(torrent.name)
            wordMatchers.all { matcher -> matcher(name) }
        }
    }

    /**
     * Returns a check that a normalized name contains the given normalized
     * word or, for an episode word, an equivalent episode notation.
     */
    private fun wordMatcher(word: String): (String) -> Boolean {
        val episodeNotation = episodeNotationRegex(word)
            ?: return { name -> name.contains(word) }

        return { name -> name.contains(word) || episodeNotation.containsMatchIn(name) }
    }

    /**
     * If the given normalized word is an episode word (`s02e01`, `2x01`),
     * returns a regex that finds the same season and episode in a normalized
     * name written as `s2e1`, `s02 e01`, `2x01` or `s2 01`, with any zero
     * padding. Otherwise returns `null`.
     *
     * Numbers are compared as digit strings without leading zeros, so very
     * long numbers cannot overflow. The match must not start right after a
     * letter or digit, nor end right before a digit, so `s02e01` does not
     * find `s12e01` or `s02e10`.
     */
    private fun episodeNotationRegex(word: String): Regex? {
        val match = SEASON_EPISODE_WORD.matchEntire(word)
            ?: SEASON_X_EPISODE_WORD.matchEntire(word)
            ?: return null

        val (season, episode) = match.destructured
        val seasonPattern = zeroPaddedPattern(season)
        val episodePattern = zeroPaddedPattern(episode)

        return Regex(
            "(?<![\\p{L}\\p{N}])" +
                    "(?:s$seasonPattern(?: ?e| )|${seasonPattern}x)" +
                    "$episodePattern(?!\\p{N})"
        )
    }

    /** Matches the given ASCII digits as the same number with any zero padding. */
    private fun zeroPaddedPattern(digits: String): String =
        "0*" + digits.trimStart('0').ifEmpty { "0" }

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
