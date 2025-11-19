package dev.httpmarco.polocloud.bridge.api

import dev.httpmarco.polocloud.sdk.java.Polocloud
import dev.httpmarco.polocloud.shared.PolocloudShared
import dev.httpmarco.polocloud.shared.events.Event
import dev.httpmarco.polocloud.shared.events.definitions.service.ServiceChangeStateEvent
import dev.httpmarco.polocloud.shared.service.Service
import dev.httpmarco.polocloud.v1.GroupType
import dev.httpmarco.polocloud.v1.services.ServiceState

/**
 * Abstract class for bridging services to a proxy (e.g., Velocity, BungeeCord).
 * F = internal representation of registered server (e.g., RegisteredServer)
 * T = server info type used by the proxy (e.g., ServerInfo)
 */
abstract class BridgeInstance<F, T>(protected val polocloud: PolocloudShared = Polocloud.instance()) {

    /** List of registered fallback servers */
    val registeredFallbacks = hashMapOf<Service, F>()

    fun processBind() {
        // Register all currently online servers of type SERVER at startup
        polocloud.serviceProvider()
            .findByType(GroupType.SERVER)
            .filter { it.state == ServiceState.ONLINE }
            .forEach { registerNewServer(it) }

        // Subscribe to service state change events
        polocloud.eventProvider().subscribe(ServiceChangeStateEvent::class.java) {
            if (it.service.type == GroupType.SERVER) {
                handleServiceStateChange(it.service)
            }
        }
    }

    /**
     * Handle a service state change event.
     * - If a server goes ONLINE, register it.
     * - If a server is STOPPING, unregister it.
     */
    private fun handleServiceStateChange(service: Service) {
        when (service.state) {
            ServiceState.ONLINE -> if (service.type == GroupType.SERVER) registerNewServer(service)
            ServiceState.STOPPING -> unregisterServer(service)
            else -> {} // Other states can be ignored
        }
    }

    /**
     * Registers a new server with the proxy.
     * Adds it to fallback list if configured as fallback.
     */
    protected open fun registerNewServer(service: Service) {
        val serverInfo = registerServerInfo(generateServerInfo(service), service)

        if (isFallback(service)) {
            registeredFallbacks[service] = serverInfo
        }
    }

    /**
     * Unregisters a server from the proxy.
     * Removes it from the fallback list if necessary.
     */
    private fun unregisterServer(service: Service) {
        findServer(service.name())?.let { server ->
            unregister(server)
        }
        registeredFallbacks.remove(service)
    }

    /**
     * Updates a Polocloud player by firing the given event.
     */
    fun updatePolocloudPlayer(event: Event) {
        polocloud.eventProvider().call(event)
    }

    fun hasFallbacks(): Boolean {
        return registeredFallbacks.isNotEmpty()
    }

    /**
     * Determines if a service is configured as a fallback server.
     */
    protected fun isFallback(service: Service): Boolean {
        // TODO use new property system
        return service.properties["fallback"]?.equals("true", ignoreCase = true) == true
    }

    /**
     * Finds the fallback server with the fewest connected players.
     */
    fun findFallback(): F? {
        return registeredFallbacks.keys
            .filter { findServer(it.name()) != null }
            .sortedWith(
                compareBy(
                    { it.properties["fallbackPriority"]?.toIntOrNull() ?: Int.MAX_VALUE },
                    { playerCount(findServer(it.name())!!) }
                ))
            .firstOrNull()
            ?.let { registeredFallbacks[it] }
    }

    /**
     * Generate server info (proxy-specific) for a service.
     */
    abstract fun generateServerInfo(service: Service): T

    /**
     * Register server info with the proxy.
     * Returns the internal representation (F) of the registered server.
     */
    abstract fun registerServerInfo(identifier: T, service: Service): F

    /**
     * Unregister the given internal server representation from the proxy.
     */
    abstract fun unregister(identifier: F)

    /**
     * Find the internal server representation by its name.
     */
    abstract fun findServer(name: String): F?

    /**
     * Returns the current player count of the given server.
     */
    abstract fun playerCount(info: F): Int
}
