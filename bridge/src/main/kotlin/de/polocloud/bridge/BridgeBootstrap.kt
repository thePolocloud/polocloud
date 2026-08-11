package de.polocloud.bridge

import de.polocloud.api.Polocloud
import de.polocloud.api.event.subscribe
import de.polocloud.api.group.GroupFilterType
import de.polocloud.shared.event.group.GroupUpdatedEvent
import de.polocloud.shared.event.server.ServerStoppedEvent
import de.polocloud.shared.event.server.ServiceOnlineEvent
import de.polocloud.shared.property.Properties
import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Shared entry point for the Velocity bridge plugin.
 *
 * Keeps the platform-specific plugin class thin: it only translates the
 * platform lifecycle into a single [start] call and forwards logging. All actual
 * work goes through the shipped Polocloud [api][Polocloud].
 *
 * A Waterfall/BungeeCord bridge variant is not available yet.
 */
class BridgeBootstrap<T>(private val instance: BridgeInstance<T>) {

    // Names of groups currently flagged as fallback targets, mapped to their priority
    // (higher tried first). Kept live via GroupUpdatedEvent.
    private val fallbackGroups: MutableMap<String, Int> = ConcurrentHashMap()

    // Services currently registered on this proxy — used to pick a fallback on connect.
    private val registeredServices = CopyOnWriteArrayList<Service>()

    /**
     * Boots the bridge on the given [platform].
     *
     * @param platform human-readable platform name, used only for logging.
     * @param log      platform logger adapter.
     */
    fun start(platform: String, log: (String) -> Unit) {
        log("Polocloud bridge starting on $platform ...")

        // Touch the shipped API so a misconfigured classpath fails loudly and early
        // rather than on the first cloud interaction later on.
        runCatching { Polocloud.groupService }
            .onSuccess { log("Polocloud bridge ready — API linked successfully") }
            .onFailure { log("Polocloud bridge failed to initialise the API: ${it.message}") }

        // Seed the fallback-group set from the current cluster state.
        runCatching {
            Polocloud.groupService.findAll()
                .filter { it.isFallback() }
                .forEach { fallbackGroups[it.name.lowercase()] = it.fallbackPriority() }
        }

        // Register everything already running when this proxy boots.
        Polocloud.serviceService.findAll().forEach(::registerIfEligible)

        // Then keep the registry in sync: the node pushes a lifecycle event whenever a
        // server starts or stops, so services that come and go after boot are added and
        // removed on the proxy instead of only reflecting the boot-time snapshot.
        // ServiceOnlineEvent (not ServerStartEvent, which fires much earlier — before the
        // service even has a port/host) is what's needed here: registering a service on
        // the proxy requires a real, reachable address.
        Polocloud.eventService.subscribe<ServiceOnlineEvent> { event ->
            log("Server online in cluster: ${event.service.name()} (group: ${event.service.group})")
            registerIfEligible(event.service)
        }
        Polocloud.eventService.subscribe<ServerStoppedEvent> { event ->
            val service = event.service
            log("Server stopped in cluster: ${service.name()} (group: ${service.group})")
            registeredServices.removeIf { it.name().equals(service.name(), ignoreCase = true) }
            instance.unregisterService(instance.mapService(service), service)
        }

        // Keep the fallback-group set current when group properties change at runtime.
        Polocloud.eventService.subscribe<GroupUpdatedEvent> { event ->
            val isFallback = event.properties.getBoolean(Properties.FALLBACK)
            if (isFallback) {
                fallbackGroups[event.name.lowercase()] = event.properties.getInt(Properties.FALLBACK_PRIORITY)
            } else {
                fallbackGroups -= event.name.lowercase()
            }
            log("Group ${event.name} fallback=${isFallback}")
        }
    }

    /**
     * Registers [service] on this proxy, unless it belongs to a proxy group itself —
     * a proxy only ever registers backend (sub-)servers, never other proxies.
     */
    private fun registerIfEligible(service: Service) {
        val group = Polocloud.groupService.find(service.group)
        if (group == null || GroupFilterType.PROXY.matches(group.platform)) return

        registeredServices.removeIf { it.name().equals(service.name(), ignoreCase = true) }
        registeredServices += service
        instance.registerService(instance.mapService(service), service)
    }

    /**
     * Picks the best running fallback service to route a player to, or `null` if none
     * is available — a proxy must never send a player anywhere else, so a `null`
     * result means the player cannot be connected/redirected at all.
     *
     * See [FallbackSelector] for the selection rules (priority tiering, least players).
     *
     * @param excludeServiceName a service name to skip, e.g. the server a player was
     *   just kicked from, so they are never redirected right back to it.
     */
    fun bestFallback(excludeServiceName: String? = null): Service? =
        FallbackSelector.select(registeredServices, fallbackGroups, excludeServiceName)

    /**
     * Releases the underlying node connection. Called from the platform shutdown hook.
     */
    fun stop() {
        runCatching { Polocloud.close() }
    }
}