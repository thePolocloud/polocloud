package de.polocloud.addons.proxy.waterfall

import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.core.PlayerCountResolver
import de.polocloud.addons.proxy.core.TablistComposer
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent

/**
 * Waterfall/BungeeCord counterpart of [de.polocloud.addons.proxy.velocity.TablistRenderer] —
 * rebuilds every connected player's tab list header/footer from the current [ProxyConfig] on a
 * repeating tick (see [WaterfallProxyBootstrap]), sharing the same layout/animation rules
 * ([TablistComposer]) but building BungeeCord's `BaseComponent[]` instead of Adventure.
 */
object TablistRenderer {

    fun render(proxy: ProxyServer, config: ProxyConfig) {
        val frames = TablistComposer.resolveFrames(config) ?: return
        val (online, max) = PlayerCountResolver.resolve(config.playerCount, proxy::getOnlineCount) { proxy.config.playerLimit }

        proxy.players.forEach { player ->
            val currentServer = player.server?.info?.name ?: ""
            player.setTabHeader(
                joinLines(frames.header, online, max, player.name, currentServer),
                joinLines(frames.footer, online, max, player.name, currentServer),
            )
        }
    }

    private fun joinLines(lines: List<String>, online: Int, max: Int, player: String, server: String): Array<BaseComponent> {
        val resolved = TablistComposer.resolveLines(lines, online, max, player, server)
        if (resolved.isEmpty()) return TextComponent.fromLegacyText("")

        return TextComponent.fromLegacyText(resolved.joinToString("\n"))
    }
}
