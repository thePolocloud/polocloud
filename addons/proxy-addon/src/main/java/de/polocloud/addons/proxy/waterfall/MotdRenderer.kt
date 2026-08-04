package de.polocloud.addons.proxy.waterfall

import de.polocloud.addons.proxy.AnimationFrames
import de.polocloud.addons.proxy.MotdItemConfig
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.ProxyPlaceholders
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.ServerPing
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.ItemTag
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Item
import net.md_5.bungee.chat.ComponentSerializer

/**
 * Waterfall/BungeeCord counterpart of [de.polocloud.addons.proxy.velocity.MotdRenderer] — same
 * behaviour (animated two-line MOTD, maintenance override, optional item-hover tooltip), built
 * against BungeeCord's `net.md_5.bungee.api.chat` component API instead of Adventure.
 */
object MotdRenderer {

    fun render(proxy: ProxyServer, config: ProxyConfig, response: ServerPing): ServerPing {
        val motd = config.motd
        if (!motd.enabled) return response

        val (online, max) = PlayerCountResolver.resolve(proxy, config.playerCount)

        val (firstLine, secondLine) = if (config.maintenance.enabled) {
            config.maintenance.motdFirstLine to config.maintenance.motdSecondLine
        } else {
            (AnimationFrames.current(motd.firstLine, motd.tickIntervalMillis) ?: "") to
                (AnimationFrames.current(motd.secondLine, motd.tickIntervalMillis) ?: "")
        }

        val resolved = ProxyPlaceholders.resolve(firstLine, online, max) + "\n" + ProxyPlaceholders.resolve(secondLine, online, max)
        val description = TextComponent(*TextComponent.fromLegacyText(resolved))

        if (motd.item.enabled) {
            hoverItem(motd.item)?.let { description.hoverEvent = it }
        }

        response.setDescriptionComponent(description)
        response.players = ServerPing.Players(max, online, response.players?.sample ?: emptyArray())
        return response
    }

    /**
     * Builds an item-preview tooltip for [item] via BungeeCord's `HoverEvent.Action.SHOW_ITEM` —
     * the same trick [de.polocloud.addons.proxy.velocity.MotdRenderer.hoverItem] does through
     * Adventure. [ItemTag.ofNbt] takes a raw SNBT string directly, so unlike the Velocity side
     * (which has to hand-build a `BinaryTagHolder`-compatible NBT compound), only the `display`
     * tag's `Name`/`Lore` text needs manual SNBT-string quoting here — and that text itself is
     * still produced by [ComponentSerializer], never by hand.
     */
    private fun hoverItem(item: MotdItemConfig): HoverEvent? {
        val namespaced = if (item.material.contains(':')) item.material else "minecraft:${item.material.lowercase()}"

        val nbt = buildString {
            append("{display:{Name:").append(snbtString(item.name))
            if (item.lore.isNotEmpty()) {
                append(",Lore:[")
                append(item.lore.joinToString(",") { snbtString(it) })
                append(']')
            }
            append("}}")
        }

        val content = Item(namespaced, item.count.coerceAtLeast(1), ItemTag.ofNbt(nbt))
        return HoverEvent(HoverEvent.Action.SHOW_ITEM, content)
    }

    /** Wraps a legacy-coded [text] as a single JSON text component and quotes it as an SNBT string literal — mirrors the Velocity side's `snbtString`. */
    private fun snbtString(text: String): String {
        val json = ComponentSerializer.toString(TextComponent(*TextComponent.fromLegacyText(text)))
        return "'" + json.replace("'", "\\'") + "'"
    }
}
