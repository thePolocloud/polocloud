package de.polocloud.addons.sign.system.spigot.command

import de.polocloud.addons.sign.system.SIGN_MANAGED_PERMISSION
import de.polocloud.addons.sign.system.SignPosition
import de.polocloud.addons.sign.system.SignSystem
import de.polocloud.addons.sign.system.spigot.BukkitSignPlatform
import de.polocloud.api.Polocloud
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SignCommand(
    private val signSystem: SignSystem,
    private val platform: BukkitSignPlatform,
) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String?>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("§cThis command can only be used by a player!")
            return true
        }

        if(!player.hasPermission(SIGN_MANAGED_PERMISSION)) {
            sender.sendMessage("§cYou dont have permission to use this command!")
            return true
        }

        when (args.getOrNull(0)?.lowercase()) {
            "add" -> {
                val group = args.getOrNull(1)
                if (group == null) player.sendMessage("§cUsage: /signs add <group>") else add(player, group)
            }
            "remove" -> remove(player)
            else -> player.sendMessage("§cUsage: /signs <add <group>|remove>")
        }

        return true
    }

    private fun add(player: Player, group: String) {
        if (Polocloud.groupService.find(group) == null) {
            player.sendMessage("§cThe group $group does not exist!")
            return
        }

        val target = player.getTargetBlockExact(7)
        if (target == null) {
            player.sendMessage("§cYou are not looking at a block!")
            return
        }

        val type = platform.detectType(target.type)
        if (type == null) {
            player.sendMessage("§cYou are not looking at a supported sign type!")
            return
        }

        signSystem.attach(type, SignPosition(target.x, target.y, target.z, target.world.name), group)
        player.sendMessage("§aSuccessfully added a $type for group $group!")
    }

    private fun remove(player: Player) {
        val target = player.getTargetBlockExact(7)
        if (target == null) {
            player.sendMessage("§cYou are not looking at a block!")
            return
        }

        val position = SignPosition(target.x, target.y, target.z, target.world.name)
        if (signSystem.detach(position)) {
            player.sendMessage("§aSuccessfully removed the sign!")
        } else {
            player.sendMessage("§cThere is no sign attached at that position!")
        }
    }
}