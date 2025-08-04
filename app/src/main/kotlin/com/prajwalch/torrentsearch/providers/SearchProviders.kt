package com.prajwalch.torrentsearch.providers

/** Search providers manager. */
object SearchProviders {
    /**
     * Search provider instances.
     *
     * Make sure they're sorted.
     */
    private val all = setOf(
        AnimeTosho(),
        Eztv(),
        Knaben(),
        LimeTorrents(),
        MyPornClub(),
        Nyaa(),
        Sukebei(),
        ThePirateBay(),
        TheRarBg(),
        TokyoToshokan(),
        TorrentsCsv(),
        UIndex(),
        Yts(),
    )

    /** Returns a list containing the info of all search providers. */
    fun allInfo(): List<SearchProviderInfo> = all.map { it.info }

    /** Returns the count of search providers. */
    fun count(): Int = all.size

    /**
     * Returns a set containing the ID of search providers that are enabled
     * by default.
     */
    fun defaultEnabledIds(): Set<SearchProviderId> =
        all.filter { it.info.enabledByDefault }.map { it.info.id }.toSet()

    /**
     * Returns a list of search providers matching the specified IDs.
     */
    fun findByIds(ids: Set<SearchProviderId>): List<SearchProvider> =
        all.filter { it.info.id in ids }
}