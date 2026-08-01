package de.polocloud.addon.bukkit

import de.polocloud.addon.ServerMob
import de.polocloud.addon.ServerMobPlatform
import de.polocloud.addon.util.Position
import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Villager
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

/**
 * Bukkit implementation of [ServerMobPlatform]. Every managed mob is a headless-AI
 * [Villager] — no external dependency needed for a mob that only has to stand still
 * and show a name tag.
 */
class BukkitServerMobPlatform(private val plugin: JavaPlugin) : ServerMobPlatform() {

    override val dataDirectory = plugin.dataFolder.toPath()

    private val entities = ConcurrentHashMap<Position, Villager>()
    private val positions = ConcurrentHashMap<Entity, Position>()

    override fun render(mob: ServerMob, services: List<Service>) {
        val existing = entities[mob.position]
        if (existing != null && !existing.isValid) {
            entities.remove(mob.position)
            positions.remove(existing)
        }

        val villager = entities[mob.position] ?: spawn(mob) ?: return

        val online = services.count { it.state == ServiceState.RUNNING }
        villager.customName = "§b${mob.group} §7(§a$online§7/§f${services.size}§7)"
        villager.isCustomNameVisible = true
    }

    override fun remove(mob: ServerMob) {
        entities.remove(mob.position)?.let {
            positions.remove(it)
            it.remove()
        }
    }

    override fun scheduleRepeating(intervalTicks: Long, task: () -> Unit) {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable(task), intervalTicks, intervalTicks)
    }

    /** The [ServerMob] position of a managed [entity], or `null` if it isn't one of ours. */
    fun positionOf(entity: Entity): Position? = positions[entity]

    private fun spawn(mob: ServerMob): Villager? {
        val world = Bukkit.getWorld(mob.position.world) ?: return null
        val location = Location(world, mob.position.x, mob.position.y, mob.position.z)

        val villager = world.spawn(location, Villager::class.java) { entity ->
            entity.setAI(false)
            entity.isInvulnerable = true
            entity.isSilent = true
            entity.isCollidable = false
            entity.setCanPickupItems(false)
            // Not part of our own persisted state (ServerMobStorage), so it must not survive
            // into the world's chunk data either — an unclean shutdown would otherwise leave
            // orphaned villagers behind that the next start's storage.load() never learns about.
            entity.isPersistent = false
        }

        entities[mob.position] = villager
        positions[villager] = mob.position
        return villager
    }
}
