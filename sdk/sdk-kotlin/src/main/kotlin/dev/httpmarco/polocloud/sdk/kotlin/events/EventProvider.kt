package dev.httpmarco.polocloud.sdk.kotlin.events

import com.google.gson.GsonBuilder
import dev.httpmarco.polocloud.sdk.kotlin.Polocloud
import dev.httpmarco.polocloud.shared.events.Event
import dev.httpmarco.polocloud.shared.events.EventProvider
import dev.httpmarco.polocloud.v1.proto.EventProviderGrpc
import dev.httpmarco.polocloud.v1.proto.EventProviderOuterClass
import io.grpc.ManagedChannel
import io.grpc.stub.StreamObserver
import kotlin.reflect.KClass

class EventProvider(channel: ManagedChannel?) : EventProvider {

    private val eventStub = EventProviderGrpc.newStub(channel)

    override fun call(event: Event) {
        TODO("Not yet implemented")
    }

    override fun <T : Event> subscribe(eventType: KClass<T>, result: (T) -> Any) {
        val request = EventProviderOuterClass.EventSubscribeRequest.newBuilder()
            .setServiceName(Polocloud.instance().selfServiceName())
            .setEventName(eventType.simpleName)
            .build()

        eventStub.subscribe(request, object : StreamObserver<EventProviderOuterClass.EventContext> {
            override fun onNext(conetext: EventProviderOuterClass.EventContext) {
                result(GsonBuilder().create().fromJson(conetext.eventData, eventType.java))
            }

            override fun onError(p0: Throwable) {
                System.err.println("Error while subscribing to event: ${p0.message}")
                p0.printStackTrace()
            }

            override fun onCompleted() {
                // No action needed on completion
            }
        })
    }
}