package dev.httpmarco.polocloud.addons.proxy.platform.velocity.events

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerPing
import dev.httpmarco.polocloud.addons.proxy.ProxyConfigAccessor
import dev.httpmarco.polocloud.addons.proxy.platform.velocity.VelocityPlatform
import dev.httpmarco.polocloud.sdk.java.Polocloud
import net.kyori.adventure.text.minimessage.MiniMessage

class VelocityMotdUpdater (
    private val platform: VelocityPlatform,
    private val server: ProxyServer,
    private val config: ProxyConfigAccessor
) {

    private val polocloudVersion: String = System.getenv("polocloud-version") ?: "unknown"

    @Subscribe
    fun onProxyPing(event: ProxyPingEvent) {
        val group = Polocloud.instance().groupProvider().find(platform.proxyAddon().poloService.groupName)!!
        val ping = event.ping

        if(group.properties["maintenance"]?.asBoolean ?: false) {
            // maintenance mode is enabled, use maintenance MOTD
            if(!config.maintenanceMotd().enabled) {
                // maintenance motd is disabled
                return
            }

            val motdLines = (config.maintenanceMotd().lineOne + "\n" + config.maintenanceMotd().lineTwo)
                .replace("%version%", polocloudVersion)
                .replace("%online_players%", server.playerCount.toString())

            val newVersionName = config.maintenanceMotd().pingMessage

            // Build from existing ping to preserve favicon, sample players, etc.
            val builder = try {
                ping.asBuilder()
            } catch (_: Throwable) {
                ServerPing.builder()
                    .favicon(ping.favicon.orElse(null))
                    .maximumPlayers(ping.players.orElse(null)?.max ?: 0)
                    .onlinePlayers(ping.players.orElse(null)?.online ?: 0)
                    .version(ping.version)
            }

            event.ping = builder
                .description(MiniMessage.miniMessage().deserialize(motdLines))
                .version(ServerPing.Version(ping.version.protocol, newVersionName))
                .build()
            return
        }

        if(!config.motd().enabled) {
            // motd module is disabled
            return
        }

        // Build from existing ping to keep favicon and other fields
        val builder = try {
            ping.asBuilder()
        } catch (_: Throwable) {
            ServerPing.builder()
                .favicon(ping.favicon.orElse(null))
                .maximumPlayers(ping.players.orElse(null)?.max ?: 0)
                .onlinePlayers(ping.players.orElse(null)?.online ?: 0)
                .version(ping.version)
        }

        var motdLines = config.motd().lineOne + "\n" + config.motd().lineTwo
        motdLines = motdLines
            .replace("%version%", polocloudVersion)
            .replace("%online_players%", server.playerCount.toString())

        event.ping = builder
            .description(MiniMessage.miniMessage().deserialize(motdLines))
            .version(ping.version)
            .build()

    }
}
