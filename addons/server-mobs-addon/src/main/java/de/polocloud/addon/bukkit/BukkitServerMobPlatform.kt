package de.polocloud.addon.bukkit

import de.polocloud.addon.ServerMob
import de.polocloud.addon.ServerMobPlaceholders
import de.polocloud.addon.ServerMobPlatform
import de.polocloud.addon.bukkit.hologram.BukkitMobFloatingItem
import de.polocloud.addon.bukkit.hologram.BukkitMobHologram
import de.polocloud.addon.config.ReloadableConfig
import de.polocloud.addon.display.MobDisplay
import de.polocloud.addon.util.Position
import de.polocloud.shared.service.Service
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Villager
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Bukkit implementation of [ServerMobPlatform]. Every managed mob is a headless-AI
 * [Villager] — no external dependency needed for a mob that only has to stand still,
 * show a [BukkitMobHologram] above itself, and turn to face nearby players.
 */
class BukkitServerMobPlatform(
    private val plugin: JavaPlugin,
    private val display: ReloadableConfig<MobDisplay>,
) : ServerMobPlatform() {

    override val dataDirectory = plugin.dataFolder.toPath()

    private val entities = ConcurrentHashMap<Position, Villager>()
    private val positions = ConcurrentHashMap<Entity, Position>()

    private var lookTask: BukkitTask? = null
    private var spinTask: BukkitTask? = null

    override fun render(mob: ServerMob, services: List<Service>) {
        val existing = entities[mob.position]
        if (existing != null && !existing.isValid) {
            entities.remove(mob.position)
            positions.remove(existing)
        }

        entities[mob.position] ?: spawn(mob) ?: return

        val hologram = display.current().hologram
        if (!hologram.enabled) {
            BukkitMobHologram.hide(mob.position)
            BukkitMobFloatingItem.hide(mob.position)
            return
        }

        val lines = hologram.lines.map { ServerMobPlaceholders.group(it, mob.group, services) }
        BukkitMobHologram.show(mob.position, lines, hologram)

        // (lines.size - 1) * lineSpacing lands exactly on the topmost line (index 0 is highest,
        // see BukkitMobHologram.spawnStands) — the gap is then added on top of that.
        val itemHeight = hologram.heightOffset + (lines.size - 1) * hologram.lineSpacing + FLOATING_ITEM_GAP
        BukkitMobFloatingItem.show(mob.position, mob.item, itemHeight)
    }

    override fun remove(mob: ServerMob) {
        BukkitMobHologram.hide(mob.position)
        BukkitMobFloatingItem.hide(mob.position)
        entities.remove(mob.position)?.let {
            positions.remove(it)
            it.remove()
        }
    }

    override fun scheduleRepeating(intervalTicks: Long, task: () -> Unit) {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable(task), intervalTicks, intervalTicks)
    }

    /** Starts periodically turning every managed mob towards the nearest nearby player. Idempotent. */
    fun startLookTask() {
        if (lookTask != null) return

        val intervalTicks = display.current().look.intervalTicks
        lookTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable(::lookAtNearestPlayers), intervalTicks, intervalTicks)
    }

    fun stopLookTask() {
        lookTask?.cancel()
        lookTask = null
    }

    /** Starts spinning every mob's floating item ([BukkitMobFloatingItem]), if any. Idempotent. */
    fun startSpinTask() {
        if (spinTask != null) return

        spinTask = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { BukkitMobFloatingItem.spinAll(SPIN_DEGREES_PER_TICK) },
            SPIN_INTERVAL_TICKS,
            SPIN_INTERVAL_TICKS,
        )
    }

    fun stopSpinTask() {
        spinTask?.cancel()
        spinTask = null
    }

    /** Removes every hologram/floating item this platform owns — called from the bootstrap's `onDisable`, mirroring their own `hideAll` contracts. */
    fun clearHolograms() {
        BukkitMobHologram.hideAll()
        BukkitMobFloatingItem.hideAll()
    }

    /** The [ServerMob] position of a managed [entity], or `null` if it isn't one of ours. */
    fun positionOf(entity: Entity): Position? = positions[entity]

    private fun lookAtNearestPlayers() {
        val look = display.current().look
        if (!look.enabled) return

        entities.values.forEach { villager ->
            if (!villager.isValid) return@forEach

            val nearest = villager.world.players
                .filter { it.location.distanceSquared(villager.location) <= look.radius * look.radius }
                .minByOrNull { it.location.distanceSquared(villager.location) }
                ?: return@forEach

            lookAt(villager, nearest.eyeLocation)
        }
    }

    /** Rotates [entity] (body yaw + pitch — its AI is disabled, so nothing else drives its rotation) towards [target]. */
    private fun lookAt(entity: LivingEntity, target: Location) {
        val origin = entity.eyeLocation
        val dx = target.x - origin.x
        val dy = target.y - origin.y
        val dz = target.z - origin.z
        val distanceXZ = sqrt(dx * dx + dz * dz)

        val location = entity.location
        location.yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        location.pitch = Math.toDegrees(-atan2(dy, distanceXZ)).toFloat()
        entity.teleport(location)
    }

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

    private companion object {
        /** Blocks between the topmost hologram line and a mob's floating item, if it has one. */
        const val FLOATING_ITEM_GAP = 0.4
        const val SPIN_INTERVAL_TICKS = 2L
        const val SPIN_DEGREES_PER_TICK = 6f
    }
}
