package de.polocloud.addons.proxy.velocity

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerPing
import de.polocloud.addons.proxy.AnimationFrames
import de.polocloud.addons.proxy.MotdItemConfig
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.ProxyPlaceholders
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.api.BinaryTagHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Builds the description shown in a client's multiplayer server list, from the current
 * [ProxyConfig]. Switches to [de.polocloud.addons.proxy.MaintenanceConfig]'s MOTD lines instead
 * of the normal animated ones while maintenance is active.
 */
object MotdRenderer {

    private val legacy = LegacyComponentSerializer.legacySection()
    private val gson = GsonComponentSerializer.gson()

    fun render(server: ProxyServer, config: ProxyConfig, ping: ServerPing): ServerPing {
        val motd = config.motd
        if (!motd.enabled) return ping

        val (online, max) = PlayerCountResolver.resolve(server, config.playerCount)

        val (firstLine, secondLine) = if (config.maintenance.enabled) {
            config.maintenance.motdFirstLine to config.maintenance.motdSecondLine
        } else {
            (AnimationFrames.current(motd.firstLine, motd.tickIntervalMillis) ?: "") to
                (AnimationFrames.current(motd.secondLine, motd.tickIntervalMillis) ?: "")
        }

        var description: Component = legacy.deserialize(ProxyPlaceholders.resolve(firstLine, online, max))
            .append(Component.newline())
            .append(legacy.deserialize(ProxyPlaceholders.resolve(secondLine, online, max)))

        if (motd.item.enabled) {
            hoverItem(motd.item)?.let { description = description.hoverEvent(it) }
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
     * `adventure-nbt` artifact, which isn't transitively on this addon's classpath), so the
     * `display` tag is hand-built — but the *text* inside it is produced by [gson], never by
     * hand, so the only escaping this code does itself is for the SNBT string delimiter.
     */
    private fun hoverItem(item: MotdItemConfig): HoverEvent<HoverEvent.ShowItem>? {
        val namespaced = if (item.material.contains(':')) item.material else "minecraft:${item.material.lowercase()}"
        val key = runCatching { Key.key(namespaced) }.getOrNull() ?: return null

        val nbt = buildString {
            append("{display:{Name:").append(snbtString(item.name))
            if (item.lore.isNotEmpty()) {
                append(",Lore:[")
                append(item.lore.joinToString(",") { snbtString(it) })
                append(']')
            }
            append("}}")
        }

        return HoverEvent.showItem(key, item.count.coerceAtLeast(1), BinaryTagHolder.binaryTagHolder(nbt))
    }

    /**
     * Wraps a legacy-coded [text] as a JSON text component (via [gson], so no manual JSON
     * escaping is needed) and quotes it as a single-quoted SNBT string literal. The JSON's own
     * backslash-escapes (`\"`, `\\`, ...) are left untouched — SNBT's quoted-string escaping is
     * the same backslash-introduced scheme, so they already mean the right thing there too. The
     * *only* character that can still break out of a `'`-delimited SNBT string is a literal `'`
     * (not a JSON special character, so [gson] never escapes it), hence that's the only thing
     * escaped here.
     */
    private fun snbtString(text: String): String {
        val json = gson.serialize(legacy.deserialize(text))
        return "'" + json.replace("'", "\\'") + "'"
    }
}
