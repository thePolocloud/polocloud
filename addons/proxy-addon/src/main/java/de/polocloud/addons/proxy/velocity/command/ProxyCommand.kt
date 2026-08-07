package de.polocloud.addons.proxy.velocity.command

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.ProxyServer
import de.polocloud.addons.proxy.Messages
import de.polocloud.addons.proxy.PROXY_ADDON_MANAGED_PERMISSION
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.config.ReloadableConfig
import de.polocloud.addons.proxy.core.ProxyCommandCore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * `/proxy maintenance [on|off]` — no argument toggles. Always registered with `hasPermission =
 * true` so [PROXY_ADDON_MANAGED_PERMISSION] can be checked explicitly inside [execute] and send
 * [Messages.noPermission] instead of Velocity's silent default (command appears not to exist).
 * Argument parsing/dispatch lives in [ProxyCommandCore]; this only sends Adventure [Component]s
 * and enumerates/kicks players, the two things that need Velocity's own API.
 */
class ProxyCommand(
    private val server: ProxyServer,
    private val config: ReloadableConfig<ProxyConfig>,
    private val messages: ReloadableConfig<Messages>,
) : SimpleCommand {

    private val legacy = LegacyComponentSerializer.legacySection()

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        if (!source.hasPermission(PROXY_ADDON_MANAGED_PERMISSION)) {
            source.sendMessage(legacy.deserialize(messages.current().noPermission))
            return
        }

        when (val outcome = ProxyCommandCore.execute(invocation.arguments().toList(), config, messages.current())) {
            is ProxyCommandCore.Outcome.SendMessage -> source.sendMessage(legacy.deserialize(outcome.message))
            is ProxyCommandCore.Outcome.MaintenanceChanged -> {
                source.sendMessage(legacy.deserialize(outcome.message))

                // LoginEvent only gates *future* joins — already-connected non-bypass players
                // must be dropped explicitly the moment maintenance turns on, or they'd stay on
                // despite it.
                if (outcome.enabled) {
                    val maintenance = config.current().maintenance
                    val kick = joinLines(maintenance.kickMessage)
                    server.allPlayers
                        .filter { !it.hasPermission(maintenance.bypassPermission) }
                        .forEach { it.disconnect(kick) }
                }
            }
        }
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean = true

    override fun suggest(invocation: SimpleCommand.Invocation): List<String> = ProxyCommandCore.suggest(invocation.arguments().toList())

    private fun joinLines(lines: List<String>): Component {
        if (lines.isEmpty()) return Component.empty()
        return lines.map(legacy::deserialize).reduce { acc, line -> acc.append(Component.newline()).append(line) }
    }
}
