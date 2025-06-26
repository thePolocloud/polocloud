package dev.httpmarco.polocloud.agent.groups

import dev.httpmarco.polocloud.agent.Agent

open class Group(val data: GroupData) {

    fun update() {
        // update the grou
        Agent.instance.runtime.groupStorage().update(group = this)
    }
}