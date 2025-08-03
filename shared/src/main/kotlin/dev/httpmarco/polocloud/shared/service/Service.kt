package dev.httpmarco.polocloud.shared.service

import dev.httpmarco.polocloud.v1.GroupType
import dev.httpmarco.polocloud.v1.ServiceState

class Service(val groupName: String, val id: Int, val hostname: String, val port: Int, val state : ServiceState, val type : GroupType, val properties: Map<String, String>) {

    fun name(): String {
        return "${groupName}-$id"
    }
}