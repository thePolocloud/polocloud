package dev.httpmarco.polocloud.platforms

import kotlinx.serialization.Serializable

@Serializable
class PlatformTable(val availableProxies : List<String>, val availableServers : List<String>) {
}
