package com.prajwalch.torrentsearch.providers

/** Search providers manager. */
object SearchProviders {
    /** List of providers which are enabled by default. */
    private val ENABLED_BY_DEFAULT = setOf(
        Eztv(id = "p1"),
        Knaben(id = "p2"),
        NyaaSi(id = "p3"),
        TorrentsCsv(id = "p4"),
        Yts(id = "p5"),
    )

    /** List of providers which are disabled by default. */
    private val DISABLED_BY_DEFAULT = setOf(
        AnimeTosho(id = "p6"),
        LimeTorrents(id = "p7"),
        MyPornClub(id = "p8"),
        ThePirateBay(id = "p9"),
        TheRarBg(id = "p10"),
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