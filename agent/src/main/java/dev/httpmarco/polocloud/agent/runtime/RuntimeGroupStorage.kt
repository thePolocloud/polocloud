package dev.httpmarco.polocloud.agent.runtime

import dev.httpmarco.polocloud.agent.groups.Group

interface RuntimeGroupStorage {

    fun items(): List<Group>

    fun item(identifier: String): Group?

    fun publish(group: Group)

    fun destroy(group: Group)

    fun present(identifier: String): Boolean

    fun update(group: Group)

}