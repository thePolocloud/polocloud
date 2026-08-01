package de.polocloud.addons.proxy.velocity

import com.velocitypowered.api.proxy.ProxyServer
import de.polocloud.addons.proxy.PlayerCountConfig
import de.polocloud.addons.proxy.PlayerCountMode
import de.polocloud.api.Polocloud
import de.polocloud.shared.service.ServiceState

/**
 * Computes the online/max numbers `%online%`/`%max%` resolve to, per [PlayerCountConfig.mode].
 * [PlayerCountMode.NETWORK] sums every currently running service's live player count across the
 * whole cluster via [Polocloud.serviceService] — every proxy running the same config therefore
 * reports the same cluster-wide total, satisfying a single, network-wide player count instead
 * of each proxy only ever showing its own locally-connected players.
 */
object PlayerCountResolver {

    /** Sane fallback maximum for [PlayerCountMode.LOCAL] when no [PlayerCountConfig.maxOverride] is configured — Velocity itself has no inherent "capacity" concept to derive one from. */
    private const val DEFAULT_LOCAL_MAX = 100

    fun resolve(server: ProxyServer, config: PlayerCountConfig): Pair<Int, Int> = when (config.mode) {
        PlayerCountMode.FIXED -> config.fixedOnline to config.fixedMax
        PlayerCountMode.LOCAL -> {
            val online = server.playerCount
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
