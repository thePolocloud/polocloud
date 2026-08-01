package de.polocloud.addon.messages

import kotlinx.serialization.Serializable

/**
 * Every player-facing string the server-mobs addon sends, persisted to `messages.json` in the
 * plugin's data folder via [de.polocloud.addon.config.SingleDocumentStorage] and hot-reloaded
 * through [de.polocloud.addon.config.ReloadableConfig] — an operator can reword any message
 * without recompiling the addon. [groupNotFound]/[mobSpawned] support the `%group%` placeholder,
 * [invalidItem] the `%item%` placeholder.
 */
@Serializable
data class Messages(
    val notAPlayer: String = "§cThis command can only be used by a player!",
    val noPermission: String = "§cYou dont have permission to use this command!",
    val usageRoot: String = "§cUsage: /servermobs <set <group> [item]|remove>",
    val usageSet: String = "§cUsage: /servermobs set <group> [item]",
    val groupNotFound: String = "§cThe group %group% does not exist!",
    val invalidItem: String = "§cUnknown item %item%!",
    val mobSpawned: String = "§aSuccessfully spawned a server mob for group %group%!",
    val notLookingAtMob: String = "§cYou are not looking at a server mob!",
    val mobRemoved: String = "§aSuccessfully removed the server mob!",
    val mobRemoveFailed: String = "§cThere is no server mob there!",
)
