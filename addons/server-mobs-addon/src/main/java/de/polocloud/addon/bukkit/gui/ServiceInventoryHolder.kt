package de.polocloud.addon.bukkit.gui

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

/**
 * Marks an [Inventory] as one of ours, so [de.polocloud.addon.bukkit.BukkitListener] can
 * tell it apart from a player's own inventory and cancel clicks/drags in it.
 */
class ServiceInventoryHolder(val group: String) : InventoryHolder {

    lateinit var backingInventory: Inventory

    override fun getInventory(): Inventory = backingInventory
}
