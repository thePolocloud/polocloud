package de.polocloud.addons.proxy.velocity

import com.velocitypowered.api.proxy.ProxyServer
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.core.PlayerCountResolver
import de.polocloud.addons.proxy.core.TablistComposer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Rebuilds every connected player's tab list header/footer from the current [ProxyConfig].
 * Called on a repeating tick (see [VelocityProxyBootstrap]) rather than only on join, so
 * animated frames actually cycle and `%online%`/`%max%` stay live. The layout/animation rules
 * live in [TablistComposer]; this only turns the resolved lines into Adventure [Component]s.
 */
object TablistRenderer {

    private val legacy = LegacyComponentSerializer.legacySection()

    fun render(server: ProxyServer, config: ProxyConfig) {
        val frames = TablistComposer.resolveFrames(config) ?: return
        val (online, max) = PlayerCountResolver.resolve(config.playerCount, server::getPlayerCount) { server.configuration.showMaxPlayers }

        server.allPlayers.forEach { player ->
            val currentServer = player.currentServer.orElse(null)?.serverInfo?.name ?: ""
            // Player.getTabList().setHeaderAndFooter(...) is deprecated in favour of this
            // Audience method (Player implements Audience via CommandSource).
            player.sendPlayerListHeaderAndFooter(
                joinLines(frames.header, online, max, player.username, currentServer),
                joinLines(frames.footer, online, max, player.username, currentServer),
            )
        }
    }

    private fun joinLines(lines: List<String>, online: Int, max: Int, player: String, server: String): Component {
        val resolved = TablistComposer.resolveLines(lines, online, max, player, server)
        if (resolved.isEmpty()) return Component.empty()

        return resolved
            .map(legacy::deserialize)
            .reduce { acc, line -> acc.append(Component.newline()).append(line) }
    }
}
