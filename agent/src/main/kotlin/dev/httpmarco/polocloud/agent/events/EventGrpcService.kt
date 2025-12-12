package dev.httpmarco.polocloud.agent.events

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.shared.events.Event
import dev.httpmarco.polocloud.v1.proto.EventProviderGrpc
import dev.httpmarco.polocloud.v1.proto.EventProviderOuterClass
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver

class EventGrpcService : EventProviderGrpc.EventProviderImplBase() {

    override fun subscribe(
        request: EventProviderOuterClass.EventSubscribeRequest,
        responseObserver: StreamObserver<EventProviderOuterClass.EventContext>
    ) {
        Agent.eventService.attach(
            request.eventName,
            request.serviceName,
            responseObserver as ServerCallStreamObserver<EventProviderOuterClass.EventContext>
        )
    }

    override fun call(
        request: EventProviderOuterClass.EventContext,
        responseObserver: StreamObserver<EventProviderOuterClass.CallEventResponse>
    ) {
        if (isCallCancelled(responseObserver)) {
            return
        }

        val response = processEvent(request)
        safeRespond(responseObserver, response)
    }

    private fun processEvent(request: EventProviderOuterClass.EventContext): EventProviderOuterClass.CallEventResponse {
        return try {
            val fqcn = "dev.httpmarco.polocloud.shared.events.definitions.${request.eventName}"
            val eventClass = Class.forName(fqcn)
            val eventObj = Agent.eventService.gsonSerializer
                .fromJson(request.eventData, eventClass) as Event

            Agent.eventService.call(eventObj)

            EventProviderOuterClass.CallEventResponse.newBuilder()
                .setSuccess(true)
                .build()
        } catch (e: Exception) {
            EventProviderOuterClass.CallEventResponse.newBuilder()
                .setSuccess(false)
                .setMessage(e.message ?: "Unknown error")
                .build()
        }
    }

    private fun isCallCancelled(observer: StreamObserver<*>): Boolean {
        return observer is ServerCallStreamObserver && observer.isCancelled
    }

    private fun safeRespond(
        observer: StreamObserver<EventProviderOuterClass.CallEventResponse>,
        response: EventProviderOuterClass.CallEventResponse
    ) {
        if (!isCallCancelled(observer)) {
            observer.onNext(response)
            observer.onCompleted()
        }
    }
}