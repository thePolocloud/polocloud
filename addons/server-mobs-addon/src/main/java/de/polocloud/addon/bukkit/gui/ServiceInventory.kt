package de.polocloud.addon.bukkit.gui

import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/** Builds the inventory a player sees after right-clicking a group's server mob. */
object ServiceInventory {

    private const val ROW_SIZE = 9
    private const val MAX_SIZE = 54

    fun build(group: String, services: List<Service>): Inventory {
        val holder = ServiceInventoryHolder(group)
        val rows = ((services.size - 1) / ROW_SIZE + 1).coerceIn(1, MAX_SIZE / ROW_SIZE)
        val inventory = Bukkit.createInventory(holder, rows * ROW_SIZE, "§8Group: §b$group")
        holder.backingInventory = inventory

        if (services.isEmpty()) {
            inventory.setItem(rows * ROW_SIZE / 2, emptyItem())
        } else {
            services.sortedBy { it.index }.take(MAX_SIZE).forEach { inventory.addItem(itemFor(it)) }
        }

        return inventory
    }

    private fun itemFor(service: Service): ItemStack {
        val item = ItemStack(materialFor(service.state))
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName("§b${service.name()} §7[§f${service.state}§7]")
            lore = listOf(
                "§7Players: §f${service.onlinePlayers}/${service.maxPlayers}",
                "§7Memory: §f${service.usedMemory.toInt()} MB",
                "§7CPU: §f${"%.1f".format(service.cpuUsage)}%",
                "§7Host: §f${service.host}:${service.port}",
            )
        }
        return item
    }

    private fun emptyItem(): ItemStack {
        val item = ItemStack(Material.BARRIER)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName("§cNo services online")
        }
        return item
    }

    private fun materialFor(state: ServiceState): Material = when (state) {
        ServiceState.RUNNING -> Material.LIME_WOOL
        ServiceState.STARTING, ServiceState.QUEUED -> Material.YELLOW_WOOL
        ServiceState.STOPPING, ServiceState.STOPPED -> Material.RED_WOOL
        ServiceState.UNKNOWN -> Material.GRAY_WOOL
    }
}
