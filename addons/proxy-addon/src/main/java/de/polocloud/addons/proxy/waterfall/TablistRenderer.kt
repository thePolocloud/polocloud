package de.polocloud.addons.proxy.waterfall

import de.polocloud.addons.proxy.AnimationFrames
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.ProxyPlaceholders
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent

/**
 * Waterfall/BungeeCord counterpart of [de.polocloud.addons.proxy.velocity.TablistRenderer] —
 * rebuilds every connected player's tab list header/footer from the current [ProxyConfig] on a
 * repeating tick (see [WaterfallProxyBootstrap]).
 */
object TablistRenderer {

    fun render(proxy: ProxyServer, config: ProxyConfig) {
        val tablist = config.tablist
        if (!tablist.enabled) return

        val (online, max) = PlayerCountResolver.resolve(proxy, config.playerCount)
        val headerFrame = AnimationFrames.current(tablist.header, tablist.tickIntervalMillis) ?: emptyList()
        val footerFrame = AnimationFrames.current(tablist.footer, tablist.tickIntervalMillis) ?: emptyList()

        proxy.players.forEach { player ->
            val currentServer = player.server?.info?.name ?: ""
            player.setTabHeader(
                joinLines(headerFrame, online, max, player.name, currentServer),
                joinLines(footerFrame, online, max, player.name, currentServer),
            )
        }
    }

    private fun joinLines(lines: List<String>, online: Int, max: Int, player: String, server: String): Array<BaseComponent> {
        if (lines.isEmpty()) return TextComponent.fromLegacyText("")

        val resolved = lines.joinToString("\n") { ProxyPlaceholders.resolve(it, online, max, player, server) }
        return TextComponent.fromLegacyText(resolved)
    }
}
