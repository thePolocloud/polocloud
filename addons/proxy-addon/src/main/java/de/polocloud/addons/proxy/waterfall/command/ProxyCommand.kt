package de.polocloud.addons.proxy.waterfall.command

import de.polocloud.addons.proxy.Messages
import de.polocloud.addons.proxy.PROXY_ADDON_MANAGED_PERMISSION
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.config.ReloadableConfig
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor

/**
 * Waterfall/BungeeCord counterpart of [de.polocloud.addons.proxy.velocity.command.ProxyCommand] —
 * same `/proxy maintenance [on|off]` behaviour. Registered with no `permission` (Bungee's default
 * [hasPermission] then just returns `true`) so [PROXY_ADDON_MANAGED_PERMISSION] can be checked
 * explicitly inside [execute] and send [Messages.noPermission], instead of Bungee's silent
 * default of hiding the command entirely.
 */
class ProxyCommand(
    private val proxy: ProxyServer,
    private val config: ReloadableConfig<ProxyConfig>,
    private val messages: ReloadableConfig<Messages>,
) : Command("proxy"), TabExecutor {

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission(PROXY_ADDON_MANAGED_PERMISSION)) {
            sender.sendMessage(*TextComponent.fromLegacyText(messages.current().noPermission))
            return
        }

        if (args.getOrNull(0)?.lowercase() != "maintenance") {
            sender.sendMessage(*TextComponent.fromLegacyText(messages.current().usage))
            return
        }

        when (args.getOrNull(1)?.lowercase()) {
            "on" -> setMaintenance(sender, true)
            "off" -> setMaintenance(sender, false)
            null -> setMaintenance(sender, !config.current().maintenance.enabled)
            else -> sender.sendMessage(*TextComponent.fromLegacyText(messages.current().usage))
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> = when (args.size) {
        0, 1 -> listOf("maintenance").filter { it.startsWith(args.getOrElse(0) { "" }, ignoreCase = true) }
        2 -> listOf("on", "off").filter { it.startsWith(args[1], ignoreCase = true) }
        else -> emptyList()
    }

    private fun setMaintenance(sender: CommandSender, enabled: Boolean) {
        val current = config.current()
        if (current.maintenance.enabled == enabled) {
            val already = if (enabled) messages.current().maintenanceAlreadyEnabled else messages.current().maintenanceAlreadyDisabled
            sender.sendMessage(*TextComponent.fromLegacyText(already))
            return
        }

        config.update(current.copy(maintenance = current.maintenance.copy(enabled = enabled)))
        sender.sendMessage(*TextComponent.fromLegacyText(if (enabled) messages.current().maintenanceEnabled else messages.current().maintenanceDisabled))

        // PostLoginEvent only gates *future* joins — already-connected non-bypass players must be
        // dropped explicitly the moment maintenance turns on, or they'd stay on despite it.
        if (enabled) {
            val kick = TextComponent.fromLegacyText(current.maintenance.kickMessage.joinToString("\n"))
            proxy.players
                .filter { !it.hasPermission(current.maintenance.bypassPermission) }
                .forEach { it.disconnect(*kick) }
        }
    }
}
