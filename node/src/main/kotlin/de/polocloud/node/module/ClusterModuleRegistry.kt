package de.polocloud.node.module

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
 * Known gap: an entry for a node that goes offline is never actively removed — it just
 * stops being updated. Fine for a live "who's running this right now" view (a crashed
 * node's last-known status is still informative), but [statusesFor] can return stale
 * entries for a node that's no longer in the cluster at all.
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

    /** Names of every module this registry has seen a status report for, from any node. */
    fun moduleNames(): Set<String> = statuses.keys.toSet()

    /** Every known node's latest status for [moduleName], most recently reporting node first isn't guaranteed — see [ModuleStatusEvent]. */
    fun statusesFor(moduleName: String): List<ModuleStatusEvent> = statuses[moduleName]?.values?.toList() ?: emptyList()

    private fun apply(event: ModuleStatusEvent) {
        statuses.computeIfAbsent(event.moduleName) { ConcurrentHashMap() }[event.nodeId] = event
    }
}
