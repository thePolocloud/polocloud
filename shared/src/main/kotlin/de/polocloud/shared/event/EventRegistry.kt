package de.polocloud.shared.event

import de.polocloud.shared.event.group.GroupUpdatedEvent
import de.polocloud.shared.event.player.PlayerDisconnectEvent
import de.polocloud.shared.event.player.PlayerJoinEvent
import de.polocloud.shared.event.player.PlayerKickEvent
import de.polocloud.shared.event.player.PlayerKickRequestEvent
import de.polocloud.shared.event.player.PlayerSendRequestEvent
import de.polocloud.shared.event.player.PlayerSwitchEvent
import de.polocloud.shared.event.server.PlayerCountChangedEvent
import de.polocloud.shared.event.server.ServerStartEvent
import de.polocloud.shared.event.server.ServerStoppedEvent
import de.polocloud.shared.event.server.ServiceOnlineEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * Maps a wire event name — an [Event]'s simple class name, as carried by
 * [de.polocloud.proto.EventContext.getEventName] — to the concrete class it decodes to.
 *
 * This is deliberately an open, mutable registry rather than a closed allow-list: a plugin
 * or addon can define its own `@Serializable` [Event] subclass and use it without ever
 * touching this module. [EventCodec.encode] registers an event's class on first use, and
 * [de.polocloud.api.event.EventService.subscribe] does the same for the type it listens
 * for, so [classFor] can resolve a name back to a class in whichever JVM produced or
 * consumes it — the only requirement is that that JVM has the class on its classpath.
 */
object EventRegistry {

    private val classesByName = ConcurrentHashMap<String, Class<out Event>>()

    init {
        register(ServerStartEvent::class.java)
        register(ServerStoppedEvent::class.java)
        register(ServiceOnlineEvent::class.java)
        register(GroupUpdatedEvent::class.java)
        register(PlayerCountChangedEvent::class.java)
        register(PlayerJoinEvent::class.java)
        register(PlayerSwitchEvent::class.java)
        register(PlayerDisconnectEvent::class.java)
        register(PlayerKickEvent::class.java)
        register(PlayerKickRequestEvent::class.java)
        register(PlayerSendRequestEvent::class.java)
    }

    /** Makes [type] resolvable by its simple name via [classFor]. Idempotent. */
    fun register(type: Class<out Event>) {
        classesByName[type.simpleName] = type
    }

    /** Returns the class registered under [name], or `null` if this JVM has never seen it. */
    fun classFor(name: String): Class<out Event>? = classesByName[name]
}
