package de.polocloud.node.module

import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.event.ClusterEventService
import de.polocloud.shared.event.EventCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Cluster-wide view of which node currently has which module loaded and whether it's
 * enabled there, built purely from [ModuleStatusEvent] broadcasts — this node's own (see
 * [ModuleManager.publishStatus]) and every peer's, relayed in exactly the same way any
 * other cluster event is (see [de.polocloud.node.event.ClusterEventRelay]).
 *
 * A single node with no peers still works: it just only ever sees its own events.
 *
 * An entry for a node that merely goes offline (crashes, restarts) is never actively
 * removed by an event — it just stops being updated, which is fine and intentional: a
 * crashed node's last-known status is still informative for a live "who's running this
 * right now" view. But once that node is gone from [NodeRepository] entirely (deleted,
 * e.g. by the periodic stale-node prune), its entries no longer describe anything real,
 * so [moduleNames] and [statusesFor] prune them against [NodeRepository] on every read.
 */
object ClusterModuleRegistry {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val eventName = EventCodec.nameOf(ModuleStatusEvent::class.java)
    private const val SUBSCRIBER_NAME = "module-status-registry"

    // moduleName -> nodeId -> latest status
    private val statuses = ConcurrentHashMap<String, ConcurrentHashMap<String, ModuleStatusEvent>>()

    private var job: Job? = null

    fun start() {
        if (job != null) return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        job = scope.launch {
            ClusterEventService.subscribe(eventName, SUBSCRIBER_NAME).collect { context ->
                (EventCodec.decode(context.eventName, context.eventData) as? ModuleStatusEvent)?.let(::apply)
            }
        }
    }

    fun stop() {
        ClusterEventService.unsubscribe(eventName, SUBSCRIBER_NAME)
        job?.cancel()
        job = null
    }

    fun publish(container: ModuleContainer, nodeId: String, nodeName: String) {
        runCatching {
            ClusterEventService.call(
                ModuleStatusEvent(
                    nodeId = nodeId,
                    nodeName = nodeName,
                    moduleName = container.descriptor.name,
                    moduleVersion = container.descriptor.version,
                    scope = container.descriptor.scope,
                    enabled = container.enabled,
                )
            )
        }.onFailure { logger.warn("Failed to publish module status for '{}': {}", container.descriptor.name, it.message) }
    }

    /** Names of every module this registry has seen a status report for, from any node still in the cluster. */
    fun moduleNames(): Set<String> {
        prune()
        return statuses.keys.toSet()
    }

    /** Every known node's latest status for [moduleName], most recently reporting node first isn't guaranteed — see [ModuleStatusEvent]. */
    fun statusesFor(moduleName: String): List<ModuleStatusEvent> {
        prune()
        return statuses[moduleName]?.values?.toList() ?: emptyList()
    }

    private fun apply(event: ModuleStatusEvent) {
        statuses.computeIfAbsent(event.moduleName) { ConcurrentHashMap() }[event.nodeId] = event
    }

    /** Drops entries for any node that no longer exists in [NodeRepository] at all — see the class doc for why merely-offline nodes are kept. */
    private fun prune() {
        val knownNodeIds = runCatching { NodeRepository.findAll().map { it.id.toString() }.toSet() }
            .getOrElse { return } // DB unreachable — better to show possibly-stale data than none at all
        for (perNode in statuses.values) {
            perNode.keys.retainAll(knownNodeIds)
        }
        statuses.entries.removeIf { it.value.isEmpty() }
    }
}
