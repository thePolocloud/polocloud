package dev.httpmarco.polocloud.shared.player

import dev.httpmarco.polocloud.shared.service.Service

interface SharedPlayerActor {

    /**
     * Sends a message to the player.
     *
     * @param message The message to send.
     */
    fun message(player : PolocloudPlayer, message: String);

    /**
     * Connects the player to the specified server.
     *
     * @param serverName The name of the server to connect to.
     */
    fun toServer(player : PolocloudPlayer, serverName: String);


    /**
     * Connects the player to the specified server.
     *
     * @param service The service to connect to.
     */
    fun toServer(player : PolocloudPlayer, service: Service) {
        toServer(player, service.name())
    }

    /**
     * Kicks the player with the specified reason.
     *
     * @param reason The reason for kicking the player.
     */
    fun kick(player : PolocloudPlayer, reason: String);

}