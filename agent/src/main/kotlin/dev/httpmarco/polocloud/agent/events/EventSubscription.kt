package dev.httpmarco.polocloud.agent.events

import dev.httpmarco.polocloud.agent.services.Service
import dev.httpmarco.polocloud.v1.proto.EventProviderOuterClass
import io.grpc.stub.StreamObserver

class EventSubscription(val service: Service, val sub : StreamObserver<EventProviderOuterClass.EventContext>) {

}