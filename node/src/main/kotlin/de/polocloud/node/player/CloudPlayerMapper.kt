package de.polocloud.node.player

import de.polocloud.node.group.PropertyCodec
import de.polocloud.proto.PlayerData
import de.polocloud.shared.player.CloudPlayer as SharedCloudPlayer
import de.polocloud.shared.property.Properties
import java.util.UUID

/**
 * Maps between the persisted domain [CloudPlayer], its protobuf [PlayerData]
 * representation (the gRPC API), and the [SharedCloudPlayer] representation
 * carried on cluster player events. Mirrors [de.polocloud.node.group.GroupProtoMapper]
 * / [de.polocloud.node.services.ServiceProtoMapper] / [de.polocloud.node.services.ServiceEventMapper].
 */
object CloudPlayerMapper {

    fun toProto(player: CloudPlayer): PlayerData = PlayerData.newBuilder()
        .setId(player.id.toString())
        .setName(player.name)
        .setSkinValue(player.skinValue)
        .setSkinSignature(player.skinSignature)
        .putAllProperties(player.properties)
        .setCurrentProxy(player.currentProxy)
        .setCurrentServer(player.currentServer ?: "")
        .build()

    fun toDomain(data: PlayerData): CloudPlayer = CloudPlayer(
        id = UUID.fromString(data.id),
        name = data.name,
        skinValue = data.skinValue,
        skinSignature = data.skinSignature,
        propertiesJson = PropertyCodec.encode(data.propertiesMap),
        currentProxy = data.currentProxy,
        currentServer = data.currentServer.ifBlank { null },
    )

    fun toShared(player: CloudPlayer): SharedCloudPlayer = SharedCloudPlayer(
        id = player.id.toString(),
        name = player.name,
        skinValue = player.skinValue,
        skinSignature = player.skinSignature,
        properties = Properties.of(player.properties),
        currentProxy = player.currentProxy,
        currentServer = player.currentServer,
    )
}
