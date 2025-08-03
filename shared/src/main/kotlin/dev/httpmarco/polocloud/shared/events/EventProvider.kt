package dev.httpmarco.polocloud.shared.events

import kotlin.reflect.KClass

interface EventProvider {

    fun call(event: Event)

    fun <T : Event> subscribe(eventType: KClass<T>, result: (T) -> Any)

}