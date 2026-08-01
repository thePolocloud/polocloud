package de.polocloud.addons.proxy

import kotlinx.serialization.Serializable

/**
 * Every player-facing string `/proxy` sends, persisted to `messages.json` in the plugin's
 * data folder and hot-reloaded — an operator can reword any message without recompiling
 * the addon.
 */
@Serializable
data class Messages(
    val noPermission: String = "§cYou dont have permission to use this command!",
    val usage: String = "§cUsage: /proxy maintenance <on|off>",
    val maintenanceEnabled: String = "§aMaintenance mode has been enabled.",
    val maintenanceDisabled: String = "§aMaintenance mode has been disabled.",
    val maintenanceAlreadyEnabled: String = "§cMaintenance mode is already enabled.",
    val maintenanceAlreadyDisabled: String = "§cMaintenance mode is already disabled.",
)
