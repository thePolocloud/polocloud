package de.polocloud.addons.sign.system.spigot

import com.google.common.io.ByteStreams
import de.polocloud.addons.sign.system.SignSystem
import de.polocloud.addons.sign.system.spigot.renderer.toSignPosition
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSignOpenEvent
import org.bukkit.plugin.java.JavaPlugin


class BukkitListener(val plugin: JavaPlugin, val signSystem: SignSystem) : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val position = event.clickedBlock?.toSignPosition() ?: return
        val entry = signSystem.registry.at(position)
        if (entry != null && entry.service != null) {
            val out = ByteStreams.newDataOutput()
            out.writeUTF("Connect")
            out.writeUTF(entry.service!!.name())
            event.player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
        }
    }

    @EventHandler
    fun onDestroy(event: BlockBreakEvent) {
        if (signSystem.registry.at(event.block.toSignPosition()) != null) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDestroy(event: PlayerSignOpenEvent) {
        if (signSystem.registry.at(event.sign.toSignPosition()) != null) {
            event.isCancelled = true
        }
    }
}