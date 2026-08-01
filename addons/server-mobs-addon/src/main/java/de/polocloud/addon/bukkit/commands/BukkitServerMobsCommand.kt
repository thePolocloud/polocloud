package de.polocloud.addon.bukkit.commands

import de.polocloud.addon.SERVER_MOB_MANAGED_PERMISSION
import de.polocloud.addon.ServerMobAddon
import de.polocloud.addon.ServerMobPlaceholders
import de.polocloud.addon.bukkit.BukkitServerMobPlatform
import de.polocloud.addon.config.ReloadableConfig
import de.polocloud.addon.messages.Messages
import de.polocloud.addon.util.Position
import de.polocloud.api.Polocloud
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

class BukkitServerMobsCommand(
    private val addon: ServerMobAddon,
    private val platform: BukkitServerMobPlatform,
    private val messages: ReloadableConfig<Messages>,
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String?>,
    ): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(messages.current().notAPlayer)
            return true
        }

        if (!player.hasPermission(SERVER_MOB_MANAGED_PERMISSION)) {
            player.sendMessage(messages.current().noPermission)
            return true
        }

        when (args.getOrNull(0)?.lowercase()) {
            "set" -> {
                val group = args.getOrNull(1)
                if (group == null) player.sendMessage(messages.current().usageSet) else set(player, group, args.getOrNull(2))
            }
            "remove" -> remove(player)
            else -> player.sendMessage(messages.current().usageRoot)
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String?>,
    ): List<String> {
        val current = (args.lastOrNull() ?: "").lowercase()

        return when (args.size) {
            1 -> listOf("set", "remove").filter { it.startsWith(current) }
            2 -> if (args[0]?.lowercase() == "set") {
                Polocloud.groupService.findAll().map { it.name }.filter { it.lowercase().startsWith(current) }
            } else {
                emptyList()
            }
            3 -> if (args[0]?.lowercase() == "set") {
                Material.values().map { it.name }.filter { it.lowercase().startsWith(current) }
            } else {
                emptyList()
            }
            else -> emptyList()
        }
    }

    private fun set(player: Player, group: String, item: String?) {
        if (Polocloud.groupService.find(group) == null) {
            player.sendMessage(ServerMobPlaceholders.group(messages.current().groupNotFound, group))
            return
        }

        val material = item?.uppercase()
        if (material != null && runCatching { Material.valueOf(material) }.isFailure) {
            player.sendMessage(messages.current().invalidItem.replace("%item%", item))
            return
        }

        val location = player.location
        val position = Position(location.x, location.y, location.z, location.world!!.name)
        addon.set(group, position, item = material)
        player.sendMessage(ServerMobPlaceholders.group(messages.current().mobSpawned, group))
    }

    private fun remove(player: Player) {
        val position = targetManagedMob(player)?.let(platform::positionOf)
        if (position == null) {
            player.sendMessage(messages.current().notLookingAtMob)
            return
        }

        if (addon.remove(position)) {
            player.sendMessage(messages.current().mobRemoved)
        } else {
            player.sendMessage(messages.current().mobRemoveFailed)
        }
    }

    private fun targetManagedMob(player: Player, maxDistance: Double = 10.0): Entity? =
        player.world.rayTraceEntities(player.eyeLocation, player.eyeLocation.direction, maxDistance) {
            platform.positionOf(it) != null
        }?.hitEntity
}
