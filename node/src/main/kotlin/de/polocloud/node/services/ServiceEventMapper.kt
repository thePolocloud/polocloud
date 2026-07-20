package de.polocloud.node.services

import de.polocloud.shared.property.Properties
import de.polocloud.shared.service.Service

/**
 * Maps a running [LocalService] to the shared [Service] model carried on cluster
 * lifecycle events (e.g. [de.polocloud.shared.event.server.ServerStartedEvent]).
 *
 * Mirrors [ServiceProtoMapper], which maps the same source to the protobuf wire
 * type used by the gRPC API.
 */
object ServiceEventMapper {

    fun toShared(service: LocalService): Service = Service(
        id = service.id.toString(),
        index = service.index,
        group = service.groupName,
        // Node and shared now share the same ServiceState enum, so no conversion is needed.
        state = service.state,
        port = service.port,
        host = service.hostname,
        pid = service.process?.pid() ?: -1L,
        cpuUsage = service.cpuUsage,
        usedMemory = service.usedMemory,
        onlinePlayers = service.onlinePlayers,
        maxPlayers = service.maxPlayers,
        motd = service.motd,
        properties = Properties.of(service.properties),
    )
}