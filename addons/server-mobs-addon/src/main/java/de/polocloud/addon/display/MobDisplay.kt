package de.polocloud.addon.display

import de.polocloud.shared.service.ServiceState
import kotlinx.serialization.Serializable

/**
 * Every visual aspect of a server mob that an operator can configure without recompiling the
 * addon — persisted to `display.json` via [de.polocloud.common.configuration.SingleDocumentStorage],
 * hot-reloaded through [de.polocloud.addon.config.ReloadableConfig]. Mirrors how
 * [de.polocloud.addons.sign.system.layout.SignLayout] configures the sign-system addon's
 * appearance, just for a single global display instead of a set of named/per-state layouts.
 */
@Serializable
data class MobDisplay(
    val hologram: HologramConfig = HologramConfig(),
    val inventory: InventoryConfig = InventoryConfig(),
    val look: LookConfig = LookConfig(),
)

/**
 * The floating text hologram shown above a server mob. [lines] resolve `%group%`/`%players%`/
 * `%maxplayers%`/`%onlineservices%`/`%services%` via [de.polocloud.addon.ServerMobPlaceholders.group] —
 * see [de.polocloud.addon.bukkit.hologram.BukkitMobHologram] for the rendering side.
 *
 * Defaults to the requested look: a player icon + status dot + yellow player count on the first
 * line, the group name in aqua below it.
 */
@Serializable
data class HologramConfig(
    val enabled: Boolean = true,
    val lines: List<String> = listOf("§fSpieler §7● §e%players%", "§b%group%"),
    /** Blocks above the mob's feet the first line is drawn at. */
    val heightOffset: Double = 1.9,
    /** Blocks between each subsequent line. */
    val lineSpacing: Double = 0.25,
)

/**
 * The inventory a player sees after right-clicking a server mob, and the items shown for each
 * of that group's services. [items] falls back to [ServiceState.UNKNOWN]'s entry (or a built-in
 * gray placeholder if that isn't configured either) for any state without an explicit entry, so
 * an operator only needs to override the states they actually care about.
 *
 * @param autoSize When `true` (default), the inventory grows with the service count (existing
 *   behaviour); when `false`, it always has exactly [rows] rows.
 */
@Serializable
data class InventoryConfig(
    val title: String = "§8Group: §b%group%",
    val autoSize: Boolean = true,
    val rows: Int = 6,
    val emptyItem: ItemConfig = ItemConfig(material = "BARRIER", name = "§cNo services online"),
    val items: Map<ServiceState, ItemConfig> = defaultItems(),
) {
    companion object {
        private const val DEFAULT_NAME = "§b%service% §7[§f%state%§7]"
        private val DEFAULT_LORE = listOf(
            "§7Players: §f%online%/%max%",
            "§7Memory: §f%memory% MB",
            "§7CPU: §f%cpu%%",
            "§7Host: §f%host%:%port%",
        )

        fun defaultItems(): Map<ServiceState, ItemConfig> = mapOf(
            ServiceState.RUNNING to ItemConfig("LIME_WOOL", DEFAULT_NAME, DEFAULT_LORE),
            ServiceState.STARTING to ItemConfig("YELLOW_WOOL", DEFAULT_NAME, DEFAULT_LORE),
            ServiceState.QUEUED to ItemConfig("YELLOW_WOOL", DEFAULT_NAME, DEFAULT_LORE),
            ServiceState.STOPPING to ItemConfig("RED_WOOL", DEFAULT_NAME, DEFAULT_LORE),
            ServiceState.STOPPED to ItemConfig("RED_WOOL", DEFAULT_NAME, DEFAULT_LORE),
            ServiceState.UNKNOWN to ItemConfig("GRAY_WOOL", DEFAULT_NAME, DEFAULT_LORE),
        )
    }
}

/**
 * One configurable inventory item. [material] is a `org.bukkit.Material` name kept as a plain
 * string (same approach as [de.polocloud.addons.sign.system.layout.BannerDesign]'s `baseColor`),
 * so this class — and the JSON it (de)serializes to — stays platform-agnostic. [name]/[lore]
 * resolve the same placeholders as [de.polocloud.addon.ServerMobPlaceholders.service].
 */
@Serializable
data class ItemConfig(val material: String, val name: String, val lore: List<String> = emptyList())

/**
 * Whether a server mob turns to face the nearest player, and how.
 *
 * @param radius Blocks within which a player is considered "nearby" and looked at.
 * @param intervalTicks How often (in ticks) the look direction is recomputed.
 */
@Serializable
data class LookConfig(val enabled: Boolean = true, val radius: Double = 8.0, val intervalTicks: Long = 4L)
