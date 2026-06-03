package com.prajwalch.torrentsearch.providers

val BuiltinSearchProviders = listOf(
    AniRena(),
    AnimeTosho(),
    BitSearch(),
    Dmhy(),
    Eztv(),
    FileMood(),
    InternetArchive(),
    Knaben(),
    LimeTorrents(),
    MyPornClub(),
    NekoBT(),
    Nyaa(),
    SubsPlease(),
    Sukebei(),
    ThirteenThirtySevenX(),
    ThePirateBay(),
    TheRarBg(),
    TokyoToshokan(),
    TorrentDatabase(),
    TorrentDownload(),
    TorrentDownloads(),
    TorrentsCSV(),
    UIndex(),
    XXXClub(),
    XXXTracker(),
    Yts(),
)

val DefaultEnabledProviderIds = BuiltinSearchProviders
    .filter { it.enabledByDefault }
    .map { it.id }
    .toSet()