package de.polocloud.shared.service

import de.polocloud.shared.property.Properties
import de.polocloud.shared.property.PropertyHolder
import kotlinx.serialization.Serializable

/**
 * A service in the cluster, as exposed through the public API and carried on
 * cluster events.
 *
 * Lives in `shared` (not `api`) so both the node (which publishes it on
 * [de.polocloud.shared.event.server.ServerStartedEvent] /
 * [de.polocloud.shared.event.server.ServerStoppedEvent]) and the api/bridge
 * (which consume it) can use the exact same type without depending on each other.
 *
 * Obtain instances via [de.polocloud.api.Polocloud.serviceService] or from the
 * server lifecycle events.
 */
@Serializable
data class Service(
    val id: String,
    val index: Int,
    val group: String,
    val state: ServiceState,
    val port: Int,
    /** Host the service is reachable on, e.g. `127.0.0.1`. */
    val host: String,
    val pid: Long,
    /**
     * CPU usage percent (0-100) and resident memory (MB) of this service's process, as
     * last sampled by the owning node. Both `0.0` until the first sample. Read-only: only
     * the node's resource sampler updates these.
     */
    val cpuUsage: Double = 0.0,
    val usedMemory: Double = 0.0,
    /**
     * Players currently connected / configured player slots, as last reported by the
     * node's Minecraft Server List Ping. `0` until the first successful ping. Read-only:
     * only the node's ping loop updates these, there is no way to set them from the API.
     */
    val onlinePlayers: Int = 0,
    val maxPlayers: Int = 0,
    /**
     * MOTD text from the node's last successful Minecraft Server List Ping. Empty
     * until the first successful ping. Read-only: only the node's ping loop updates this.
     */
    val motd: String = "",
    /** Free-form key/value properties attached to this service. */
    override val properties: Properties = Properties(),
) : PropertyHolder() {

    /** Cluster-wide service name, e.g. `lobby-1`. */
    fun name() = "$group-$index"
}