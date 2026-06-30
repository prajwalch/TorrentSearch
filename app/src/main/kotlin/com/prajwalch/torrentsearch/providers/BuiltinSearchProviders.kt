package com.prajwalch.torrentsearch.providers

val BuiltinSearchProviders = listOf(
    AniLibria(),
    AniRena(),
    AnimeTosho(),
    AudioBookBay(),
    BangumiMoe(),
    BitSearch(),
    BTDigg(),
    Dmhy(),
    Ext(),
    Eztv(),
    FileMood(),
    InternetArchive(),
    Knaben(),
    LimeTorrents(),
    LinuxTracker(),
    MegaPeer(),
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
    ZeroMagnet(),
)

val DefaultEnabledProviderIds = BuiltinSearchProviders
    .filter { it.enabledByDefault }
    .map { it.id }
    .toSet()