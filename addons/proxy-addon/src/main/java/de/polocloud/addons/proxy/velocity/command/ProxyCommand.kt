package de.polocloud.addons.proxy.velocity.command

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.ProxyServer
import de.polocloud.addons.proxy.Messages
import de.polocloud.addons.proxy.PROXY_ADDON_MANAGED_PERMISSION
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.config.ReloadableConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * `/proxy maintenance [on|off]` — no argument toggles. Always registered with `hasPermission =
 * true` so [PROXY_ADDON_MANAGED_PERMISSION] can be checked explicitly inside [execute] and send
 * [Messages.noPermission] instead of Velocity's silent default (command appears not to exist).
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

        val args = invocation.arguments()
        if (args.getOrNull(0)?.lowercase() != "maintenance") {
            source.sendMessage(legacy.deserialize(messages.current().usage))
            return
        }

        when (args.getOrNull(1)?.lowercase()) {
            "on" -> setMaintenance(source, true)
            "off" -> setMaintenance(source, false)
            null -> setMaintenance(source, !config.current().maintenance.enabled)
            else -> source.sendMessage(legacy.deserialize(messages.current().usage))
        }
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean = true

    override fun suggest(invocation: SimpleCommand.Invocation): List<String> {
        val args = invocation.arguments()
        return when (args.size) {
            0, 1 -> listOf("maintenance").filter { it.startsWith(args.getOrElse(0) { "" }, ignoreCase = true) }
            2 -> listOf("on", "off").filter { it.startsWith(args[1], ignoreCase = true) }
            else -> emptyList()
        }
    }

    private fun setMaintenance(source: CommandSource, enabled: Boolean) {
        val current = config.current()
        if (current.maintenance.enabled == enabled) {
            val already = if (enabled) messages.current().maintenanceAlreadyEnabled else messages.current().maintenanceAlreadyDisabled
            source.sendMessage(legacy.deserialize(already))
            return
        }

        config.update(current.copy(maintenance = current.maintenance.copy(enabled = enabled)))
        source.sendMessage(legacy.deserialize(if (enabled) messages.current().maintenanceEnabled else messages.current().maintenanceDisabled))

        // LoginEvent only gates *future* joins — already-connected non-bypass players must be
        // dropped explicitly the moment maintenance turns on, or they'd stay on despite it.
        if (enabled) {
            val kick = joinLines(current.maintenance.kickMessage)
            server.allPlayers
                .filter { !it.hasPermission(current.maintenance.bypassPermission) }
                .forEach { it.disconnect(kick) }
        }
    }

    private fun joinLines(lines: List<String>): Component {
        if (lines.isEmpty()) return Component.empty()
        return lines.map(legacy::deserialize).reduce { acc, line -> acc.append(Component.newline()).append(line) }
    }
}
