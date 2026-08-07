package de.polocloud.node.services

import de.polocloud.proto.ServiceLogLine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Builds the log stream for a co-located [LocalService]: its buffered recent lines
 * followed by its live output. Shared by the CLI-oriented
 * [de.polocloud.node.communication.impl.services.ServiceManagerImpl] and the SDK-facing
 * [de.polocloud.node.communication.impl.services.ServiceApiServiceImpl] — both expose the
 * same underlying stream, just over different gRPC services.
 */
object LocalServiceLogStreaming {

    fun stream(local: LocalService): Flow<ServiceLogLine> = callbackFlow {
        local.recentLogs().forEach { line -> trySend(ServiceLogLine.newBuilder().setLine(line).build()) }

        val listener: (String) -> Unit = { line -> trySend(ServiceLogLine.newBuilder().setLine(line).build()) }
        local.addLogListener(listener)

        awaitClose { local.removeLogListener(listener) }
    }
}
