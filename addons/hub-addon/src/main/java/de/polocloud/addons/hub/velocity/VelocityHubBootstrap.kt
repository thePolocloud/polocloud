package de.polocloud.addons.hub.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import de.polocloud.addons.hub.HubAddon
import de.polocloud.addons.hub.velocity.command.HubCommand
import de.polocloud.shared.service.Service
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.slf4j.Logger

/**
 * Velocity entry point for the hub addon. Registers `/hub` (aliases `/lobby`, `/l`) and
 * implements [HubAddon]'s platform hooks directly — mirrors how
 * [de.polocloud.bridge.velocity.VelocityBridgePlugin] extends `BridgeInstance` itself
 * instead of wiring up a separate platform object.
 */
@Plugin(
    id = "polocloud-hub-addon",
    name = "Polocloud Hub Addon Plugin",
    version = "3.0.0",
    description = "Handle /hub and aliases commands",
    authors = ["polocloud"],
)
class VelocityHubBootstrap @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
) : HubAddon<Player>() {

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        val meta = server.commandManager.metaBuilder("hub")
            .aliases("lobby", "l")
            .plugin(this)
            .build()

        server.commandManager.register(meta, HubCommand(this))
        logger.info("Registered /hub command (aliases: lobby, l)")
    }

    override fun currentServerName(player: Player): String? =
        player.currentServer.orElse(null)?.serverInfo?.name

    override fun connect(player: Player, service: Service): Boolean {
        val registeredServer = server.getServer(service.name()).orElse(null) ?: return false
        player.createConnectionRequest(registeredServer).fireAndForget()
        return true
    }

    override fun sendMessage(player: Player, message: String) {
        player.sendMessage(Component.text(message, NamedTextColor.RED))
    }
}