package de.polocloud.addon.bukkit.hologram

import de.polocloud.addon.util.Position
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.concurrent.ConcurrentHashMap

/**
 * The optional floating item shown spinning above a server mob's [BukkitMobHologram], set via
 * `/servermobs set <group> [item]`.
 *
 * Uses an [ItemDisplay], not an armor stand wearing the item as a helmet (the more common
 * hack): a display entity renders exactly at the position it's placed at, with no hidden
 * head-bone offset an armor stand's equipment slot has — that offset was making the
 * configured hologram-to-item gap look about a block bigger than it actually was. It also
 * lets [SCALE] shrink the item arbitrarily via [Transformation], which an armor stand's fixed
 * small/normal size can't.
 */
object BukkitMobFloatingItem {

    /** Shrinks the floating item below its normal held/dropped size. */
    private const val SCALE = 0.3f

    /**
     * Whether the running server exposes [ItemDisplay] — added in Minecraft 1.19.4. This
     * addon's plugin.yml only declares `api-version: 1.13`, so it also loads fine on older
     * servers where the class (and [Transformation]/[Quaternionf] usage below) doesn't exist
     * at all; probing for it here up front lets [show] fail soft instead of the server
     * throwing a `NoClassDefFoundError` the first time a mob is given an item.
     */
    private val supported = runCatching { Class.forName("org.bukkit.entity.ItemDisplay") }.isSuccess

    private var warned = false

    private val displays = ConcurrentHashMap<Position, ItemDisplay>()
    private val yaws = ConcurrentHashMap<Position, Float>()

    /** Shows/updates the floating item at [position], [height] blocks above its feet. `null`/invalid [material] hides it instead. */
    fun show(position: Position, material: String?, height: Double) {
        if (!supported) {
            warnUnsupported()
            return
        }

        val itemMaterial = material?.let { runCatching { Material.valueOf(it) }.getOrNull() }
        if (itemMaterial == null) {
            hide(position)
            return
        }

        val world = Bukkit.getWorld(position.world) ?: return
        val display = displays[position] ?: spawn(world, position).also { displays[position] = it }

        val targetY = position.y + height
        if (display.location.y != targetY) {
            val location = display.location
            location.y = targetY
            display.teleport(location)
        }

        display.setItemStack(ItemStack(itemMaterial))
    }

    /** Removes the floating item at [position], if any. */
    fun hide(position: Position) {
        displays.remove(position)?.remove()
        yaws.remove(position)
    }

    /** Removes every floating item this platform owns — called on plugin disable. */
    fun hideAll() {
        displays.keys.toList().forEach(::hide)
    }

    /** Nudges every active floating item's yaw forward by [degrees] — called on a fast repeating task to animate the spin. */
    fun spinAll(degrees: Float) {
        displays.forEach { (position, display) ->
            if (!display.isValid) return@forEach

            val yaw = (yaws.getOrDefault(position, 0f) + degrees) % 360f
            yaws[position] = yaw

            val current = display.transformation
            display.transformation = Transformation(
                current.translation,
                Quaternionf().rotateY(Math.toRadians(yaw.toDouble()).toFloat()),
                current.scale,
                current.rightRotation,
            )
        }
    }

    /** Logs a one-time warning that this server can't run the floating-item feature, without disabling the addon itself. */
    private fun warnUnsupported() {
        if (warned) return
        warned = true
        Bukkit.getLogger().warning(
            "[ServerMobs] Floating items above mob holograms need ItemDisplay entities (Minecraft 1.19.4+); this server doesn't support them, so the feature is disabled."
        )
    }

    private fun spawn(world: World, position: Position): ItemDisplay {
        val location = Location(world, position.x, position.y, position.z)
        return world.spawn(location, ItemDisplay::class.java) { display ->
            display.setGravity(false)
            display.isInvulnerable = true
            // Not part of our own persisted state (ServerMobStorage), so it must not survive
            // into the world's chunk data either, same reasoning as BukkitMobHologram's stands.
            display.isPersistent = false
            display.billboard = Display.Billboard.FIXED
            display.transformation = Transformation(
                Vector3f(),
                Quaternionf(),
                Vector3f(SCALE, SCALE, SCALE),
                Quaternionf(),
            )
        }
    }
}
