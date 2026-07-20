package de.polocloud.shared.event

/**
 * Marker interface for everything that can be dispatched through the cluster
 * event bus.
 *
 * Concrete events are `@Serializable` data classes carrying the relevant payload, e.g.
 * [de.polocloud.shared.event.server.ServerStartedEvent]. The built-in events live in
 * `shared` so both the node (producer) and the api/bridge (consumer) can use them without
 * depending on each other — but a plugin/addon is free to define its own `@Serializable`
 * [Event] subclass anywhere on its own classpath and fire/subscribe to it exactly the same
 * way: [EventCodec] and [EventRegistry] resolve it reflectively, no change to this module
 * required. See [EventCodec] for how (de)serialization works and [EventRegistry] for how a
 * name is resolved back to a class on each end of the wire.
 */
interface Event
