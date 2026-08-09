package de.polocloud.addons.notify.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import de.polocloud.addons.notify.NotifyAddon
import de.polocloud.api.Polocloud
import de.polocloud.api.event.subscribe
import de.polocloud.shared.event.server.ServerStoppedEvent
import de.polocloud.shared.event.server.ServiceOnlineEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.slf4j.Logger

/**
 * Velocity entry point for the notify addon. Subscribes to the cluster's service lifecycle
 * events and broadcasts a message to every online player holding
 * [de.polocloud.addons.notify.NOTIFY_MANAGED_PERMISSION] whenever a service comes online or
 * stops (mirrors how [de.polocloud.addons.hub.velocity.VelocityHubBootstrap] wraps
 * [de.polocloud.addons.hub.HubAddon]).
 */
@Plugin(
    id = "polocloud-notify-addon",
    name = "Polocloud Notify Addon Plugin",
    version = "3.0.0",
    description = "Notifies players with a permission when a service starts, comes online or stops",
    authors = ["polocloud"],
)
class VelocityNotifyBootstrap @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
) : NotifyAddon<Player>() {

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        Polocloud.eventService.subscribe<ServiceOnlineEvent> { notifyStarted(it.service) }
        Polocloud.eventService.subscribe<ServerStoppedEvent> { notifyStopped(it.service) }
        logger.info("Notify addon is now listening for service lifecycle events")
    }

    override fun onlinePlayers(): Collection<Player> = server.allPlayers

    override fun hasPermission(player: Player, permission: String): Boolean = player.hasPermission(permission)

    override fun sendMessage(player: Player, message: String) {
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message))
    }
}
