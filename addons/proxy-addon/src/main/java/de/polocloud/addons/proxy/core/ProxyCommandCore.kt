package de.polocloud.addons.proxy.core

import de.polocloud.addons.proxy.Messages
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.config.ReloadableConfig

/**
 * Argument parsing/dispatch shared by both platforms' `/proxy maintenance [on|off]` command —
 * everything that doesn't depend on how a platform sends a message or enumerates/kicks players.
 * Permission checking is deliberately *not* part of this: only the caller knows how to check
 * [de.polocloud.addons.proxy.PROXY_ADDON_MANAGED_PERMISSION] against its own command-source type
 * and send [Messages.noPermission] on failure.
 */
object ProxyCommandCore {

    /** What a platform's `ProxyCommand` should do after [execute] parsed/applied one invocation. */
    sealed interface Outcome {
        /** Send [message] to the command source; nothing else changed. */
        data class SendMessage(val message: String) : Outcome

        /**
         * Maintenance mode was actually toggled to [enabled] — [ProxyConfig.maintenance] was
         * already persisted via [ReloadableConfig.update] by the time this is returned. Send
         * [message], and if [enabled], disconnect every currently-connected, non-bypass player
         * (the config change alone only gates *future* logins).
         */
        data class MaintenanceChanged(val enabled: Boolean, val message: String) : Outcome
    }

    /** Tab-completion suggestions for `args`, shared by both platforms' `/proxy` command. */
    fun suggest(args: List<String>): List<String> = when (args.size) {
        0, 1 -> listOf("maintenance").filter { it.startsWith(args.getOrElse(0) { "" }, ignoreCase = true) }
        2 -> listOf("on", "off").filter { it.startsWith(args[1], ignoreCase = true) }
        else -> emptyList()
    }

    /** Parses and applies `/proxy <args>` — assumes the permission check already passed. */
    fun execute(args: List<String>, config: ReloadableConfig<ProxyConfig>, messages: Messages): Outcome {
        if (args.getOrNull(0)?.lowercase() != "maintenance") return Outcome.SendMessage(messages.usage)

        val target = when (args.getOrNull(1)?.lowercase()) {
            "on" -> true
            "off" -> false
            null -> !config.current().maintenance.enabled
            else -> return Outcome.SendMessage(messages.usage)
        }

        val current = config.current()
        if (current.maintenance.enabled == target) {
            val already = if (target) messages.maintenanceAlreadyEnabled else messages.maintenanceAlreadyDisabled
            return Outcome.SendMessage(already)
        }

        config.update(current.copy(maintenance = current.maintenance.copy(enabled = target)))
        val message = if (target) messages.maintenanceEnabled else messages.maintenanceDisabled
        return Outcome.MaintenanceChanged(target, message)
    }
}
