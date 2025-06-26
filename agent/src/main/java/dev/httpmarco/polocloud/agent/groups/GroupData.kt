package dev.httpmarco.polocloud.agent.groups

import kotlinx.serialization.Serializable

@Serializable
data class GroupData(val name: String, var minOnlineService: Int, var maxOnlineService: Int)
