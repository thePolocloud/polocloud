package de.polocloud.addon

import de.polocloud.addon.util.Position
import kotlinx.serialization.Serializable

/**
 * A mob entity attached to a group, displaying that group's running services when
 * interacted with. Platform-agnostic and directly persistable: no live service binding
 * is carried on the entry itself — [ServerMobAddon] resolves the group's current
 * services fresh from [de.polocloud.api.Polocloud.serviceService] on every render.
 */
@Serializable
data class ServerMob(val group: String, val position: Position, val type: String = "VILLAGER")
