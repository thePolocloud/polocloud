package de.polocloud.addons.proxy.velocity

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerPing
import de.polocloud.addons.proxy.MotdItemConfig
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.core.MotdComposer
import de.polocloud.addons.proxy.core.MotdItemBuilder
import de.polocloud.addons.proxy.core.PlayerCountResolver
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.api.BinaryTagHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Builds the description shown in a client's multiplayer server list, from the current
 * [ProxyConfig]. Switches to [de.polocloud.addons.proxy.MaintenanceConfig]'s MOTD lines instead
 * of the normal animated ones while maintenance is active — the platform-neutral line/item
 * composition rules live in [MotdComposer]/[MotdItemBuilder]; this only turns their output into
 * Adventure [Component]s, Velocity's chat-component type.
 */
object MotdRenderer {

    private val legacy = LegacyComponentSerializer.legacySection()
    private val gson = GsonComponentSerializer.gson()

    fun render(server: ProxyServer, config: ProxyConfig, ping: ServerPing): ServerPing {
        val (online, max) = PlayerCountResolver.resolve(config.playerCount, server::getPlayerCount) { server.configuration.showMaxPlayers }
        val lines = MotdComposer.resolveLines(config, online, max) ?: return ping

        var description: Component = legacy.deserialize(lines.firstLine)
            .append(Component.newline())
            .append(legacy.deserialize(lines.secondLine))

        if (config.motd.item.enabled) {
            hoverItem(config.motd.item)?.let { description = description.hoverEvent(it) }
        }

        return ping.asBuilder()
            .description(description)
            .onlinePlayers(online)
            .maximumPlayers(max)
            .build()
    }

    /**
     * Builds an item-preview tooltip for [item] via Adventure's `HoverEvent.showItem` — hovering
     * the server entry in a client's multiplayer list then shows this item, the trick the user
     * asked for ("adventure 5 api um item in die motd zu packen"). [BinaryTagHolder] only takes
     * a raw SNBT string (no compound-tag builder is available without pulling in the separate
     * `adventure-nbt` artifact, which isn't transitively on this addon's classpath), so
     * [MotdItemBuilder.buildNbt] hand-builds the `display` tag — but the *text* inside it is
     * produced by [gson], never by hand.
     */
    private fun hoverItem(item: MotdItemConfig): HoverEvent<HoverEvent.ShowItem>? {
        val namespaced = MotdItemBuilder.namespacedMaterial(item.material)
        val key = runCatching { Key.key(namespaced) }.getOrNull() ?: return null

        val nbt = MotdItemBuilder.buildNbt(item) { text -> gson.serialize(legacy.deserialize(text)) }
        return HoverEvent.showItem(key, item.count.coerceAtLeast(1), BinaryTagHolder.binaryTagHolder(nbt))
    }
}
