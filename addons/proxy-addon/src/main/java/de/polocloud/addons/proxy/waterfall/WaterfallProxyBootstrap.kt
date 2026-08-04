package de.polocloud.addons.proxy.waterfall

import de.polocloud.addons.proxy.Messages
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.config.ReloadableConfig
import de.polocloud.addons.proxy.config.SingleDocumentStorage
import de.polocloud.addons.proxy.waterfall.command.ProxyCommand
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.event.ProxyPingEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import java.util.concurrent.TimeUnit

/**
 * Waterfall/BungeeCord counterpart of
 * [de.polocloud.addons.proxy.velocity.VelocityProxyBootstrap] — same animated tab list, MOTD,
 * maintenance mode and network-wide player count, wired up against the Bungee plugin API.
 * Registered via `bungee.yml`'s `main`, so — unlike Velocity — no plugin annotation is needed.
 */
class WaterfallProxyBootstrap : Plugin(), Listener {

    private lateinit var config: ReloadableConfig<ProxyConfig>
    private lateinit var messages: ReloadableConfig<Messages>

    override fun onEnable() {
        config = ReloadableConfig(SingleDocumentStorage(dataFolder.toPath().resolve("proxy.json"), ProxyConfig.serializer())) { ProxyConfig() }
        messages = ReloadableConfig(SingleDocumentStorage(dataFolder.toPath().resolve("messages.json"), Messages.serializer())) { Messages() }

        proxy.pluginManager.registerListener(this, this)
        proxy.pluginManager.registerCommand(this, ProxyCommand(proxy, config, messages))

        proxy.scheduler.schedule(this, Runnable { TablistRenderer.render(proxy, config.current()) }, 0, TABLIST_TICK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)

        logger.info("Proxy addon enabled")
    }

    @EventHandler
    fun onPing(event: ProxyPingEvent) {
        event.response = MotdRenderer.render(proxy, config.current(), event.response)
    }

    @EventHandler
    fun onPostLogin(event: PostLoginEvent) {
        val maintenance = config.current().maintenance
        if (!maintenance.enabled) return

        val player = event.player
        if (player.hasPermission(maintenance.bypassPermission)) return

        player.disconnect(*TextComponent.fromLegacyText(maintenance.kickMessage.joinToString("\n")))
    }

    private companion object {
        const val TABLIST_TICK_INTERVAL_MILLIS = 500L
    }
}
