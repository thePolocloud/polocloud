package de.polocloud.api

import de.polocloud.api.connection.PolocloudConnection
import de.polocloud.api.event.EventService
import de.polocloud.api.group.GroupApiClient
import de.polocloud.api.group.GroupService
import de.polocloud.api.group.GrpcGroupApiClient
import de.polocloud.api.services.GrpcServiceApiClient
import de.polocloud.api.services.ServiceApiClient
import de.polocloud.api.services.ServiceService

/**
 * Standalone entry point to the PoloCloud API.
 *
 * A service or plugin uses this to talk back to the node over mTLS:
 * ```kotlin
 * val groups = Polocloud.groupService.findAll()
 * ```
 *
 * The underlying gRPC channel is opened lazily on the first call, so referencing
 * [Polocloud] does not require a provisioned identity until an API call is made.
 */
object Polocloud {

    private val connection = PolocloudConnection()

    private val groupClient: GroupApiClient = GrpcGroupApiClient { connection.channel() }
    private val serviceClient: ServiceApiClient = GrpcServiceApiClient { connection.channel() }

    /** Access to the cluster's groups (`findAll`, `find`, …) — see [GroupService] for the blocking/async split. */
    val groupService = GroupService(groupClient)

    /** Access to the cluster's services (`findAll`, `find`, …) — see [ServiceService] for the blocking/async split. */
    val serviceService = ServiceService(serviceClient)

    /**
     * Cluster-wide event bus. Subscribe to cloud events such as
     * [de.polocloud.shared.event.server.ServiceOnlineEvent].
     */
    val eventService = EventService(channelProvider = { connection.channel() })

    /**
     * Closes the underlying connection. A subsequent API call re-opens it.
     */
    fun close() {
        eventService.close()
        serviceService.close()
        groupService.close()
        connection.close()
    }
}
