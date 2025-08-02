package com.prajwalch.torrentsearch.providers

/** Search providers manager. */
object SearchProviders {
    /** List of providers which are enabled by default. */
    private val ENABLED_BY_DEFAULT = setOf(
        Eztv(),
        Knaben(),
        Nyaa(),
        TorrentsCsv(),
        Yts(),
    )

    /** List of providers which are disabled by default. */
    private val DISABLED_BY_DEFAULT = setOf(
        AnimeTosho(),
        LimeTorrents(),
        MyPornClub(),
        ThePirateBay(),
        TheRarBg(),
        UIndex(),
    )

    /** List of all providers. */
    private val ALL = ENABLED_BY_DEFAULT.plus(DISABLED_BY_DEFAULT).sortedBy { it.info.name }

    /**
     * Returns a list containing instance of search provider associated with
     * the given ids.
     */
    fun get(ids: Set<SearchProviderId>): List<SearchProvider> {
        return ALL.filter { ids.contains(it.info.id) }
    }

    /**
     * Returns a list containing the id of search providers which are enabled
     * by default.
     */
    fun enabledIds(): Set<SearchProviderId> {
        return ENABLED_BY_DEFAULT.map { it.info.id }.toSet()
    }

    /** Returns a list containing info of all search providers. */
    fun infos(): List<SearchProviderInfo> {
        return ALL.map { it.info }
    }
}