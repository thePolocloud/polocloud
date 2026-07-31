package de.polocloud.shared.event.server

import de.polocloud.shared.event.Event
import de.polocloud.shared.service.Service
import kotlinx.serialization.Serializable

/**
 * Fired by the node right at the beginning of a service's start process — before the
 * platform is resolved, its port/host assigned, or its process launched. This is a
 * "start requested/begun" signal, not a readiness one: [service]'s address/state
 * fields are whatever the service happened to be at that moment (typically still
 * `QUEUED`, with no port/host assigned yet).
 *
 * For "this service is actually reachable" semantics, subscribe to
 * [de.polocloud.shared.event.server.ServiceOnlineEvent] instead, which fires once the
 * service has answered a Minecraft server list ping for the first time.
 *
 * @param service the starting service, as of the moment the start process began.
 */
@Serializable
data class ServerStartEvent(
    val service: Service,
) : Event
