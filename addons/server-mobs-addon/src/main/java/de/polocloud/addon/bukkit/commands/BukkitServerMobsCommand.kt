package de.polocloud.addon.bukkit.commands

import de.polocloud.addon.SERVER_MOB_MANAGED_PERMISSION
import de.polocloud.addon.ServerMobAddon
import de.polocloud.addon.bukkit.BukkitServerMobPlatform
import de.polocloud.addon.util.Position
import de.polocloud.api.Polocloud
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

class BukkitServerMobsCommand(
    private val addon: ServerMobAddon,
    private val platform: BukkitServerMobPlatform,
) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String?>,
    ): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("§cThis command can only be used by a player!")
            return true
        }

        if (!player.hasPermission(SERVER_MOB_MANAGED_PERMISSION)) {
            player.sendMessage("§cYou dont have permission to use this command!")
            return true
        }

        when (args.getOrNull(0)?.lowercase()) {
            "set" -> {
                val group = args.getOrNull(1)
                if (group == null) player.sendMessage("§cUsage: /servermobs set <group>") else set(player, group)
            }
            "remove" -> remove(player)
            else -> player.sendMessage("§cUsage: /servermobs <set <group>|remove>")
        }

        return true
    }

    private fun set(player: Player, group: String) {
        if (Polocloud.groupService.find(group) == null) {
            player.sendMessage("§cThe group $group does not exist!")
            return
        }

        val location = player.location
        val position = Position(location.x, location.y, location.z, location.world!!.name)
        addon.set(group, position)
        player.sendMessage("§aSuccessfully spawned a server mob for group $group!")
    }

    private fun remove(player: Player) {
        val position = targetManagedMob(player)?.let(platform::positionOf)
        if (position == null) {
            player.sendMessage("§cYou are not looking at a server mob!")
            return
        }

        if (addon.remove(position)) {
            player.sendMessage("§aSuccessfully removed the server mob!")
        } else {
            player.sendMessage("§cThere is no server mob there!")
        }
    }

    private fun targetManagedMob(player: Player, maxDistance: Double = 10.0): Entity? =
        player.world.rayTraceEntities(player.eyeLocation, player.eyeLocation.direction, maxDistance) {
            platform.positionOf(it) != null
        }?.hitEntity
}
