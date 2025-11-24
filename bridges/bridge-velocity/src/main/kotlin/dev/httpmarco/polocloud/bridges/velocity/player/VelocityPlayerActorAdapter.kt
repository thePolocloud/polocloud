package dev.httpmarco.polocloud.bridges.velocity.player

import com.velocitypowered.api.proxy.ProxyServer
import dev.httpmarco.polocloud.sdk.java.player.PlayerActorAdapter
import net.kyori.adventure.text.Component
import java.util.UUID

class VelocityPlayerActorAdapter(val server : ProxyServer) : PlayerActorAdapter {

    override fun message(uuid: UUID?, message: String) {
        server.getPlayer(uuid)?.get()?.sendMessage(Component.text(message))
    }

    override fun kick(uuid: UUID, message: String) {
        server.getPlayer(uuid)?.get()?.disconnect(Component.text(message))
    }

    override fun toServer(uuid: UUID?, service: String?) {
        server.getPlayer(uuid)?.get()?.createConnectionRequest(server.getServer(service).get())?.connect()
    }
}