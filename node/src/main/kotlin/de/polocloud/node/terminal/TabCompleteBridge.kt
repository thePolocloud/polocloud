package de.polocloud.node.terminal

import de.polocloud.node.event.ClusterEventService
import de.polocloud.shared.event.EventCodec
import de.polocloud.shared.event.EventRegistry
import de.polocloud.shared.event.terminal.TabCompleteRequestEvent
import de.polocloud.shared.event.terminal.TabCompleteResponseEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Node-side half of the cluster's tab-completion protocol: asks whichever service/plugin is
 * registered under a given service name to suggest console completions, addressed and
 * correlated via [TabCompleteRequestEvent]/[TabCompleteResponseEvent] over the existing
 * [ClusterEventService] cluster event bus (the same one carrying e.g. `ServerStartedEvent`),
 * so it works whether the target service runs on this node or a peer one.
 *
 * Support is entirely opt-in on the answering side — currently only the Velocity bridge
 * plugin listens for [TabCompleteRequestEvent] and answers it (see `VelocityBridgePlugin`).
 * A service whose platform never answers just leaves the request to time out, which
 * [requestCompletions] turns into an empty suggestion list rather than an error, so
 * `service <name> screen` degrades to plain typing for platforms without support.
 */
object TabCompleteBridge {

    private const val TIMEOUT_MILLIS = 300L

    private val pending = ConcurrentHashMap<String, CompletableDeferred<List<String>>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // This JVM only ever decodes responses (never encodes one itself), so unlike a
        // request — auto-registered as a side effect of encoding it in requestCompletions —
        // the response type needs an explicit registration for EventCodec.decode to resolve it.
        EventRegistry.register(TabCompleteResponseEvent::class.java)
        scope.launch {
            ClusterEventService.subscribe(EventCodec.nameOf(TabCompleteResponseEvent::class.java), "node")
                .collect { context ->
                    val event = EventCodec.decode(context.eventName, context.eventData) as? TabCompleteResponseEvent
                        ?: return@collect
                    pending.remove(event.requestId)?.complete(event.suggestions)
                }
        }
    }

    /**
     * Asks [serviceName] for completions of [buffer] (the console input typed so far),
     * waiting up to [TIMEOUT_MILLIS] for a reply. Returns an empty list if nothing answers
     * in time.
     */
    suspend fun requestCompletions(serviceName: String, buffer: String): List<String> {
        val requestId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<List<String>>()
        pending[requestId] = deferred
        try {
            ClusterEventService.call(TabCompleteRequestEvent(requestId, serviceName, buffer))
            return withTimeoutOrNull(TIMEOUT_MILLIS) { deferred.await() } ?: emptyList()
        } finally {
            pending.remove(requestId)
        }
    }
}