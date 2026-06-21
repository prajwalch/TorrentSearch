package com.prajwalch.torrentsearch.providers

val BuiltinSearchProviders = listOf(
    AniRena(),
    AnimeTosho(),
    AudioBookBay(),
    BitSearch(),
    Dmhy(),
    Ext(),
    Eztv(),
    FileMood(),
    InternetArchive(),
    Knaben(),
    LimeTorrents(),
    MyPornClub(),
    NekoBT(),
    Nyaa(),
    Rutor(),
    SubsPlease(),
    Sukebei(),
    ThePirateBay(),
    TheRarBg(),
    ThirteenThirtySevenX(),
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