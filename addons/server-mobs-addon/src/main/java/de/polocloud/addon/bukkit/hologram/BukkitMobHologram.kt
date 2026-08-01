package de.polocloud.addon.bukkit.hologram

import de.polocloud.addon.display.HologramConfig
import de.polocloud.addon.util.Position
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import java.util.concurrent.ConcurrentHashMap

/**
 * A small, dependency-free hologram floating above a server mob: one invisible, name-tagged
 * marker [ArmorStand] per text line. Copies the exact approach of
 * [de.polocloud.addons.sign.system.spigot.renderer.hologram.BukkitHologram] from the sign-system
 * addon — reusing that class directly isn't possible since it's keyed by that addon's own
 * `SignPosition`/module, and this addon only depends on plain `spigot-api`, same as that one.
 *
 * Entities are reused across [show] calls at the same position — only their custom name is
 * updated — rather than respawned every refresh, which would otherwise flicker on every
 * [de.polocloud.addon.ServerMobAddon] render tick. They're only replaced when the number of
 * lines actually changes.
 */
object BukkitMobHologram {

    private val holograms = ConcurrentHashMap<Position, MutableList<ArmorStand>>()

    /** Shows/updates [lines] as a hologram above [position], spawning it on first use. */
    fun show(position: Position, lines: List<String>, settings: HologramConfig) {
        if (lines.isEmpty()) {
            hide(position)
            return
        }

        val world = Bukkit.getWorld(position.world) ?: return
        val existing = holograms[position]

        val stands = if (existing != null && existing.size == lines.size) {
            existing
        } else {
            existing?.forEach { it.remove() }
            spawnStands(world, position, lines.size, settings).also { holograms[position] = it }
        }

        stands.forEachIndexed { index, stand ->
            stand.customName = lines[index]
            stand.isCustomNameVisible = true
        }
    }

    /** Removes the hologram at [position], if any. */
    fun hide(position: Position) {
        holograms.remove(position)?.forEach { it.remove() }
    }

    /** Removes every hologram this platform instance owns — called on plugin disable so no armor stand outlives it. */
    fun hideAll() {
        holograms.keys.toList().forEach(::hide)
    }

    /** Spawns [count] stacked marker armor stands above [position], first line highest. */
    private fun spawnStands(world: World, position: Position, count: Int, settings: HologramConfig): MutableList<ArmorStand> =
        (0 until count).map { index ->
            val y = position.y + settings.heightOffset + (count - 1 - index) * settings.lineSpacing
            val location = Location(world, position.x, y, position.z)
            world.spawn(location, ArmorStand::class.java) { stand ->
                stand.isVisible = false
                stand.setGravity(false)
                stand.isMarker = true
                stand.isCustomNameVisible = true
                stand.isInvulnerable = true
                // Not part of our own persisted state (ServerMobStorage), so it must not survive
                // into the world's chunk data either — an unclean shutdown would otherwise leave
                // orphaned armor stands behind that hideAll()/hide() never learn about.
                stand.isPersistent = false
            }
        }.toMutableList()
}
