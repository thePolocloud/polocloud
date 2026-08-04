package de.polocloud.addons.proxy.waterfall

import de.polocloud.addons.proxy.PlayerCountConfig
import de.polocloud.addons.proxy.PlayerCountMode
import de.polocloud.api.Polocloud
import de.polocloud.shared.service.ServiceState
import net.md_5.bungee.api.ProxyServer

/**
 * Waterfall/BungeeCord counterpart of [de.polocloud.addons.proxy.velocity.PlayerCountResolver] —
 * same semantics per [PlayerCountMode], just resolved off the Bungee [ProxyServer] instead of
 * Velocity's.
 */
object PlayerCountResolver {

    private const val DEFAULT_LOCAL_MAX = 100

    fun resolve(proxy: ProxyServer, config: PlayerCountConfig): Pair<Int, Int> = when (config.mode) {
        PlayerCountMode.FIXED -> config.fixedOnline to config.fixedMax
        PlayerCountMode.LOCAL -> {
            val online = proxy.onlineCount
            val max = config.maxOverride.takeIf { it > 0 } ?: DEFAULT_LOCAL_MAX
            online to max
        }
        PlayerCountMode.NETWORK -> {
            val services = Polocloud.serviceService.findAll().filter { it.state == ServiceState.RUNNING }
            val online = services.sumOf { it.onlinePlayers }
            val max = config.maxOverride.takeIf { it > 0 } ?: services.sumOf { it.maxPlayers }
            online to max
        }
    }
}
