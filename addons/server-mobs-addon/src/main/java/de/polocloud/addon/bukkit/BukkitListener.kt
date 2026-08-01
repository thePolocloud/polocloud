package de.polocloud.addon.bukkit

import de.polocloud.addon.ServerMobAddon
import de.polocloud.addon.bukkit.gui.ServiceInventory
import de.polocloud.addon.bukkit.gui.ServiceInventoryHolder
import de.polocloud.api.Polocloud
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerInteractEntityEvent

class BukkitListener(
    private val addon: ServerMobAddon,
    private val platform: BukkitServerMobPlatform,
) : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEntityEvent) {
        val position = platform.positionOf(event.rightClicked) ?: return
        val mob = addon.registry.at(position) ?: return
        event.isCancelled = true

        val services = Polocloud.serviceService.findByGroup(mob.group)
        event.player.openInventory(ServiceInventory.build(mob.group, services))
    }

    /** A managed villager stands still by AI, but a player can still shove/hit it — block both. */
    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        if (platform.positionOf(event.entity) != null) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.inventory.holder is ServiceInventoryHolder) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is ServiceInventoryHolder) {
            event.isCancelled = true
        }
    }
}
