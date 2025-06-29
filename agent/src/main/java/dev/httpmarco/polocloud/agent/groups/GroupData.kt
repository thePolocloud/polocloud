package dev.httpmarco.polocloud.agent.groups

import kotlinx.serialization.Serializable

@Serializable
data class GroupData(val name: String, var minMemory : Int, var maxMemory : Int, var minOnlineService: Int, var maxOnlineService: Int)
