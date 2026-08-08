package de.polocloud.addons.hub.waterfall.command

import de.polocloud.addons.hub.HubAddon
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command

/**
 * Waterfall/BungeeCord counterpart of [de.polocloud.addons.hub.velocity.command.HubCommand].
 * Registered under `hub` with aliases `lobby`, `l` (see
 * [de.polocloud.addons.hub.waterfall.WaterfallHubBootstrap]). Only players can be routed;
 * console/RCON invocations are silently ignored, same as the Velocity side.
 */
class HubCommand(private val addon: HubAddon<ProxiedPlayer>) : Command("hub", null, "lobby", "l") {

    override fun execute(sender: CommandSender, args: Array<String>) {
        val player = sender as? ProxiedPlayer ?: return
        addon.sendToHub(player)
    }
}
