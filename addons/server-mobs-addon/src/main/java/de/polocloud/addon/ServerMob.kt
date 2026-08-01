package de.polocloud.addon

import de.polocloud.addon.util.Position
import kotlinx.serialization.Serializable

/**
 * A mob entity attached to a group, displaying that group's running services when
 * interacted with. Platform-agnostic and directly persistable: no live service binding
 * is carried on the entry itself — [ServerMobAddon] resolves the group's current
 * services fresh from [de.polocloud.api.Polocloud.serviceService] on every render.
 *
 * @param item Name of an `org.bukkit.Material` (kept as a plain string so this class stays
 *   platform-agnostic, same approach as [type]) to show as a floating, spinning item above the
 *   hologram — e.g. `"RED_BED"`. `null` (the default) shows no floating item. Set via
 *   `/servermobs set <group> [item]`.
 */
@Serializable
data class ServerMob(val group: String, val position: Position, val type: String = "VILLAGER", val item: String? = null)
