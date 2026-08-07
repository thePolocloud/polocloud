package de.polocloud.addons.proxy.waterfall.command

import de.polocloud.addons.proxy.Messages
import de.polocloud.addons.proxy.PROXY_ADDON_MANAGED_PERMISSION
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.config.ReloadableConfig
import de.polocloud.addons.proxy.core.ProxyCommandCore
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
 * default of hiding the command entirely. Shares argument parsing/dispatch with the Velocity
 * side via [ProxyCommandCore]; this only sends BungeeCord's `BaseComponent[]` and
 * enumerates/kicks players.
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

        when (val outcome = ProxyCommandCore.execute(args.toList(), config, messages.current())) {
            is ProxyCommandCore.Outcome.SendMessage -> sender.sendMessage(*TextComponent.fromLegacyText(outcome.message))
            is ProxyCommandCore.Outcome.MaintenanceChanged -> {
                sender.sendMessage(*TextComponent.fromLegacyText(outcome.message))

                // PostLoginEvent only gates *future* joins — already-connected non-bypass
                // players must be dropped explicitly the moment maintenance turns on, or they'd
                // stay on despite it.
                if (outcome.enabled) {
                    val maintenance = config.current().maintenance
                    val kick = TextComponent.fromLegacyText(maintenance.kickMessage.joinToString("\n"))
                    proxy.players
                        .filter { !it.hasPermission(maintenance.bypassPermission) }
                        .forEach { it.disconnect(*kick) }
                }
            }
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> = ProxyCommandCore.suggest(args.toList())
}
