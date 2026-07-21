package de.polocloud.bridge.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import de.polocloud.shared.service.Service
import de.polocloud.api.Polocloud
import de.polocloud.api.event.subscribe
import de.polocloud.bridge.BridgeBootstrap
import de.polocloud.bridge.BridgeInstance
import de.polocloud.shared.event.terminal.TabCompleteRequestEvent
import de.polocloud.shared.event.terminal.TabCompleteResponseEvent
import org.slf4j.Logger
import java.net.InetSocketAddress

/**
 * Velocity entry point for the Polocloud bridge.
 *
 * The runtime plugin description is provided by `velocity-plugin.json`; the
 * [Plugin] annotation only documents the metadata here.
 */
@Plugin(
    id = "polocloud-bridge",
    name = "Polocloud Bridge",
    version = "3.0.0",
    description = "Connects this proxy to the Polocloud node.",
    authors = ["polocloud"],
)
class VelocityBridgePlugin @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
) : BridgeInstance<ServerInfo>() {

    private val bootstrap = BridgeBootstrap(this)

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        // remove all registered servers on startup
        server.allServers.forEach {
            server.unregisterServer(it.serverInfo)
        }

        bootstrap.start("Velocity") { logger.info(it) }

        // Answers the node's `service <name> screen` tab-completion requests (see
        // TabCompleteBridge on the node side) with this proxy's own registered command
        // aliases — the same completion Velocity's own console would offer.
        Polocloud.eventService.subscribe<TabCompleteRequestEvent> { request ->
            if (request.serviceName != Polocloud.eventService.serviceName) return@subscribe
            val suggestions = suggestAliases(request.buffer)
            Polocloud.eventService.call(TabCompleteResponseEvent(request.requestId, suggestions))
        }
    }

    /**
     * Suggests matching command aliases for [buffer]'s first word.
     *
     * Only the command alias itself is supported: Velocity's public [CommandManager] exposes
     * no generic per-argument suggestion API, only per-command `suggest()`/`suggestAsync()`,
     * which needs the actual registered [com.velocitypowered.api.command.Command] instance
     * that [CommandManager.getCommandMeta] doesn't hand back. A second (or later) word
     * therefore gets no suggestions at all rather than an attempt at one.
     */
    private fun suggestAliases(buffer: String): List<String> {
        val prefix = buffer.trimStart()
        if (prefix.any { it.isWhitespace() }) return emptyList()

        val source = server.consoleCommandSource
        return server.commandManager.aliases
            .filter { it.startsWith(prefix, ignoreCase = true) && server.commandManager.hasCommand(it, source) }
            .sorted()
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        bootstrap.stop()
    }

    @Subscribe
    fun onConnect(event: PlayerChooseInitialServerEvent) {
        // A player may only join through this proxy if a fallback group has a running
        // service. If none is found, the initial server is left unset and Velocity
        // disconnects the player instead of dropping them onto an arbitrary server.
        bootstrap.bestFallback()
            ?.let { service -> server.getServer(service.name()).orElse(null) }
            ?.let { event.setInitialServer(it) }
    }

    @Subscribe
    fun onKick(event: KickedFromServerEvent) {
        // Send a kicked player to the emptiest fallback (by priority) instead of just
        // disconnecting them, excluding the server that just kicked them.
        val target = bootstrap.bestFallback(excludeServiceName = event.server.serverInfo.name)
            ?.let { service -> server.getServer(service.name()).orElse(null) }
            ?: return

        event.result = KickedFromServerEvent.RedirectPlayer.create(target)
    }

    override fun registerService(
        info: ServerInfo,
        service: Service
    ) {
        logger.info("Registering service ${service.name()} on ${info.address}")
        server.registerServer(info)
    }

    override fun unregisterService(
        info: ServerInfo,
        service: Service
    ) {
        logger.info("Unregistering service ${service.name()} on ${info.address}")
        server.unregisterServer(info)
    }

    override fun mapService(service: Service): ServerInfo {
        return ServerInfo(service.name(), InetSocketAddress(service.host, service.port))
    }
}