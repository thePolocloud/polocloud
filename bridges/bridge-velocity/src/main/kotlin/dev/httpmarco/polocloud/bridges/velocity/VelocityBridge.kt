package dev.httpmarco.polocloud.bridges.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import dev.httpmarco.polocloud.bridge.api.BridgeInstance
import dev.httpmarco.polocloud.sdk.java.Polocloud
import dev.httpmarco.polocloud.shared.events.definitions.PlayerJoinEvent
import dev.httpmarco.polocloud.shared.events.definitions.PlayerLeaveEvent
import dev.httpmarco.polocloud.shared.player.PolocloudPlayer
import dev.httpmarco.polocloud.shared.service.Service
import org.bstats.velocity.Metrics
import java.net.InetSocketAddress
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class VelocityBridge @Inject constructor(
    val proxyServer: ProxyServer,
    val metricsFactory: Metrics.Factory
) : BridgeInstance<RegisteredServer, ServerInfo>() {

    private lateinit var metrics: Metrics

    init {
        // remove all registered servers on startup
        proxyServer.allServers.forEach {
            proxyServer.unregisterServer(it.serverInfo)
        }
    }

    @Subscribe
    fun onInitialize(event: ProxyInitializeEvent) {
        this.processBind()

        val pluginId = 26763
        metrics = metricsFactory.make(this, pluginId)
    }

    @Subscribe
    fun onConnect(event: PlayerChooseInitialServerEvent) {
        findFallback()?.let { event.setInitialServer(it) }
    }

    @Subscribe
    fun onPlayerJoin(event: ServerConnectedEvent) {
        val player = event.player
        updatePolocloudPlayer(
            PlayerJoinEvent(
                PolocloudPlayer(
                    player.username,
                    player.uniqueId,
                    event.server.serverInfo.name,
                    Polocloud.instance().selfServiceName()
                )
            )
        )
    }

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        val player = event.player

        val serviceName = player.currentServer
            .flatMap { Optional.ofNullable(it.serverInfo.name) }
            .orElse(null)
        if (serviceName == null) {
            // Player was not connected to any service
            return
        }

        // todo why is this needed? A new player? Fix this later
        updatePolocloudPlayer(PlayerLeaveEvent(PolocloudPlayer(player.username, player.uniqueId, serviceName, Polocloud.instance().selfServiceName())))
    }

    @Subscribe
    fun onKick(event: KickedFromServerEvent) {
        if (event.player.isActive) {
            if (event.server.serverInfo == null) {
                // Player was not connected to any service
                return
            }
            val server = findFallback()
            if (server == null || server == event.server) {
                return
            }
            event.result = KickedFromServerEvent.RedirectPlayer.create(server)
        }
    }

    override fun generateServerInfo(service: Service): ServerInfo {
        return ServerInfo(service.name(), InetSocketAddress(service.hostname, service.port))
    }

    override fun registerServerInfo(
        identifier: ServerInfo,
        service: Service
    ): RegisteredServer {
        return proxyServer.registerServer(identifier)
    }

    override fun unregister(identifier: RegisteredServer) {
        proxyServer.unregisterServer(identifier.serverInfo)
    }

    override fun findServer(name: String): RegisteredServer? {
        return proxyServer.getServer(name).getOrNull()
    }

    override fun playerCount(info: RegisteredServer): Int {
        return info.playersConnected.size
    }
}
