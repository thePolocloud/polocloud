package de.polocloud.addon

import de.polocloud.addon.util.Position
import de.polocloud.api.Polocloud
import de.polocloud.api.event.subscribe
import de.polocloud.shared.event.server.PlayerCountChangedEvent
import de.polocloud.shared.event.server.ServerStoppedEvent
import de.polocloud.shared.event.server.ServiceOnlineEvent

/**
 * Platform-agnostic core of the server-mobs addon.
 *
 * A platform module (see [ServerMobPlatform]) instantiates this once, calls [start] from
 * its enable hook and [stop] from its disable hook — mirroring how
 * `SignSystem` wires the sign-system addon. All rendering goes through the injected
 * [ServerMobPlatform], so this class never touches a concrete platform API directly.
 */
class ServerMobAddon(private val platform: ServerMobPlatform) {

    val registry = ServerMobRegistry()

    private val storage = ServerMobStorage(platform.dataDirectory.resolve("mobs.json"))

    /**
     * Loads persisted mobs, immediately renders whatever is already running in the
     * cluster (rather than only reacting to services that change afterwards), then
     * subscribes to the cluster events that keep every mob's display live from here on.
     */
    fun start() {
        storage.load().forEach(registry::attach)
        renderAll()

        Polocloud.eventService.subscribe<ServiceOnlineEvent> { refresh(it.service.group) }
        Polocloud.eventService.subscribe<ServerStoppedEvent> { refresh(it.service.group) }
        Polocloud.eventService.subscribe<PlayerCountChangedEvent> { refresh(it.service.group) }

        platform.scheduleRepeating(REFRESH_INTERVAL_TICKS, ::renderAll)
    }

    /** Releases what this class itself owns; any live entities are the platform's responsibility. */
    fun stop() = registry.all().forEach(platform::remove)

    /** Spawns (or replaces, if one already stands at [position]) a mob bound to [group]. */
    fun set(group: String, position: Position, type: String = "VILLAGER"): ServerMob {
        registry.detach(position)?.let(platform::remove)

        val mob = ServerMob(group, position, type)
        registry.attach(mob)
        storage.save(registry.all())
        render(mob)
        return mob
    }

    /** Removes the mob at [position], if any. */
    fun remove(position: Position): Boolean {
        val mob = registry.detach(position) ?: return false
        platform.remove(mob)
        storage.save(registry.all())
        return true
    }

    private fun refresh(group: String) = registry.byGroup(group).forEach(::render)

    private fun renderAll() = registry.all().forEach(::render)

    private fun render(mob: ServerMob) {
        val services = Polocloud.serviceService.findByGroup(mob.group)
        platform.render(mob, services)
    }

    private companion object {
        // 5s: mirrors the online-player-count-esque cadence a name tag needs, without
        // hammering the node with a findByGroup call every single tick.
        const val REFRESH_INTERVAL_TICKS = 100L
    }
}
