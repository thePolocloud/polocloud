package dev.httpmarco.polocloud.agent.events.definitions

import dev.httpmarco.polocloud.agent.events.Event
import dev.httpmarco.polocloud.agent.services.Service

class ServiceOnlineEvent(private val service: Service) : Event {

}