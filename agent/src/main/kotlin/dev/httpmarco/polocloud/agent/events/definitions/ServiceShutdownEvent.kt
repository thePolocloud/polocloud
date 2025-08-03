package dev.httpmarco.polocloud.agent.events.definitions

import dev.httpmarco.polocloud.agent.events.Event
import dev.httpmarco.polocloud.agent.services.Service

class ServiceShutdownEvent(private val service: Service) : Event {
}