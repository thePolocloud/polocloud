package de.polocloud.addons.notify

import de.polocloud.shared.service.Service

/**
 * Platform-agnostic core of the notify addon (see [de.polocloud.addons.hub.HubAddon] for the
 * same shape). Owns the broadcast-selection logic; a platform module (Velocity today) only
 * has to translate [onlinePlayers]/[hasPermission]/[sendMessage] into its own proxy API.
 */
abstract class NotifyAddon<P> {

    /** Every player currently connected to the proxy. */
    abstract fun onlinePlayers(): Collection<P>

    /** Whether [player] holds [permission]. */
    abstract fun hasPermission(player: P, permission: String): Boolean

    /** Sends [message] to [player]. */
    abstract fun sendMessage(player: P, message: String)

    /** Sends [message] to every online player holding [NOTIFY_MANAGED_PERMISSION]. */
    fun broadcast(message: String) {
        onlinePlayers()
            .filter { hasPermission(it, NOTIFY_MANAGED_PERMISSION) }
            .forEach { sendMessage(it, message) }
    }

    fun notifyStarted(service: Service) =
        broadcast("§8[§aNotify§8] §7Service §a${service.name()} §7is now §aonline§7.")

    fun notifyStopped(service: Service) =
        broadcast("§8[§cNotify§8] §7Service §c${service.name()} §7has §cstopped§7.")
}
