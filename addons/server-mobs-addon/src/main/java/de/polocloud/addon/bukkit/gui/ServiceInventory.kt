package de.polocloud.addon.bukkit.gui

import de.polocloud.addon.ServerMobPlaceholders
import de.polocloud.addon.display.InventoryConfig
import de.polocloud.addon.display.ItemConfig
import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/** Builds the inventory a player sees after right-clicking a group's server mob, entirely driven by [InventoryConfig]. */
object ServiceInventory {

    private const val ROW_SIZE = 9
    private const val MAX_ROWS = 6

    fun build(group: String, services: List<Service>, config: InventoryConfig): Inventory {
        val holder = ServiceInventoryHolder(group)
        val rows = if (config.autoSize) {
            ((services.size - 1) / ROW_SIZE + 1).coerceIn(1, MAX_ROWS)
        } else {
            config.rows.coerceIn(1, MAX_ROWS)
        }
        val title = ServerMobPlaceholders.group(config.title, group, services)
        val inventory = Bukkit.createInventory(holder, rows * ROW_SIZE, title)
        holder.backingInventory = inventory

        if (services.isEmpty()) {
            inventory.setItem(rows * ROW_SIZE / 2, itemFrom(config.emptyItem))
        } else {
            services.sortedBy { it.index }.take(rows * ROW_SIZE).forEach { inventory.addItem(itemFor(it, group, config)) }
        }

        return inventory
    }

    private fun itemFor(service: Service, group: String, config: InventoryConfig): ItemStack {
        val entry = config.items[service.state] ?: config.items[ServiceState.UNKNOWN] ?: FALLBACK
        val item = ItemStack(materialOf(entry.material))
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(ServerMobPlaceholders.service(entry.name, group, service))
            lore = entry.lore.map { ServerMobPlaceholders.service(it, group, service) }
        }
        return item
    }

    private fun itemFrom(entry: ItemConfig): ItemStack {
        val item = ItemStack(materialOf(entry.material))
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(entry.name)
            lore = entry.lore.takeIf { it.isNotEmpty() }
        }
        return item
    }

    /** Falls back to gray wool for an unrecognised/typo'd material name rather than failing the whole render. */
    private fun materialOf(name: String): Material = runCatching { Material.valueOf(name) }.getOrDefault(Material.GRAY_WOOL)

    private val FALLBACK = ItemConfig("GRAY_WOOL", "§b%service% §7[§f%state%§7]")
}
