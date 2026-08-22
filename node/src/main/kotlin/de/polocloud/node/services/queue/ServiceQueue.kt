package de.polocloud.node.services.queue

import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.group.Group
import de.polocloud.node.group.GroupRepository
import de.polocloud.node.services.LocalService
import de.polocloud.node.services.Service
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.NodePeerServiceQuery
import de.polocloud.node.services.cluster.PeerServiceQuery
import de.polocloud.proto.NodeState
import de.polocloud.shared.service.ServiceState
import de.polocloud.node.services.factory.FactoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

private val SERVICE_START_DISPATCHER = Dispatchers.IO.limitedParallelism(4)

class ServiceQueue(
    private val factory: FactoryService,
    private val serviceProvider: ServiceProvider,
    // Injectable so the round-robin/eligibility logic can be unit-tested without a
    // real database — default to the real repositories in production.
    private val groups: () -> List<Group> = { GroupRepository.findAll() },
    private val onlineNodes: () -> List<NodeData> = {
        runCatching { NodeRepository.find(NodeState.ONLINE) }.getOrDefault(emptyList())
    },
    private val peerQuery: PeerServiceQuery = NodePeerServiceQuery(),
    private val loadProvider: NodeLoadProvider = HeartbeatNodeLoadProvider,
) {

    private lateinit var thread: Thread
    private val logger = LoggerFactory.getLogger(ServiceQueue::class.java)
    private val queue: Queue<Pair<LocalService, Group>> = LinkedList()

    fun run() {
        thread = Thread({
            while (!Thread.currentThread().isInterrupted) {
                try {
                    tick()
                    Thread.sleep(2000)
                } catch (_: InterruptedException) {
                    // Interrupted by close() during shutdown — exit the loop quietly.
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    logger.error("Service queue tick failed", e)
                }
            }
        }, "service-queue")
        thread.isDaemon = true
        thread.start()
        logger.info("Service queue started")
    }

    fun close() {
        thread.interrupt()
    }

    /**
     * Drops every still-queued service of [groupName] (e.g. when the group is deleted),
     * so no new services of a removed group get started — and deletes the DB row
     * [enqueueRequired] already persisted for each one (a queued service is written to
     * the database as soon as it's queued, not only once it starts), so a group delete
     * right after enqueuing doesn't leave rows behind that would trip the groups table's
     * foreign-key constraint.
     *
     * Synchronised on the queue because it is invoked from the terminal/gRPC thread while
     * the queue thread also mutates the list.
     */
    fun removeGroup(groupName: String) {
        val removed = synchronized(queue) {
            val matching = queue.filter { it.second.name.equals(groupName, ignoreCase = true) }
            queue.removeIf { it.second.name.equals(groupName, ignoreCase = true) }
            matching
        }
        removed.forEach { (service, _) ->
            runCatching { serviceProvider.remove(service) }.onFailure {
                logger.warn("Failed to delete queued service {} (id={}) from the database: {}", service.name(), service.id, it.message)
            }
        }
    }

    private fun tick() {
        enqueueRequired()
        drainQueue()
    }

    /** The groups and indexes currently queued, e.g. `lobby-1`. Exposed for testing. */
    internal fun queuedIndexes(groupName: String): List<Int> = synchronized(queue) {
        queue.filter { it.second.name == groupName }.map { it.first.serviceIndex }
    }

    /** Runs a single `enqueueRequired` pass without starting the background thread. Exposed for testing. */
    internal fun enqueueRequiredForTest() = enqueueRequired()

    private fun enqueueRequired() {
        val allGroups = groups()
        if (allGroups.isEmpty()) return

        val online = onlineNodes()
        val groupMemoryMb = allGroups.associate { it.name to it.memory }
        val usedMemoryMb = nodeMemoryUsage(online, groupMemoryMb).toMutableMap()

        for (group in allGroups) {
            val eligible = GroupNodeEligibility.eligibleOnlineNodes(group, online).sortedBy { it.id.toString() }
            if (eligible.isEmpty()) continue

            // This node isn't (or is no longer) allowed to run this group — leave it to
            // whichever node(s) are actually eligible.
            val self = eligible.firstOrNull { it.id.toString() == serviceProvider.nodeId } ?: continue

            // A group crashing repeatedly right after start is backed off by
            // CrashLoopGuard (fed from FactoryService's exit hook) — skip placing more of
            // it until the backoff window elapses, instead of restarting it as fast as
            // start+detect-exit allows.
            if (serviceProvider.crashLoopGuard.isBackingOff(group.name)) continue

            val cluster = clusterState(group, eligible)
            val queued = synchronized(queue) { queue.count { it.second.name == group.name } }.toLong()
            val minOnlineNeeded = (group.minOnline - cluster.running - queued).coerceAtLeast(0)
            val capacityLeft = (group.maxOnline - cluster.running - queued).coerceAtLeast(0)

            // Load-based scale-up: once every currently-running instance's aggregate
            // occupancy (players / configured slots, both cluster-wide) reaches
            // startThreshold, grow by one more — capped at maxOnline via capacityLeft.
            // Only considered once at least one instance has actually reported real
            // slots (cluster.maxPlayers > 0); before the first successful ping (or with
            // no instances running yet) there's nothing to measure load from, and a
            // startThreshold of 0.0 (the un-configured default — see GroupBuilder) opts
            // a group out of this entirely rather than scaling it to maxOnline at 0 load.
            val occupancy = if (cluster.maxPlayers > 0) cluster.onlinePlayers.toDouble() / cluster.maxPlayers else 0.0
            val loadTriggered = capacityLeft > 0 && group.startThreshold > 0.0 &&
                cluster.maxPlayers > 0 && occupancy >= group.startThreshold
            val clusterNeeded = maxOf(minOnlineNeeded, if (loadTriggered) 1L else 0L).coerceAtMost(capacityLeft)

            if (clusterNeeded <= 0) continue

            val assignment = assignReplicas(eligible, cluster.perNodeRunning, usedMemoryMb, group.memory, clusterNeeded.toInt())
            val myShare = assignment[self.name()] ?: 0
            if (myShare <= 0) continue

            usedMemoryMb[self.name()] = (usedMemoryMb[self.name()] ?: 0) + myShare * group.memory

            if (loadTriggered && minOnlineNeeded <= 0) {
                logger.info(
                    "Group '{}' is at {}/{} players ({}% >= {}% threshold) — starting {} more service(s) cluster-wide (this node: {}), capped at maxOnline: {}",
                    group.name, cluster.onlinePlayers, cluster.maxPlayers,
                    "%.1f".format(occupancy * 100), "%.1f".format(group.startThreshold * 100),
                    clusterNeeded, myShare, group.maxOnline
                )
            } else {
                logger.info(
                    "Group '{}' needs {} more service(s) cluster-wide (this node: {}) — minOnline: {}, cluster running: {}, queued: {}",
                    group.name, clusterNeeded, myShare, group.minOnline, cluster.running, queued
                )
            }
            if (assignment.values.sum() < clusterNeeded.toInt()) {
                logger.warn(
                    "Group '{}' could only place {}/{} needed replica(s) cluster-wide — every eligible node is at its memory capacity",
                    group.name, assignment.values.sum(), clusterNeeded
                )
            }
            repeat(myShare) {
                val index = nextIndex(group, cluster.usedIndexes)
                val service = LocalService(
                    Service(UUID.randomUUID(), index, group.name, ServiceState.QUEUED, "127.0.0.1", -1, serviceProvider.nodeId)
                )

                serviceProvider.update(service)
                synchronized(queue) { queue.offer(Pair(service, group)) }
                logger.info("Queued {}-{} [memory: {}MB, platform: {}/{}]",
                    group.name, index, group.memory, group.platform, group.version
                )
            }
        }
    }

    /**
     * Total memory (MB), across every group, that each online node currently runs
     * locally — used to enforce [NodeData.maxMemory] as a hard placement cap. Computed
     * once per tick via the same peer-query mechanism as [clusterState], but scoped to
     * all groups at once (blank plan filter) instead of one node fetch per group.
     */
    private fun nodeMemoryUsage(online: List<NodeData>, groupMemoryMb: Map<String, Int>): Map<String, Int> {
        val self = online.firstOrNull { it.id.toString() == serviceProvider.nodeId }
        val localUsed = serviceProvider.localServices.sumOf { groupMemoryMb[it.groupName] ?: 0 }

        val others = online.filter { it.id.toString() != serviceProvider.nodeId }
        val remoteUsed = if (others.isEmpty()) emptyMap() else runBlocking {
            coroutineScope {
                others.map { node ->
                    async {
                        val used = runCatching { peerQuery.localServicesOf(node, "") }
                            .onFailure { logger.warn("Failed to query services from node {}: {}", node.name(), it.message) }
                            .getOrDefault(emptyList())
                            .sumOf { service -> groupMemoryMb[service.plan] ?: 0 }
                        node.name() to used
                    }
                }.awaitAll().toMap()
            }
        }

        return buildMap {
            self?.let { put(it.name(), localUsed) }
            putAll(remoteUsed)
        }
    }

    /**
     * [perNodeRunning] is keyed by [NodeData.name] and only covers eligible nodes.
     * [onlinePlayers]/[maxPlayers] are summed across every currently running instance of
     * the group, cluster-wide, as last reported by each node's Server List Ping —
     * see [de.polocloud.node.services.LocalService.onlinePlayers].
     */
    private data class ClusterState(
        val running: Long,
        val usedIndexes: Set<Int>,
        val perNodeRunning: Map<String, Int>,
        val onlinePlayers: Int,
        val maxPlayers: Int,
    )

    /**
     * Aggregates [group]'s running-service count, used indexes, per-node breakdown and
     * player occupancy across every node in [eligible] besides this one, via the same
     * peer-query mechanism the cluster-wide service listing handlers use
     * ([de.polocloud.node.services.cluster.PeerServiceQuery]). A slow or unreachable peer
     * is skipped rather than failing the whole tick, at the cost of briefly
     * under-counting that peer's services.
     */
    private fun clusterState(group: Group, eligible: List<NodeData>): ClusterState {
        val self = eligible.firstOrNull { it.id.toString() == serviceProvider.nodeId }
        val localRunning = factory.runningCount(group.name)
        val localIndexes = factory.runningIndexes(group.name)
        val localGroupServices = serviceProvider.localServices.filter { it.groupName == group.name }
        val localOnlinePlayers = localGroupServices.sumOf { it.onlinePlayers }
        val localMaxPlayers = localGroupServices.sumOf { it.maxPlayers }

        val others = eligible.filter { it.id.toString() != serviceProvider.nodeId }
        if (others.isEmpty()) {
            val perNodeRunning = self?.let { mapOf(it.name() to localRunning.toInt()) } ?: emptyMap()
            return ClusterState(localRunning, localIndexes, perNodeRunning, localOnlinePlayers, localMaxPlayers)
        }

        val remote = runBlocking {
            coroutineScope {
                others.map { node ->
                    async {
                        node to runCatching { peerQuery.localServicesOf(node, group.name) }
                            .onFailure { logger.warn("Failed to query services from node {}: {}", node.name(), it.message) }
                            .getOrDefault(emptyList())
                    }
                }.awaitAll()
            }
        }

        val perNodeRunning = buildMap {
            self?.let { put(it.name(), localRunning.toInt()) }
            remote.forEach { (node, services) -> put(node.name(), services.size) }
        }

        return ClusterState(
            localRunning + remote.sumOf { it.second.size },
            localIndexes + remote.flatMap { it.second }.map { it.index }.toSet(),
            perNodeRunning,
            localOnlinePlayers + remote.sumOf { (_, services) -> services.sumOf { it.onlinePlayers } },
            localMaxPlayers + remote.sumOf { (_, services) -> services.sumOf { it.maxPlayers } },
        )
    }
    
    private fun assignReplicas(
        eligible: List<NodeData>,
        perNodeRunning: Map<String, Int>,
        usedMemoryMb: Map<String, Int>,
        groupMemoryMb: Int,
        count: Int,
    ): Map<String, Int> {
        val runningSim = perNodeRunning.toMutableMap()
        val memorySim = usedMemoryMb.toMutableMap()
        val assigned = mutableMapOf<String, Int>()

        fun hasCapacity(node: NodeData): Boolean =
            node.maxMemory <= 0 || (memorySim[node.name()] ?: 0) + groupMemoryMb <= node.maxMemory

        repeat(count) {
            val pick = eligible.filter(::hasCapacity).minWithOrNull(
                compareBy(
                    { node: NodeData -> loadProvider.loadOf(node) },
                    { node: NodeData -> runningSim[node.name()] ?: 0 },
                    { node: NodeData -> node.id.toString() },
                )
            ) ?: return@repeat

            assigned[pick.name()] = (assigned[pick.name()] ?: 0) + 1
            runningSim[pick.name()] = (runningSim[pick.name()] ?: 0) + 1
            memorySim[pick.name()] = (memorySim[pick.name()] ?: 0) + groupMemoryMb
        }
        return assigned
    }

    private fun drainQueue() {
        val batch = synchronized(queue) { generateSequence { queue.poll() }.toList() }
        if (batch.isEmpty()) return

        runBlocking {
            batch.map { (service, group) ->
                async(SERVICE_START_DISPATCHER) { startOne(service, group) }
            }.awaitAll()
        }
    }

    private fun startOne(service: LocalService, group: Group) {
        logger.info("Starting {}-{} [memory: {}MB, platform: {}/{}]", group.name, service.serviceIndex, group.memory, group.platform, group.version)
        try {
            factory.start(service, group)
        } catch (e: Exception) {
            logger.error("Failed to start {}-{}: {}", group.name, service.serviceIndex, e.message)
        }
    }

    /** [clusterOtherIndexes] are indexes already used by eligible peers, so two nodes never pick the same one. */
    private fun nextIndex(group: Group, clusterOtherIndexes: Set<Int>): Int {
        val queuedIndexes = synchronized(queue) {
            queue.filter { it.second.name == group.name }.map { it.first.serviceIndex }.toSet()
        }
        val usedIndexes = queuedIndexes + clusterOtherIndexes
        var index = 1
        while (index in usedIndexes) index++
        return index
    }
}
