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
    LinuxTracker(),
    Mikan(),
    MyPornClub(),
    NekoBT(),
    Nyaa(),
    OxTorrent(),
    Rutor(),
    SubsPlease(),
    Sukebei(),
    ThePirateBay(),
    TheRarBg(),
    ThirteenThirtySevenX(),
    TokyoToshokan(),
    Torrent9(),
    TorrentDatabase(),
    TorrentDownload(),
    TorrentDownloads(),
    TorrentKitty(),
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