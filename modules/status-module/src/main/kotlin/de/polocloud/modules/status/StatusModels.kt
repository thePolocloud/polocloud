package de.polocloud.modules.status

import kotlinx.serialization.Serializable

@Serializable
enum class GroupAvailability {
    ONLINE,
    OFFLINE,
    /** Forced off via [StatusGroupConfig.available], regardless of what's actually running. */
    MAINTENANCE,
}

@Serializable
data class GroupStatus(
    val name: String,
    val displayName: String,
    val availability: GroupAvailability,
    val onlineServices: Int,
    val onlinePlayers: Int,
    val maxPlayers: Int,
)

@Serializable
data class StatusSnapshot(
    val node: String,
    val generatedAt: Long,
    val groups: List<GroupStatus>,
)
