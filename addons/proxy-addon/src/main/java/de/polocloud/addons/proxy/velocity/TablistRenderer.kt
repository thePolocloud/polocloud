package de.polocloud.addons.proxy.velocity

import com.velocitypowered.api.proxy.ProxyServer
import de.polocloud.addons.proxy.AnimationFrames
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.ProxyPlaceholders
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Rebuilds every connected player's tab list header/footer from the current [ProxyConfig].
 * Called on a repeating tick (see [VelocityProxyBootstrap]) rather than only on join, so
 * animated frames actually cycle and `%online%`/`%max%` stay live.
 */
object TablistRenderer {

    private val legacy = LegacyComponentSerializer.legacySection()

    fun render(server: ProxyServer, config: ProxyConfig) {
        val tablist = config.tablist
        if (!tablist.enabled) return

        val (online, max) = PlayerCountResolver.resolve(server, config.playerCount)
        val headerFrame = AnimationFrames.current(tablist.header, tablist.tickIntervalMillis) ?: emptyList()
        val footerFrame = AnimationFrames.current(tablist.footer, tablist.tickIntervalMillis) ?: emptyList()

        server.allPlayers.forEach { player ->
            val currentServer = player.currentServer.orElse(null)?.serverInfo?.name ?: ""
            // Player.getTabList().setHeaderAndFooter(...) is deprecated in favour of this
            // Audience method (Player implements Audience via CommandSource).
            player.sendPlayerListHeaderAndFooter(
                joinLines(headerFrame, online, max, player.username, currentServer),
                joinLines(footerFrame, online, max, player.username, currentServer),
            )
        }
    }

    private fun joinLines(lines: List<String>, online: Int, max: Int, player: String, server: String): Component {
        if (lines.isEmpty()) return Component.empty()

        return lines
            .map { legacy.deserialize(ProxyPlaceholders.resolve(it, online, max, player, server)) }
            .reduce { acc, line -> acc.append(Component.newline()).append(line) }
    }
}
