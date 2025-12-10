package dev.httpmarco.polocloud.agent.events

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.logger
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

        observer.setOnCancelHandler {
            remoteEvents[event]?.remove(subscription)
        }
    }

    fun dropServiceSubscriptions(service: Service) {
        remoteEvents.forEach { (_, subs) ->
            subs.removeIf {
                if (it.service == service) {
                    try {
                        it.sub.onCompleted()
                    } catch (e: Exception) {
                        logger.warn("Event stream already closed for service ${service.name()}: ${e.message}")
                    }
                    return@removeIf true;
                }
                return@removeIf false;
            }
        }
    }

    fun registeredAmount(): Int = remoteEvents.values.sumOf { it.size }

    override fun call(event: Event) {
        val eventName = event.javaClass.simpleName

        localSubscribers[eventName]?.forEach { it(event) }

        remoteEvents[eventName]?.forEach {
            if (!it.sub.isCancelled) {
                it.sub.onNext(
                    EventProviderOuterClass.EventContext.newBuilder()
                        .setEventName(eventName)
                        .setEventData(gsonSerializer.toJson(event))
                        .build()
                )
            }
        }
    }

    override fun <T : Event> subscribe(
        eventType: Class<T>,
        result: EventCallback<T>
    ) {
        val eventName = eventType.simpleName
        localSubscribers.computeIfAbsent(eventName) { mutableListOf() }
            .add { e -> result.call(e as T) }
    }
}