package de.polocloud.addons.sign.system.spigot.renderer

import de.polocloud.addons.sign.system.SignPosition
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.block.BlockState

/**
 * The [BlockState] of type [T] at this position, or `null` if the world isn't loaded or the
 * block there isn't a [T] (e.g. it was broken/replaced since the entry was attached).
 *
 * Shared by every [de.polocloud.addons.sign.system.SignEntryRenderer] that targets a real
 * block ([BukkitSignRenderer], [BukkitBannerRenderer]) instead of each resolving
 * world → block → block state on its own.
 */
inline fun <reified T : BlockState> SignPosition.blockStateAt(): T? {
    val bukkitWorld = Bukkit.getWorld(world) ?: return null
    return bukkitWorld.getBlockAt(x, y, z).state as? T
}

/** The [SignPosition] this block occupies. */
fun Block.toSignPosition(): SignPosition = SignPosition(x, y, z, world.name)

/** The [SignPosition] this block state occupies. */
fun BlockState.toSignPosition(): SignPosition = SignPosition(x, y, z, world.name)
