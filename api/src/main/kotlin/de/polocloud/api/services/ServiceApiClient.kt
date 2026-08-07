package de.polocloud.api.services

import de.polocloud.proto.ServiceData
import kotlinx.coroutines.flow.Flow

/**
 * Transport-agnostic gateway to the node's service API.
 *
 * Implemented by [GrpcServiceApiClient] for real gRPC traffic; abstracted as an
 * interface so [ServiceService] can be unit-tested without a live node.
 */
interface ServiceApiClient {

    suspend fun findServices(groupFilter: String?, stateFilter: String?): List<ServiceData>

    /** Number of services matching the filters, without transferring their full data. */
    suspend fun countServices(groupFilter: String?, stateFilter: String?): Int

    /** Stops the running service named [name]. Returns `false` (with a reason) if it isn't currently running. */
    suspend fun stopService(name: String): ServiceCommandResult

    /** Runs [command] in the console of the running service named [name]. */
    suspend fun executeServiceCommand(name: String, command: String): ServiceCommandResult

    /** Re-applies template [templateName] onto the running service named [name]'s work directory. */
    suspend fun copyTemplate(name: String, templateName: String): ServiceCommandResult

    /** The buffered recent log lines of the running service named [name], followed by its live output. */
    fun streamServiceLogs(name: String): Flow<String>
}

/** Outcome of a [ServiceApiClient.stopService]/[ServiceApiClient.executeServiceCommand] call. */
data class ServiceCommandResult(val success: Boolean, val message: String)
