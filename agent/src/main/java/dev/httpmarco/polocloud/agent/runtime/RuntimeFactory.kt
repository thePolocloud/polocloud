package dev.httpmarco.polocloud.agent.runtime

import dev.httpmarco.polocloud.agent.services.Service

interface RuntimeFactory {

    fun bootApplication(service: Service)

    fun shutdownApplication(service: Service)

}