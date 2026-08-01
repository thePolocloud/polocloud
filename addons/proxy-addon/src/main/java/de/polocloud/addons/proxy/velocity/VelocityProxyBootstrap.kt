package de.polocloud.addons.proxy.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import de.polocloud.addons.proxy.Messages
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.config.ReloadableConfig
import de.polocloud.addons.proxy.config.SingleDocumentStorage
import de.polocloud.addons.proxy.velocity.command.ProxyCommand
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.slf4j.Logger
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Plugin(
    id = "polocloud-proxy-addon",
    name = "Polocloud Proxy Addon Plugin",
    version = "3.0.0",
    description = "Animated tab list, MOTD, maintenance mode and network-wide player count",
    authors = ["polocloud"],
)
class VelocityProxyBootstrap @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path,
) {

    private val legacy = LegacyComponentSerializer.legacySection()

    private lateinit var config: ReloadableConfig<ProxyConfig>
    private lateinit var messages: ReloadableConfig<Messages>

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        config = ReloadableConfig(SingleDocumentStorage(dataDirectory.resolve("proxy.json"), ProxyConfig.serializer())) { ProxyConfig() }
        messages = ReloadableConfig(SingleDocumentStorage(dataDirectory.resolve("messages.json"), Messages.serializer())) { Messages() }

        val meta = server.commandManager.metaBuilder("proxy").plugin(this).build()
        server.commandManager.register(meta, ProxyCommand(server, config, messages))

        server.scheduler.buildTask(this, Runnable { TablistRenderer.render(server, config.current()) })
            .repeat(TABLIST_TICK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
            .schedule()

        logger.info("Proxy addon enabled")
    }

    @Subscribe
    fun onPing(event: ProxyPingEvent) {
        event.ping = MotdRenderer.render(server, config.current(), event.ping)
    }

    @Subscribe
    fun onLogin(event: LoginEvent) {
        val maintenance = config.current().maintenance
        if (!maintenance.enabled) return
        if (event.player.hasPermission(maintenance.bypassPermission)) return

        val reason = maintenance.kickMessage
            .map { legacy.deserialize(it) }
            .reduceOrNull { acc, line -> acc.append(Component.newline()).append(line) }
            ?: Component.empty()

        event.setResult(ResultedEvent.ComponentResult.denied(reason))
    }

    private companion object {
        const val TABLIST_TICK_INTERVAL_MILLIS = 500L
    }
}
