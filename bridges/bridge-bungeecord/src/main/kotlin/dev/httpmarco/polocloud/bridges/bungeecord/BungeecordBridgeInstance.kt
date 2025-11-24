package dev.httpmarco.polocloud.bridges.bungeecord

import dev.httpmarco.polocloud.bridge.api.BridgeInstance
import dev.httpmarco.polocloud.sdk.java.Polocloud
import dev.httpmarco.polocloud.shared.events.definitions.PlayerJoinEvent
import dev.httpmarco.polocloud.shared.events.definitions.PlayerLeaveEvent
import dev.httpmarco.polocloud.shared.player.PolocloudPlayer
import dev.httpmarco.polocloud.shared.service.Service
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.PreLoginEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.event.ServerConnectedEvent
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.net.InetSocketAddress

class BungeecordBridgeInstance : BridgeInstance<ServerInfo, ServerInfo>(), Listener {

    init {
        this.processBind()
    }

    @EventHandler
    fun onPostLogin(event: PreLoginEvent) {
        if (hasFallbacks()) {
            event.reason = TextComponent("No fallback servers are registered.")
        }
    }

    @EventHandler
    fun connect(event: ServerConnectEvent) {
        if (event.reason == ServerConnectEvent.Reason.JOIN_PROXY || event.reason == ServerConnectEvent.Reason.LOBBY_FALLBACK) {
            val target = findFallback()

            if (target == null) {
                event.player.disconnect(TextComponent("No fallback servers available."))
                return
            }

            event.target = target
        }
    }

    @EventHandler
    fun onServerConnected(event: ServerConnectedEvent) {
        val player = event.player
        val serverInfo = event.server.info
        val serviceName = serverInfo.name

        val cloudPlayer = PolocloudPlayer(player.name, player.uniqueId, serviceName, Polocloud.instance().selfServiceName())
        updatePolocloudPlayer(PlayerJoinEvent(cloudPlayer))
    }

    @EventHandler
    fun onDisconnect(event: PlayerDisconnectEvent) {
        val player = event.player
        val serverInfo = player.server?.info
        val serviceName = serverInfo?.name ?: "unknown"

        // TODO why is this needed? A new player? Fix this later
        val cloudPlayer = PolocloudPlayer(player.name, player.uniqueId, serviceName, Polocloud.instance().selfServiceName())
        updatePolocloudPlayer(PlayerLeaveEvent(cloudPlayer))
    }


    @EventHandler
    fun kick(event: ServerKickEvent) {
        event.isCancelled = true
        event.cancelServer = findFallback()
    }

    override fun generateServerInfo(service: Service): ServerInfo {
        return ProxyServer.getInstance()
            .constructServerInfo(service.name(), InetSocketAddress(service.hostname, service.port), null, false)
    }

    override fun registerServerInfo(identifier: ServerInfo, service: Service): ServerInfo {
        ProxyServer.getInstance().servers[identifier.name] = identifier
        return identifier
    }

    override fun unregister(identifier: ServerInfo) {
        ProxyServer.getInstance().servers.remove(identifier.name)
    }

    override fun findServer(name: String): ServerInfo? {
        return ProxyServer.getInstance().getServerInfo(name)
    }

    override fun playerCount(info: ServerInfo): Int {
        return info.players.size
    }
}