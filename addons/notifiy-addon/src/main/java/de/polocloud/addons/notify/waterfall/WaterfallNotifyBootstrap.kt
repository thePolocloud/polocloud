package de.polocloud.addons.notify.waterfall

import de.polocloud.addons.notify.NotifyAddon
import de.polocloud.api.Polocloud
import de.polocloud.api.event.subscribe
import de.polocloud.shared.event.server.ServerStoppedEvent
import de.polocloud.shared.event.server.ServiceOnlineEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Plugin

/**
 * Waterfall/BungeeCord counterpart of
 * [de.polocloud.addons.notify.velocity.VelocityNotifyBootstrap] — same service-lifecycle
 * broadcast behaviour, wired up against the Bungee plugin API. Registered via `bungee.yml`'s
 * `main`, so — unlike Velocity — no plugin annotation is needed. [NotifyAddon] is a class and
 * Bungee plugins must themselves extend [Plugin], so the platform hooks are implemented on a
 * private delegate here instead of by extending [NotifyAddon] directly.
 */
class WaterfallNotifyBootstrap : Plugin() {

    private val notify = object : NotifyAddon<ProxiedPlayer>() {
        override fun onlinePlayers(): Collection<ProxiedPlayer> = proxy.players

        override fun hasPermission(player: ProxiedPlayer, permission: String): Boolean = player.hasPermission(permission)

        override fun sendMessage(player: ProxiedPlayer, message: String) {
            player.sendMessage(*TextComponent.fromLegacyText(message))
        }
    }

    override fun onEnable() {
        Polocloud.eventService.subscribe<ServiceOnlineEvent> { notify.notifyStarted(it.service) }
        Polocloud.eventService.subscribe<ServerStoppedEvent> { notify.notifyStopped(it.service) }
        logger.info("Notify addon is now listening for service lifecycle events")
    }
}
