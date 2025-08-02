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
        ThePirateBay(),
        TheRarBg(),
        TorrentsCsv(),
        UIndex(),
        Yts(),
    )

    /**
     * Returns a list containing instance of search provider associated with
     * the given ids.
     */
    fun get(ids: Set<SearchProviderId>): List<SearchProvider> {
        return all.filter { ids.contains(it.info.id) }
    }

    /**
     * Returns a list containing the id of search providers which are enabled
     * by default.
     */
    fun enabledIds(): Set<SearchProviderId> {
        return all.filter { it.info.enabled }.map { it.info.id }.toSet()
    }

    /** Returns a list containing info of all search providers. */
    fun infos(): List<SearchProviderInfo> {
        return all.map { it.info }
    }
}