package de.polocloud.api.services

import de.polocloud.proto.ServiceData
import de.polocloud.shared.property.Properties
import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState

/**
 * Maps the protobuf [ServiceData] wire type to the shared [Service] model.
 */
object ServiceMapper {

    fun toApi(data: ServiceData): Service = Service(
        id = data.id,
        index = data.index,
        group = data.group,
        state = ServiceState.fromWire(data.state),
        port = data.port,
        host = data.host,
        pid = data.pid,
        cpuUsage = data.cpuUsage,
        usedMemory = data.usedMemory,
        onlinePlayers = data.onlinePlayers,
        maxPlayers = data.maxPlayers,
        motd = data.motd,
        properties = Properties.of(data.propertiesMap),
    )
}