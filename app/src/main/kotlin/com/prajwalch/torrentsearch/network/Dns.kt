package com.prajwalch.torrentsearch.network

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.model.DohProvider

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

import okhttp3.Dns
import okhttp3.OkHttpClient

import java.net.InetAddress

class DynamicDns(private val settingsRepository: SettingsRepository) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val dohProvider = runBlocking { settingsRepository.dohProvider.firstOrNull() }
        return createDns(dohProvider).lookup(hostname)
    }

    private fun createDns(dohProvider: DohProvider?): Dns {
        val bootstrapClient = OkHttpClient.Builder().build()
        return when (dohProvider) {
            DohProvider.Default, null -> Dns.SYSTEM
            DohProvider.Cloudflare -> DohProviders.buildCloudflare(bootstrapClient)
            DohProvider.NextDNS -> DohProviders.buildNextDNS(bootstrapClient)
            DohProvider.Google -> DohProviders.buildGoogle(bootstrapClient)
            DohProvider.AdGuard -> DohProviders.buildAdGuard(bootstrapClient)
            DohProvider.Quad9 -> DohProviders.buildQuad9(bootstrapClient)
            DohProvider.AliDNS -> DohProviders.buildAliDNS(bootstrapClient)
            DohProvider.DNSPod -> DohProviders.buildDNSPod(bootstrapClient)
            DohProvider.ThreeSixty -> DohProviders.build360(bootstrapClient)
            DohProvider.Quad101 -> DohProviders.buildQuad101(bootstrapClient)
            DohProvider.Mullvad -> DohProviders.buildMullvad(bootstrapClient)
            DohProvider.ControlD -> DohProviders.buildControlD(bootstrapClient)
            DohProvider.Njalla -> DohProviders.buildNajalla(bootstrapClient)
            DohProvider.Shecan -> DohProviders.buildShecan(bootstrapClient)
        }
    }
}