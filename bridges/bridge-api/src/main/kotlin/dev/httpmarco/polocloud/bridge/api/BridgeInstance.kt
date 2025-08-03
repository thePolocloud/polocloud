package dev.httpmarco.polocloud.bridge.api

import dev.httpmarco.polocloud.shared.events.definitions.ServiceOnlineEvent
import dev.httpmarco.polocloud.shared.events.definitions.ServiceShutdownEvent
import dev.httpmarco.polocloud.shared.polocloudShared
import dev.httpmarco.polocloud.v1.GroupType

abstract class BridgeInstance<T> {

    abstract fun generateInfo(name: String, hostname: String, port: Int): T

    abstract fun registerService(identifier: T, fallback: Boolean = false)

    abstract fun unregisterService(identifier: T)

    abstract fun findInfo(name: String): T?

    init {
        // it is bad, but if sdk ist present, we can use it
        Class.forName("dev.httpmarco.polocloud.sdk.kotlin.Polocloud")
    }

    fun initialize() {
        // TODO register all services that are already online

        polocloudShared.eventProvider().subscribe(ServiceOnlineEvent::class) {
            val service = it.service

            if (service.type == GroupType.SERVER) {
                val fallback = service.properties["fallback"]?.equals("true", ignoreCase = true) == true
                registerService(generateInfo(service.name(), service.hostname, service.port), fallback)
            }
        }

        polocloudShared.eventProvider().subscribe(ServiceShutdownEvent::class) {
            findInfo(it.service.name())?.let { info ->
                unregisterService(info)
            }!!
        }
    }
}