package de.polocloud.addons.sign.system

import de.polocloud.addons.sign.system.layout.LayoutRegistry
import de.polocloud.addons.sign.system.layout.LayoutStorage
import de.polocloud.addons.sign.system.layout.SignPlaceholders
import de.polocloud.api.Polocloud
import de.polocloud.api.event.subscribe
import de.polocloud.shared.event.server.PlayerCountChangedEvent
import de.polocloud.shared.event.server.ServerStoppedEvent
import de.polocloud.shared.event.server.ServiceOnlineEvent
import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState
import java.util.concurrent.atomic.AtomicLong

/**
 * Platform-agnostic core of the sign system.
 *
 * A platform module (see [SignPlatform]) instantiates this once, calls [start] from
 * its enable hook and [stop] from its disable hook — mirroring how
 * [de.polocloud.bridge.BridgeBootstrap] wires the proxy bridge for Velocity/Waterfall.
 * All rendering goes through the injected [SignPlatform]/[SignEntryRenderer]s, so
 * this class never touches a concrete platform API directly.
 */
class SignSystem(private val platform: SignPlatform) {

    val registry = SignRegistry()
    val layouts = LayoutRegistry(LayoutStorage(platform.dataDirectory.resolve("layouts.json")))

    private val storage = SignStorage(platform.dataDirectory.resolve("signs.json"))
    private val tick = AtomicLong(0)

    /**
     * Loads persisted entries, immediately binds/renders whatever is already running
     * in the cluster (rather than only reacting to services that start afterwards),
     * then subscribes to the cluster events that keep everything live from here on.
     */
    fun start() {
        storage.load().forEach(registry::attach)

        Polocloud.serviceService.findAll().forEach(::bind)
        renderAll()

        // ServiceOnlineEvent, not ServerStartEvent: a sign must only bind to a service
        // that's actually reachable/RUNNING, and ServerStartEvent now fires much earlier
        // — before the service even has a port/host assigned.
        Polocloud.eventService.subscribe<ServiceOnlineEvent> { bind(it.service) }
        Polocloud.eventService.subscribe<ServerStoppedEvent> { unbind(it.service) }
        Polocloud.eventService.subscribe<PlayerCountChangedEvent> { refresh(it.service) }

        platform.scheduleRepeating(ANIMATION_INTERVAL_TICKS) {
            tick.incrementAndGet()
            renderAll()
        }
    }

    fun attach(type: SignEntryType, position: SignPosition, group: String, layoutId: String = "default") {
        val entry = SignEntry(type, position, group, layoutId)

        registry.attach(entry)
        storage.save(registry.all())
        bindIfPossible(entry)
    }

    fun detach(position: SignPosition): Boolean {
        val entry = registry.detach(position) ?: return false

        platform.renderer(entry.type)?.remove(entry)
        storage.save(registry.all())
        return true
    }

    /** Releases what this class itself owns; the scheduler and any live entities are the platform's responsibility. */
    fun stop() = layouts.stop()

    private fun bind(service: Service) {
        val entry = registry.findFree(service.group) ?: return
        entry.service = service
        render(entry)
    }

    /**
     * Binds [entry] to a running service of its group that isn't already showing on
     * another sign, if one exists — a service must never occupy two signs at once.
     */
    private fun bindIfPossible(entry: SignEntry) {
        if (entry.service != null) return
        val service = Polocloud.serviceService.findByGroup(entry.group)
            .firstOrNull { it.state == ServiceState.RUNNING && registry.boundTo(it.name()).isEmpty() }
        entry.service = service
        render(entry)
    }

    private fun unbind(service: Service) {
        registry.boundTo(service.name()).forEach { entry ->
            entry.service = null
            render(entry)
        }
    }

    private fun refresh(service: Service) {
        registry.boundTo(service.name()).forEach { entry ->
            entry.service = service
            render(entry)
        }
    }

    private fun renderAll() = registry.all().forEach(::render)

    private fun render(entry: SignEntry) {
        val renderer = platform.renderer(entry.type) ?: return
        val layout = layouts.find(entry.layoutId, entry.type) ?: return
        val state = entry.service?.state ?: ServiceState.UNKNOWN
        val frame = layout.animation(state).frameAt(tick.get()) ?: return

        renderer.render(entry, SignPlaceholders.apply(frame, entry))
    }

    private companion object {
        const val ANIMATION_INTERVAL_TICKS = 5L
    }
}