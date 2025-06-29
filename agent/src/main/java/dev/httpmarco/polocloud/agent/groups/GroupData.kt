package dev.httpmarco.polocloud.agent.groups

import dev.httpmarco.polocloud.platforms.PlatformIndex
import kotlinx.serialization.Serializable

@Serializable
data class GroupData(val name: String, var platform: PlatformIndex, var minMemory : Int, var maxMemory : Int, var minOnlineService: Int, var maxOnlineService: Int)
