package de.polocloud.node.services

import de.polocloud.proto.ServiceData

/**
 * Maps a running [LocalService] to its protobuf [ServiceData] representation
 * exposed over the API. Mirrors [de.polocloud.node.group.GroupProtoMapper].
 */
object ServiceProtoMapper {

    fun toProto(service: LocalService): ServiceData = ServiceData.newBuilder()
        .setId(service.id.toString())
        .setIndex(service.index)
        .setGroup(service.groupName)
        .setState(service.state.name)
        .setPort(service.port)
        .setPid(service.process?.pid() ?: -1L)
        .setHost(service.hostname)
        .putAllProperties(service.properties)
        .setOnlinePlayers(service.onlinePlayers)
        .setMaxPlayers(service.maxPlayers)
        .setMotd(service.motd)
        .setCpuUsage(service.cpuUsage)
        .setUsedMemory(service.usedMemory)
        .build()
}