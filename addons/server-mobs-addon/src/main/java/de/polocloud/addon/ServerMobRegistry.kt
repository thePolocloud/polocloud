package de.polocloud.addon

import de.polocloud.addon.util.Position
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe collection of every [ServerMob] known to this platform instance.
 *
 * Backed by a [CopyOnWriteArrayList]: entries are attached/detached rarely (a player
 * running `/servermobs set`) but read constantly (every refresh tick, every cluster
 * event), so optimising for cheap, lock-free reads is the right trade-off.
 */
class ServerMobRegistry {

    private val mobs = CopyOnWriteArrayList<ServerMob>()

    fun attach(mob: ServerMob) {
        mobs += mob
    }

    fun detach(position: Position): ServerMob? {
        val mob = at(position) ?: return null
        mobs.remove(mob)
        return mob
    }

    fun all(): List<ServerMob> = mobs

    /** The mob attached at [position], if any. */
    fun at(position: Position): ServerMob? = mobs.firstOrNull { it.position == position }

    /** Every mob attached to [group]. */
    fun byGroup(group: String): List<ServerMob> = mobs.filter { it.group.equals(group, ignoreCase = true) }
}
