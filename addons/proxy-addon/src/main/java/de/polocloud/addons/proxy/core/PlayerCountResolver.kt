package de.polocloud.addons.proxy.core

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
 *
 * Platform-agnostic: [PlayerCountMode.LOCAL] is the only mode that needs anything from the
 * actual proxy implementation, so [resolve] takes it as two lazily-evaluated callbacks
 * ([localOnline]/[localConfiguredMax]) instead of a Velocity/Waterfall `ProxyServer` — letting
 * both platforms' thin `PlayerCountResolver` wrappers share this one implementation instead of
 * maintaining two structurally identical copies.
 */
object PlayerCountResolver {

    /** Last-resort fallback maximum for [PlayerCountMode.LOCAL], only used when neither [PlayerCountConfig.maxOverride] nor [localConfiguredMax] report a usable (> 0) limit. */
    private const val DEFAULT_LOCAL_MAX = 100

    fun resolve(config: PlayerCountConfig, localOnline: () -> Int, localConfiguredMax: () -> Int): Pair<Int, Int> = when (config.mode) {
        PlayerCountMode.FIXED -> config.fixedOnline to config.fixedMax
        PlayerCountMode.LOCAL -> {
            val online = localOnline()
            val max = config.maxOverride.takeIf { it > 0 }
                ?: localConfiguredMax().takeIf { it > 0 }
                ?: DEFAULT_LOCAL_MAX
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
