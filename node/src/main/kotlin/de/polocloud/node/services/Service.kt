package de.polocloud.node.services

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.EntryRef
import de.polocloud.database.RepositoryName
import de.polocloud.node.group.Group
import de.polocloud.shared.service.ServiceState
import java.util.UUID

@RepositoryName("services")
open class Service(
    @EntryIdentifier val id: UUID,
    val serviceIndex: Int,
    @EntryRef(clazz = Group::class) val groupName: String,
    var state: ServiceState,
    var hostname: String,
    var port: Int,
    val nodeId: String,
) {

    fun name() = "$groupName-$serviceIndex"
}
