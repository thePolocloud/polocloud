package de.polocloud.shared.event.server

import de.polocloud.shared.event.Event
import de.polocloud.shared.service.Service
import kotlinx.serialization.Serializable

/**
 * Fired by the node once a service has answered a Minecraft server list ping for the
 * first time, i.e. it is fully booted and ready to accept players — not merely spawned
 * as an OS process. See [de.polocloud.shared.service.ServiceState.RUNNING] and the node's
 * `ServicePingFactory.markOnline`, which is the sole place this is dispatched from.
 *
 * Distinct from [de.polocloud.shared.event.server.ServerStartEvent], which fires much
 * earlier — right at the beginning of the start process, before the service even has a
 * port/host assigned. Consumers that need "actually reachable" semantics (bridge,
 * sign-system) subscribe to this event, not that one.
 *
 * @param service the now-online service, including its address (host/port).
 */
@Serializable
data class ServiceOnlineEvent(
    val service: Service,
) : Event
