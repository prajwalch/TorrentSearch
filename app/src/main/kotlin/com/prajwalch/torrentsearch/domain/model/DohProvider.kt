package com.prajwalch.torrentsearch.domain.model

enum class DohProvider(val id: String) {
    Default("_default_"),
    Cloudflare("cloudflare"),
    NextDNS("nextdns"),
    Google("google"),
    AdGuard("adguard"),
    Quad9("quad9"),
    AliDNS("alidns"),
    DNSPod("dnspod"),
    ThreeSixty("360"),
    Quad101("quad101"),
    Mullvad("mullvad"),
    ControlD("controld"),
    Njalla("njalla"),
    Shecan("shecan");

    companion object {
        fun fromId(id: String): DohProvider = entries.find { it.id == id } ?: Default
    }
}