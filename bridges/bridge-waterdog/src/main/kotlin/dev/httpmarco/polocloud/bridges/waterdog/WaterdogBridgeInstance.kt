package dev.httpmarco.polocloud.bridges.waterdog

import dev.httpmarco.polocloud.bridge.api.BridgeInstance
import dev.httpmarco.polocloud.sdk.java.Polocloud
import dev.httpmarco.polocloud.shared.events.definitions.PlayerJoinEvent
import dev.httpmarco.polocloud.shared.events.definitions.PlayerLeaveEvent
import dev.httpmarco.polocloud.shared.player.PolocloudPlayer
import dev.httpmarco.polocloud.shared.service.Service
import dev.waterdog.waterdogpe.ProxyServer
import dev.waterdog.waterdogpe.event.defaults.InitialServerConnectedEvent
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectedEvent
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent
import dev.waterdog.waterdogpe.network.connection.handler.IJoinHandler
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo
import dev.waterdog.waterdogpe.player.ProxiedPlayer
import java.net.InetSocketAddress

class WaterdogBridgeInstance : BridgeInstance<BedrockServerInfo, BedrockServerInfo>(), IJoinHandler {

    init {
        this.processBind()
        val eventManager = ProxyServer.getInstance().eventManager

        eventManager.subscribe(PlayerLoginEvent::class.java) { event ->
            val fallback = findFallback()

            if (fallback == null) {
                event.cancelReason = "No fallback servers are registered."
                event.isCancelled = true
            }
        }

        eventManager.subscribe(InitialServerConnectedEvent::class.java) { event ->
            val player = event.player
            val cloudPlayer = PolocloudPlayer(
                player.name,
                player.uniqueId,
                event.serverInfo.serverName,
                Polocloud.instance().selfServiceName()
            )
            updatePolocloudPlayer(PlayerJoinEvent(cloudPlayer))
        }

        eventManager.subscribe(PlayerDisconnectedEvent::class.java) { event ->
            val player = event.player
            val serverName = player.connectingServer?.serverName ?: "unknown"
            val cloudPlayer =
                PolocloudPlayer(player.name, player.uniqueId, serverName, Polocloud.instance().selfServiceName())
            updatePolocloudPlayer(PlayerLeaveEvent(cloudPlayer))
        }
    }

    override fun generateServerInfo(service: Service): BedrockServerInfo {
        return BedrockServerInfo(service.name(), InetSocketAddress(service.hostname, service.port), null)
    }

    override fun registerServerInfo(
        identifier: BedrockServerInfo,
        service: Service
    ): BedrockServerInfo {
        ProxyServer.getInstance().registerServerInfo(identifier)
        return findServer(identifier.serverName)!!
    }

    override fun unregister(identifier: BedrockServerInfo) {
        ProxyServer.getInstance().removeServerInfo(identifier.serverName)
    }

    override fun findServer(name: String): BedrockServerInfo? {
        return ProxyServer.getInstance().getServerInfo(name) as BedrockServerInfo?
    }

    override fun playerCount(info: BedrockServerInfo): Int {
        return info.players.size
    }

    override fun determineServer(p0: ProxiedPlayer?): ServerInfo {
        return findFallback()!!
    }
}