package dev.httpmarco.polocloud.agent.events

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.shutdownProcess
import dev.httpmarco.polocloud.shared.events.Event
import dev.httpmarco.polocloud.shared.events.EventCallback
import dev.httpmarco.polocloud.shared.events.SharedEventProvider
import dev.httpmarco.polocloud.shared.service.Service
import dev.httpmarco.polocloud.v1.proto.EventProviderOuterClass
import io.grpc.stub.ServerCallStreamObserver
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class EventService : SharedEventProvider() {

    private val remoteEvents = ConcurrentHashMap<String, MutableList<EventSubscription>>()
    private val localSubscribers = ConcurrentHashMap<String, MutableList<(Event) -> Unit>>()

    /**
     * Attach a gRPC subscriber to a specific event.
     */
    fun attach(
        event: String,
        serviceName: String,
        observer: ServerCallStreamObserver<EventProviderOuterClass.EventContext>
    ) {
        val service = Agent.runtime.serviceStorage().find(serviceName)

        if (service == null) {
            i18n.warn("agent.events.service.not-found", serviceName, event)
            observer.onCompleted()
            return
        }

        val subscription = EventSubscription(service, observer)
        remoteEvents.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(subscription)

        // Remove subscription on cancel
        observer.setOnCancelHandler {
            remoteEvents[event]?.remove(subscription)
        }
    }

    /**
     * Remove all subscriptions for a given service.
     */
    fun dropServiceSubscriptions(service: Service) {
        remoteEvents.forEach { (_, subs) ->
            subs.removeIf {
                if (it.service.name() == service.name()) {
                    try {
                        it.sub.onCompleted()
                    } catch (e: Exception) {
                        logger.warn("Event stream already closed for service ${service.name()}: ${e.message}")
                    }
                    return@removeIf true
                }
                false
            }
        }
    }

    /**
     * Count all registered remote subscriptions.
     */
    fun registeredAmount(): Int = remoteEvents.values.sumOf { it.size }

    /**
     * Call an event, notifying local and remote subscribers.
     */
    override fun call(event: Event) {
        val eventName = event.javaClass.simpleName

        // Call local subscribers safely
        localSubscribers[eventName]?.forEach { subscriber ->
            runCatching { subscriber(event) }
        }

        remoteEvents[eventName]?.forEach { subscription ->
            val sub = subscription.sub

            // Skip if stream cancelled or shutdown in progress
            if (sub.isCancelled || shutdownProcess()) return@forEach

            try {
                sub.onNext(
                    EventProviderOuterClass.EventContext.newBuilder()
                        .setEventName(eventName)
                        .setEventData(gsonSerializer.toJson(event))
                        .build()
                )
            } catch (e: IllegalStateException) {
                // Stream already closed – log only if not shutting down
                if (!shutdownProcess()) {
                    logger.warn(
                        "gRPC stream already closed while sending event {} for service {}",
                        eventName,
                        subscription.service.name(),
                        e
                    )
                }
            } catch (e: Exception) {
                // Catch any other unexpected exceptions
                logger.error(
                    "Unexpected exception while sending event {} to service {}",
                    eventName,
                    subscription.service.name(),
                    e
                )
            }
        }
    }

    /**
     * Subscribe a local callback to an event.
     */
    override fun <T : Event> subscribe(
        eventType: Class<T>,
        result: EventCallback<T>
    ) {
        val eventName = eventType.simpleName
        localSubscribers.computeIfAbsent(eventName) { CopyOnWriteArrayList() }
            .add { e -> runCatching { result.call(e as T) } }
    }
}
