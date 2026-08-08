package de.polocloud.addons.hub.waterfall

import de.polocloud.addons.hub.HubAddon
import de.polocloud.addons.hub.waterfall.command.HubCommand
import de.polocloud.shared.service.Service
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Plugin

/**
 * Waterfall/BungeeCord counterpart of
 * [de.polocloud.addons.hub.velocity.VelocityHubBootstrap] — same `/hub` (aliases `/lobby`,
 * `/l`) fallback routing, wired up against the Bungee plugin API. Registered via `bungee.yml`'s
 * `main`, so — unlike Velocity — no plugin annotation is needed. [HubAddon] is a class and
 * Bungee plugins must themselves extend [Plugin], so the platform hooks are implemented on a
 * private delegate here instead of by extending [HubAddon] directly.
 */
class WaterfallHubBootstrap : Plugin() {

    private val hub = object : HubAddon<ProxiedPlayer>() {
        override fun currentServerName(player: ProxiedPlayer): String? = player.server?.info?.name

        override fun connect(player: ProxiedPlayer, service: Service): Boolean {
            val target = proxy.getServerInfo(service.name()) ?: return false
            player.connect(target)
            return true
        }

        override fun sendMessage(player: ProxiedPlayer, message: String) {
            player.sendMessage(*TextComponent.fromLegacyText("§c$message"))
        }
    }

    override fun onEnable() {
        proxy.pluginManager.registerCommand(this, HubCommand(hub))
        logger.info("Registered /hub command (aliases: lobby, l)")
    }
}
