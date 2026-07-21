package de.polocloud.api.event

import de.polocloud.proto.EventContext
import de.polocloud.proto.EventProviderGrpcKt
import de.polocloud.proto.EventSubscribeRequest
import de.polocloud.shared.event.Event
import de.polocloud.shared.event.EventCodec
import de.polocloud.shared.event.EventRegistry
import io.grpc.ManagedChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

/**
 * Cluster-wide event bus client.
 *
 * Talks to the node's `EventProvider` gRPC service: each subscribed event type
 * opens a long-lived server stream, incoming [EventContext]s are decoded via the
 * shared [EventCodec] and dispatched to the registered listeners. [call] pushes a
 * local event to the node, which re-broadcasts it to every subscriber.
 *
 * Obtain the shared instance via [de.polocloud.api.Polocloud.eventService].
 *
 * ```kotlin
 * Polocloud.eventService.subscribe(ServerStartedEvent::class.java) { event ->
 *     println("started: ${event.serviceName}")
 * }
 * ```
 */
class EventService internal constructor(
    private val channelProvider: () -> ManagedChannel,
    /** This process's own service name, e.g. so a bridge plugin can tell whether an
     *  incoming request event is addressed to it. */
    val serviceName: String = resolveServiceName(),
) {

    private val listeners = ConcurrentHashMap<Class<out Event>, CopyOnWriteArrayList<Consumer<out Event>>>()
    private val streams = ConcurrentHashMap<String, Boolean>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun stub() = EventProviderGrpcKt.EventProviderCoroutineStub(channelProvider())

    /**
     * Registers [listener] for [type] and opens the cluster stream for it on first use.
     *
     * Also registers [type] in [EventRegistry] so events of this type can be decoded here
     * even if this process never [call]s one itself — e.g. a plugin that only listens for a
     * custom event another service publishes.
     */
    fun <T : Event> subscribe(type: Class<T>, listener: Consumer<T>) {
        EventRegistry.register(type)
        listeners.computeIfAbsent(type) { CopyOnWriteArrayList() }.add(listener)
        openStream(EventCodec.nameOf(type))
    }

    /** Removes a previously [subscribed][subscribe] listener. */
    fun <T : Event> unsubscribe(type: Class<T>, listener: Consumer<T>) {
        listeners[type]?.remove(listener)
    }

    /** Publishes [event] to the node, which broadcasts it to all subscribers. */
    fun <T : Event> call(event: T) {
        val encoded = EventCodec.encode(event)
        val context = EventContext.newBuilder()
            .setEventName(encoded.name)
            .setEventData(encoded.data)
            .build()
        scope.launch { runCatching { stub().call(context) } }
    }

    /** Stops all streams and releases the background scope. */
    fun close() = scope.cancel()

    private fun openStream(eventName: String) {
        // One stream per event name; putIfAbsent guards against concurrent subscribes.
        if (streams.putIfAbsent(eventName, true) != null) return

        scope.launch {
            val request = EventSubscribeRequest.newBuilder()
                .setEventName(eventName)
                .setServiceName(serviceName)
                .build()

            while (isActive) {
                try {
                    stub().subscribe(request).collect(::dispatch)
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    // Node restart / transient channel failure — back off and retry.
                    delay(RECONNECT_DELAY_MS)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dispatch(context: EventContext) {
        val event = EventCodec.decode(context.eventName, context.eventData) ?: return
        listeners[event.javaClass]?.forEach { (it as Consumer<Event>).accept(event) }
    }

    private companion object {
        const val RECONNECT_DELAY_MS = 3_000L

        /** Best-effort identifier of the service hosting this client, for node-side logging. */
        fun resolveServiceName(): String =
            System.getProperty("polocloud.service.name")
                ?: System.getenv("POLOCLOUD_SERVICE_NAME")
                ?: ""
    }
}

/** Kotlin-friendly [EventService.subscribe] that infers the event type. */
inline fun <reified T : Event> EventService.subscribe(listener: Consumer<T>) =
    subscribe(T::class.java, listener)
