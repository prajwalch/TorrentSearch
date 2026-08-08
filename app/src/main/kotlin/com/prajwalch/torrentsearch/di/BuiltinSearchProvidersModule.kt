package com.prajwalch.torrentsearch.di

import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.providers.AniLibria
import com.prajwalch.torrentsearch.providers.AniRena
import com.prajwalch.torrentsearch.providers.AnimeTosho
import com.prajwalch.torrentsearch.providers.AudioBookBay
import com.prajwalch.torrentsearch.providers.BTDigg
import com.prajwalch.torrentsearch.providers.BangumiMoe
import com.prajwalch.torrentsearch.providers.BitSearch
import com.prajwalch.torrentsearch.providers.BlueRoms
import com.prajwalch.torrentsearch.providers.Bt4g
import com.prajwalch.torrentsearch.providers.Btsow
import com.prajwalch.torrentsearch.providers.Dmhy
import com.prajwalch.torrentsearch.providers.Ext
import com.prajwalch.torrentsearch.providers.Eztv
import com.prajwalch.torrentsearch.providers.FileMood
import com.prajwalch.torrentsearch.providers.InternetArchive
import com.prajwalch.torrentsearch.providers.Knaben
import com.prajwalch.torrentsearch.providers.LimeTorrents
import com.prajwalch.torrentsearch.providers.LinuxTracker
import com.prajwalch.torrentsearch.providers.MegaPeer
import com.prajwalch.torrentsearch.providers.Mikan
import com.prajwalch.torrentsearch.providers.MyPornClub
import com.prajwalch.torrentsearch.providers.NekoBT
import com.prajwalch.torrentsearch.providers.Nyaa
import com.prajwalch.torrentsearch.providers.OxTorrent
import com.prajwalch.torrentsearch.providers.Rutor
import com.prajwalch.torrentsearch.providers.SearchProvider
import com.prajwalch.torrentsearch.providers.SubsPlease
import com.prajwalch.torrentsearch.providers.Sukebei
import com.prajwalch.torrentsearch.providers.ThePirateBay
import com.prajwalch.torrentsearch.providers.TheRarBg
import com.prajwalch.torrentsearch.providers.ThirteenThirtySevenX
import com.prajwalch.torrentsearch.providers.TokyoToshokan
import com.prajwalch.torrentsearch.providers.Torrent9
import com.prajwalch.torrentsearch.providers.TorrentDatabase
import com.prajwalch.torrentsearch.providers.TorrentDownload
import com.prajwalch.torrentsearch.providers.TorrentDownloads
import com.prajwalch.torrentsearch.providers.TorrentKitty
import com.prajwalch.torrentsearch.providers.TorrentsCSV
import com.prajwalch.torrentsearch.providers.Torrentz
import com.prajwalch.torrentsearch.providers.UIndex
import com.prajwalch.torrentsearch.providers.XXXClub
import com.prajwalch.torrentsearch.providers.XXXTracker
import com.prajwalch.torrentsearch.providers.Yts
import com.prajwalch.torrentsearch.providers.ZeroMagnet

import org.koin.dsl.module

private fun provideBuiltinSearchProviders(networkClient: NetworkClient): List<SearchProvider> =
    listOf(
        AniLibria(networkClient),
        AniRena(networkClient),
        AnimeTosho(networkClient),
        AudioBookBay(networkClient),
        BTDigg(networkClient),
        BangumiMoe(networkClient),
        BitSearch(networkClient),
        BlueRoms(networkClient),
        Bt4g(networkClient),
        Btsow(networkClient),
        Dmhy(networkClient),
        Ext(networkClient),
        Eztv(networkClient),
        FileMood(networkClient),
        InternetArchive(networkClient),
        Knaben(networkClient),
        LimeTorrents(networkClient),
        LinuxTracker(networkClient),
        MegaPeer(networkClient),
        Mikan(networkClient),
        MyPornClub(networkClient),
        NekoBT(networkClient),
        Nyaa(networkClient),
        OxTorrent(networkClient),
        Rutor(networkClient),
        SubsPlease(networkClient),
        Sukebei(networkClient),
        ThePirateBay(networkClient),
        TheRarBg(networkClient),
        ThirteenThirtySevenX(networkClient),
        TokyoToshokan(networkClient),
        Torrent9(networkClient),
        TorrentDatabase(networkClient),
        TorrentDownload(networkClient),
        TorrentDownloads(networkClient),
        TorrentKitty(networkClient),
        TorrentsCSV(networkClient),
        Torrentz(networkClient),
        UIndex(networkClient),
        XXXClub(networkClient),
        XXXTracker(networkClient),
        Yts(networkClient),
        ZeroMagnet(networkClient),
    )

val builtinSearchProvidersModule = module {
    single<List<SearchProvider>> { provideBuiltinSearchProviders(networkClient = get()) }
}
