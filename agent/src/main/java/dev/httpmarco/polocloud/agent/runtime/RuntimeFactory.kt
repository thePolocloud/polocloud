package dev.httpmarco.polocloud.agent.runtime

import dev.httpmarco.polocloud.agent.services.Service

interface RuntimeFactory<out T : Service> {

    fun bootApplication(service: @UnsafeVariance T)

    fun shutdownApplication(service: @UnsafeVariance T)

}