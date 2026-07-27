package de.polocloud.api.group

import de.polocloud.proto.GroupData
import de.polocloud.shared.property.Properties

/**
 * Maps between the protobuf [GroupData] wire type and the public API [Group].
 */
object GroupMapper {

    fun toApi(data: GroupData): Group = Group(
        name = data.name,
        memory = data.memory,
        startThreshold = data.startThreshold,
        minOnline = data.minOnline,
        maxOnline = data.maxOnline,
        platform = data.platform,
        version = data.version,
        properties = Properties.of(data.propertiesMap),
        templates = data.templatesList,
        nodes = data.nodesList,
    )

    fun toProto(group: Group): GroupData = GroupData.newBuilder()
        .setName(group.name)
        .setMemory(group.memory)
        .setStartThreshold(group.startThreshold)
        .setMinOnline(group.minOnline)
        .setMaxOnline(group.maxOnline)
        .setPlatform(group.platform)
        .setVersion(group.version)
        .putAllProperties(group.properties.asMap())
        .addAllTemplates(group.templates)
        .addAllNodes(group.nodes)
        .build()
}
