package de.polocloud.addons.hub.velocity.command

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import de.polocloud.addons.hub.HubAddon

/**
 * Handles `/hub` and its aliases `/lobby`, `/l` (see
 * [de.polocloud.addons.hub.velocity.VelocityHubBootstrap] for alias registration).
 * Only players can be routed; console/RCON invocations are silently ignored.
 */
class HubCommand(private val addon: HubAddon<Player>) : SimpleCommand {

    override fun execute(invocation: SimpleCommand.Invocation) {
        val player = invocation.source() as? Player ?: return
        addon.sendToHub(player)
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean =
        invocation.source() is Player
}
