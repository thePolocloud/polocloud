package de.polocloud.addon.bukkit.hologram

import de.polocloud.addon.util.Position
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

/**
 * The optional floating item shown spinning above a server mob's [BukkitMobHologram], set via
 * `/servermobs set <group> [item]`. One invisible, gravity-less marker [ArmorStand] per mob,
 * wearing the configured item as its helmet — the same "invisible armor stand as an item
 * display" trick [BukkitMobHologram] uses for text, just with an equipped item instead of a
 * custom name, and its yaw nudged forward every [spinAll] call instead of staying fixed.
 */
object BukkitMobFloatingItem {

    private val stands = ConcurrentHashMap<Position, ArmorStand>()

    /** Shows/updates the floating item at [position], [height] blocks above its feet. `null`/invalid [material] hides it instead. */
    fun show(position: Position, material: String?, height: Double) {
        val itemMaterial = material?.let { runCatching { Material.valueOf(it) }.getOrNull() }
        if (itemMaterial == null) {
            hide(position)
            return
        }

        val world = Bukkit.getWorld(position.world) ?: return
        val stand = stands[position] ?: spawn(world, position).also { stands[position] = it }

        if (stand.location.y != position.y + height) {
            val location = stand.location
            location.y = position.y + height
            stand.teleport(location)
        }

        stand.equipment?.helmet = ItemStack(itemMaterial)
    }

    /** Removes the floating item at [position], if any. */
    fun hide(position: Position) {
        stands.remove(position)?.remove()
    }

    /** Removes every floating item this platform owns — called on plugin disable. */
    fun hideAll() {
        stands.keys.toList().forEach(::hide)
    }

    /** Nudges every active floating item's yaw forward by [degrees] — called on a fast repeating task to animate the spin. */
    fun spinAll(degrees: Float) {
        stands.values.forEach { stand ->
            if (!stand.isValid) return@forEach
            val location = stand.location
            location.yaw = (location.yaw + degrees) % 360f
            stand.teleport(location)
        }
    }

    private fun spawn(world: org.bukkit.World, position: Position): ArmorStand {
        val location = Location(world, position.x, position.y, position.z)
        return world.spawn(location, ArmorStand::class.java) { stand ->
            stand.isVisible = false
            stand.setGravity(false)
            stand.isMarker = true
            stand.isSmall = true
            stand.isInvulnerable = true
            stand.setBasePlate(false)
            // Not part of our own persisted state (ServerMobStorage), so it must not survive
            // into the world's chunk data either, same reasoning as BukkitMobHologram's stands.
            stand.isPersistent = false
        }
    }
}
