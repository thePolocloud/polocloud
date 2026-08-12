package de.polocloud.api.player

import de.polocloud.proto.FindPlayerRequest
import de.polocloud.proto.ListPlayersRequest
import de.polocloud.proto.PlayerApiServiceGrpcKt
import de.polocloud.proto.PlayerData
import de.polocloud.proto.RegisterPlayerRequest
import de.polocloud.proto.UnregisterPlayerRequest
import de.polocloud.proto.UpdatePlayerServerRequest
import de.polocloud.shared.player.CloudPlayer
import de.polocloud.shared.property.Properties
import io.grpc.ManagedChannel
import java.util.concurrent.TimeUnit

/**
 * gRPC-backed [PlayerApiClient] that talks to the node's `PlayerApiService`.
 *
 * Mirrors [de.polocloud.api.services.GrpcServiceApiClient] (lazy channel, deadline
 * on every call).
 */
class GrpcPlayerApiClient(
    private val channelProvider: () -> ManagedChannel,
) : PlayerApiClient {

    private fun stub() = PlayerApiServiceGrpcKt.PlayerApiServiceCoroutineStub(channelProvider())
        .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)

    override suspend fun registerPlayer(player: CloudPlayer): PlayerCommandResult {
        val request = RegisterPlayerRequest.newBuilder().setPlayer(toProto(player)).build()
        val response = stub().registerPlayer(request)
        return PlayerCommandResult(response.success, response.message)
    }

    override suspend fun updatePlayerServer(playerId: String, server: String): PlayerCommandResult {
        val request = UpdatePlayerServerRequest.newBuilder().setPlayerId(playerId).setServer(server).build()
        val response = stub().updatePlayerServer(request)
        return PlayerCommandResult(response.success, response.message)
    }

    override suspend fun unregisterPlayer(playerId: String, kicked: Boolean, reason: String): PlayerCommandResult {
        val request = UnregisterPlayerRequest.newBuilder()
            .setPlayerId(playerId)
            .setKicked(kicked)
            .setReason(reason)
            .build()
        val response = stub().unregisterPlayer(request)
        return PlayerCommandResult(response.success, "")
    }

    override suspend fun findPlayer(playerId: String?, name: String?): CloudPlayer? {
        val request = FindPlayerRequest.newBuilder().apply {
            playerId?.let { setPlayerId(it) }
            name?.let { setName(it) }
        }.build()
        val response = stub().findPlayer(request)
        return if (response.found) toShared(response.player) else null
    }

    override suspend fun listPlayers(): List<CloudPlayer> {
        val response = stub().listPlayers(ListPlayersRequest.getDefaultInstance())
        return response.playersList.map(::toShared)
    }

    private fun toProto(player: CloudPlayer): PlayerData = PlayerData.newBuilder()
        .setId(player.id)
        .setName(player.name)
        .setSkinValue(player.skinValue)
        .setSkinSignature(player.skinSignature)
        .putAllProperties(player.properties.asMap())
        .setCurrentProxy(player.currentProxy)
        .setCurrentServer(player.currentServer ?: "")
        .build()

    private fun toShared(data: PlayerData): CloudPlayer = CloudPlayer(
        id = data.id,
        name = data.name,
        skinValue = data.skinValue,
        skinSignature = data.skinSignature,
        properties = Properties.of(data.propertiesMap),
        currentProxy = data.currentProxy,
        currentServer = data.currentServer.ifBlank { null },
    )

    private companion object {
        const val DEADLINE_SECONDS = 10L
    }
}
