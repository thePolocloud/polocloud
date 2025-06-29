package dev.httpmarco.polocloud.agent.groups

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.services.Service

open class Group(val data: GroupData) {

    fun update() {
        // update the grou
        Agent.instance.runtime.groupStorage().update(group = this)
    }

    fun serviceCount() : Int {
        //todo
        return 0
    }

    fun services() : List<Service> {
        //todo
        return emptyList()
    }
}