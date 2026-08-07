package de.polocloud.api.services

import de.polocloud.api.Polocloud
import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.function.Consumer

/**
 * Public, blocking entry point to the service API.
 *
 * Backed by a [ServiceApiClient] (gRPC in production). Obtain the shared instance
 * via [de.polocloud.api.Polocloud.serviceService].
 *
 * The results are cluster-wide: the connected node aggregates its own local services
 * with every other online node's local services before responding, so a proxy or
 * plugin only ever needs to talk to one node to see the whole cluster (see
 * `FindServicesServerHandler` on the node side).
 *
 * Calls run on [Dispatchers.IO] rather than the caller's thread: callers such as
 * the proxy bridge invoke these from a platform lifecycle thread, and confining
 * the suspending gRPC call to that thread can deadlock when the same thread is
 * needed to resume the response continuation.
 */
class ServiceService internal constructor(
    private val client: ServiceApiClient,
) {

    // Backs streamLogs' background subscriptions, mirroring EventService's own scope.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** All services currently known to the connected node. */
    fun findAll(): List<Service> =
        runBlocking(Dispatchers.IO) { client.findServices(null, null) }.map(ServiceMapper::toApi)

    /** The service with the given `group-index` [name], or `null` if none matches. */
    fun find(name: String): Service? =
        findAll().firstOrNull { it.name().equals(name, ignoreCase = true) }

    /** All services belonging to [group]. */
    fun findByGroup(group: String): List<Service> =
        runBlocking(Dispatchers.IO) { client.findServices(group, null) }.map(ServiceMapper::toApi)

    /** All services currently in [state] (e.g. [ServiceState.RUNNING]). */
    fun findByState(state: ServiceState): List<Service> =
        runBlocking(Dispatchers.IO) { client.findServices(null, state.name) }.map(ServiceMapper::toApi)

    /** Number of services currently known to the connected node. */
    fun count(): Int = runBlocking(Dispatchers.IO) { client.countServices(null, null) }

    /** Number of services belonging to [group]. */
    fun count(group: String): Int = runBlocking(Dispatchers.IO) { client.countServices(group, null) }

    /**
     * Stops the running service named [name], regardless of which node it's running on.
     *
     * Prefer [Service.shutdown] when a [Service] instance is already at hand.
     *
     * @return `false` if no service named [name] is currently running anywhere in the cluster.
     */
    fun stop(name: String): Boolean = runBlocking(Dispatchers.IO) { client.stopService(name) }.success

    /**
     * Runs [command] in the console of the running service named [name], regardless of
     * which node it's running on.
     *
     * Prefer [Service.execute] when a [Service] instance is already at hand.
     *
     * @return `false` if no service named [name] is currently running anywhere in the cluster.
     */
    fun executeCommand(name: String, command: String): Boolean =
        runBlocking(Dispatchers.IO) { client.executeServiceCommand(name, command) }.success

    /**
     * Re-applies template [templateName] onto the running service named [name]'s work
     * directory, regardless of which node it's running on.
     *
     * Prefer [Service.copyTemplate] when a [Service] instance is already at hand.
     *
     * @return `false` if no service named [name] is currently running anywhere in the cluster.
     */
    fun copyTemplate(name: String, templateName: String): Boolean =
        runBlocking(Dispatchers.IO) { client.copyTemplate(name, templateName) }.success

    /**
     * Streams the console output of the running service named [name] to [listener] —
     * first its recent buffered log lines, then every new line as it's produced.
     *
     * Prefer [Service.streamLogs] when a [Service] instance is already at hand.
     *
     * @return an [AutoCloseable] that ends the subscription; otherwise it keeps running
     * for as long as this [ServiceService] (i.e. [de.polocloud.api.Polocloud]) is open.
     */
    fun streamLogs(name: String, listener: Consumer<String>): AutoCloseable {
        val job = scope.launch {
            runCatching { client.streamServiceLogs(name).collect(listener::accept) }
        }
        return AutoCloseable { job.cancel() }
    }

    /** Ends every active [streamLogs] subscription and releases background resources. */
    fun close() = scope.cancel()
}

/**
 * Stops this service, regardless of which node it's running on.
 *
 * @return `false` if it's no longer running anywhere in the cluster.
 */
fun Service.shutdown(): Boolean = Polocloud.serviceService.stop(name())

/**
 * Runs [command] in this service's console, regardless of which node it's running on.
 *
 * @return `false` if it's no longer running anywhere in the cluster.
 */
fun Service.execute(command: String): Boolean = Polocloud.serviceService.executeCommand(name(), command)

/**
 * Re-applies template [templateName] onto this service's work directory, regardless of
 * which node it's running on.
 *
 * @return `false` if it's no longer running anywhere in the cluster.
 */
fun Service.copyTemplate(templateName: String): Boolean = Polocloud.serviceService.copyTemplate(name(), templateName)

/** Streams this service's console output to [listener]. See [ServiceService.streamLogs]. */
fun Service.streamLogs(listener: Consumer<String>): AutoCloseable = Polocloud.serviceService.streamLogs(name(), listener)
