package de.polocloud.node.services

import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.group.GroupRepository
import de.polocloud.node.services.factory.FactoryService
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.ping.ServicePingFactory
import de.polocloud.node.services.queue.CrashLoopGuard
import de.polocloud.node.services.queue.ServiceQueue
import de.polocloud.shared.event.server.ServerStoppedEvent
import de.polocloud.shared.service.ServiceState
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class ServiceProvider(
    nodePort: Int = 4241,
    // Host services advertise to the API (the node's reachable hostname).
    nodeHost: String = "127.0.0.1",
    /** Id of the node this provider runs on; attached to services in the API view. */
    val nodeId: String = "",
    /**
     * Shared with [de.polocloud.node.group.GroupService] so both agree on the same
     * loaded platform set (e.g. resolving whether a group's platform is a proxy for its
     * default templates) instead of each loading its own copy.
     */
    val platformService: PlatformService = PlatformService(),
) {

    private val logger = LoggerFactory.getLogger(ServiceProvider::class.java)

    // Concurrent by design: the queue and prune threads mutate this list while API
    // handlers iterate it. A plain ArrayList would risk ConcurrentModificationException
    // and torn reads; CopyOnWriteArrayList gives every reader a stable snapshot.
    val localServices = CopyOnWriteArrayList<LocalService>()

    /** Backs off placing new replicas of a group that keeps crashing right after start. */
    val crashLoopGuard = CrashLoopGuard()

    private val factory = FactoryService(platformService, this, nodePort, nodeHost)
    private val queue = ServiceQueue(factory, this)

    /** The node-wide forwarding secret shared by every service this node starts. */
    val forwardingHandler get() = factory.forwardingHandler

    // Pings starting services and flips them to RUNNING once they are reachable.
    private val pingFactory = ServicePingFactory(this)

    fun run() {
        platformService.load()
        reconcileStaleServices()
        queue.run()
        pingFactory.run()
    }

    /**
     * Drops every row this node itself owns in the `services` table before it starts
     * placing anything of its own.
     *
     * Peers are queried live over gRPC for their own local services
     * ([de.polocloud.node.services.cluster.PeerServiceQuery]), never through this table, so
     * on a fresh start — with [localServices] still empty — every row already tagged with
     * this node's [nodeId] is necessarily left over from a previous run of this same node
     * that never reached [shutdown] (a forceful kill during development, an OOM-kill, a
     * container restart). Without this, those rows linger forever showing a stale `RUNNING`
     * state, and the scaling queue undercounts how many replicas are actually needed since it
     * only counts [localServices], causing it to place new replicas on top of names/ports the
     * stale rows still claim.
     *
     * Scoped to [nodeId] rather than dropping the whole table: the backend can be a database
     * shared by several nodes (see [ServiceRepository.findAllForNode]), and an unscoped wipe
     * here would delete other nodes' currently-running services out from under them.
     */
    private fun reconcileStaleServices() {
        ServiceRepository.findAllForNode(nodeId).forEach { stale ->
            runCatching { ServiceRepository.delete(stale) }
        }
    }

    fun shutdown() {
        runCatching { pingFactory.close() }
        runCatching { queue.close() }

        // Isolate each service: one that hangs or throws must not stop the rest
        // from being terminated.
        this.localServices.forEach { service ->
            runCatching { service.shutdown() }
        }
        this.localServices.clear()
    }

    fun find(name: String) : Service? {
        val index = name.substringAfterLast('-', "").toIntOrNull()
        if (index != null) {
            val groupName = name.substringBeforeLast('-')
            val narrowed = ServiceRepository.findByGroup(groupName).firstOrNull { it.serviceIndex == index }
            if (narrowed != null) return narrowed
        }
        return findAll().firstOrNull { it.name().equals(name, ignoreCase = true) }
    }

    fun findAll() = ServiceRepository.findAll()

    fun exists(name: String) = find(name) != null

    fun update(service: Service) {
        ServiceRepository.save(service)
    }

    /** Persists the current state of [service] (port/host/state) to the database. */
    fun persist(service: Service) {
        ServiceRepository.save(service)
    }

    /** Removes [service] from the database (e.g. once its process has exited). */
    fun remove(service: Service) {
        ServiceRepository.delete(service)
    }

    /** The live [LocalService] with the given `group-index` [name], or `null` if not running here. */
    fun findLocal(name: String): LocalService? =
        localServices.firstOrNull { it.name().equals(name, ignoreCase = true) }

    /** Returns whether this call actually performed cleanup (`false` if a concurrent caller already had). */
    fun shutdownLocal(service: LocalService): Boolean {
        // A thrown exception is not the same as "a concurrent caller already handled this" —
        // only a clean `false` (the CAS guard in LocalService.shutdown) means skip.
        val alreadyHandledElsewhere = !runCatching { service.shutdown() }.getOrDefault(true)
        if (alreadyHandledElsewhere) return false
        localServices.remove(service)
        ClusterEventService.call(ServerStoppedEvent(ServiceEventMapper.toShared(service)))
        return true
    }

    /**
     * Stops every running service of [groupName] and drops any of its still-queued
     * services. Used when a group is deleted so it leaves no orphaned processes behind.
     */
    fun shutdownGroup(groupName: String) {
        queue.removeGroup(groupName)
        localServices
            .filter { it.groupName.equals(groupName, ignoreCase = true) }
            .forEach { shutdownLocal(it) }
    }

    /** Runs [command] in every running service of [groupName] on this node. Returns how many it was actually sent to. */
    fun executeGroupCommand(groupName: String, command: String): Int =
        localServices
            .filter { it.groupName.equals(groupName, ignoreCase = true) }
            .count { it.executeCommand(command) }

    /**
     * Stops [service]'s process and immediately starts a fresh replica in its place, with
     * the same group and index. Unlike a plain [shutdownLocal] — which only gets replaced
     * once the scaling queue's next tick notices the group is under `minOnline` — this
     * always brings the same instance count straight back, regardless of `minOnline`/
     * `maxOnline`. No-op (returns `false`) if a concurrent caller already shut this
     * instance down, or the group no longer exists.
     */
    fun restartLocal(service: LocalService): Boolean {
        val group = GroupRepository.find(service.groupName)
        if (group == null) {
            logger.warn("Cannot restart {}: group '{}' no longer exists", service.name(), service.groupName)
            return false
        }
        val index = service.serviceIndex
        if (!shutdownLocal(service)) return false

        val replacement = LocalService(
            Service(UUID.randomUUID(), index, group.name, ServiceState.QUEUED, "127.0.0.1", -1, nodeId)
        )
        update(replacement)
        factory.start(replacement, group)
        return true
    }

    /** Restarts every running service of [groupName] on this node (see [restartLocal]). Returns how many were restarted. */
    fun restartGroup(groupName: String): Int =
        localServices
            .filter { it.groupName.equals(groupName, ignoreCase = true) }
            .count { restartLocal(it) }
}
