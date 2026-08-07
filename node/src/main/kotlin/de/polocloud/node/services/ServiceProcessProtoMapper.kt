package de.polocloud.node.services

import de.polocloud.proto.ProtoServiceProcessData

/**
 * Maps a running [LocalService] to the CLI-facing [ProtoServiceProcessData] wire type
 * (consumed by the `service` command). Mirrors [ServiceProtoMapper], which maps the
 * same source to the API-facing `ServiceData`.
 */
object ServiceProcessProtoMapper {

    fun toProto(service: LocalService, nodeId: String = ""): ProtoServiceProcessData =
        ProtoServiceProcessData.newBuilder()
            .setUuid(service.id.toString())
            .setIndex(service.serviceIndex)
            .setPlan(service.groupName)
            .setNodeId(nodeId)
            .setBoundPort(service.port)
            .setPid((service.process?.pid() ?: -1L).toInt())
            // State travels as its name (RUNNING, STARTING, …) — lossless, unlike the
            // coarser common.ServiceState enum.
            .setState(service.state.name)
            .putAllProperties(service.properties)
            .setOnlinePlayers(service.onlinePlayers)
            .setMaxPlayers(service.maxPlayers)
            .setMotd(service.motd)
            .build()
}
