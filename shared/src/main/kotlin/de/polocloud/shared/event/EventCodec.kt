package de.polocloud.shared.event

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.concurrent.ConcurrentHashMap

/** An [Event] reduced to its wire form: a stable [name] plus JSON [data]. */
data class EncodedEvent(val name: String, val data: String)

/**
 * Serializes [Event]s to/from the `(eventName, eventData)` pair carried by
 * [de.polocloud.proto.EventContext].
 *
 * Works for any `@Serializable` [Event], not just a hand-maintained set: the [KSerializer]
 * is resolved reflectively from the event's own runtime class rather than looked up in a
 * closed map, and [encode] registers that class into [EventRegistry] as a side effect so a
 * later [decode] of the same name — in this JVM, e.g. a service that both fires and listens
 * for its own custom event — resolves correctly. A JVM that only *subscribes* to a type
 * gets the same registration via [de.polocloud.api.event.EventService.subscribe].
 *
 * Shared by the node (encode before broadcast) and the api (decode on receive)
 * so both ends agree on the exact wire representation.
 */
object EventCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // KClass.serializer() walks the class's generated Companion via reflection; cache the
    // result per class so hot events (e.g. player-count updates) don't pay for that repeatedly.
    private val serializers = ConcurrentHashMap<Class<out Event>, KSerializer<Event>>()

    /** The wire name used for events of the given [type]. */
    fun nameOf(type: Class<out Event>): String = type.simpleName

    /**
     * Encodes [event] into its wire form and registers its class in [EventRegistry].
     *
     * @throws kotlinx.serialization.SerializationException if [event]'s class isn't `@Serializable`.
     */
    fun encode(event: Event): EncodedEvent {
        val type = event.javaClass
        EventRegistry.register(type)
        return EncodedEvent(type.simpleName, json.encodeToString(serializerFor(type), event))
    }

    /** Decodes an event from its wire form, or `null` if [name] is unregistered in this JVM. */
    fun decode(name: String, data: String): Event? {
        val type = EventRegistry.classFor(name) ?: return null
        return json.decodeFromString(serializerFor(type), data)
    }

    @Suppress("UNCHECKED_CAST")
    @OptIn(InternalSerializationApi::class)
    private fun serializerFor(type: Class<out Event>): KSerializer<Event> =
        serializers.computeIfAbsent(type) { it.kotlin.serializer() as KSerializer<Event> }
}
