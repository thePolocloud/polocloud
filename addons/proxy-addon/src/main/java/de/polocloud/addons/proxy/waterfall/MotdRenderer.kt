package de.polocloud.addons.proxy.waterfall

import de.polocloud.addons.proxy.MotdItemConfig
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.core.MotdComposer
import de.polocloud.addons.proxy.core.MotdItemBuilder
import de.polocloud.addons.proxy.core.PlayerCountResolver
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.ServerPing
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.ItemTag
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Item
import net.md_5.bungee.chat.ComponentSerializer

/**
 * Waterfall/BungeeCord counterpart of [de.polocloud.addons.proxy.velocity.MotdRenderer] — same
 * behaviour (animated two-line MOTD, maintenance override, optional item-hover tooltip), sharing
 * the same platform-neutral line/item composition rules ([MotdComposer]/[MotdItemBuilder]) but
 * built against BungeeCord's `net.md_5.bungee.api.chat` component API instead of Adventure.
 */
object MotdRenderer {

    fun render(proxy: ProxyServer, config: ProxyConfig, response: ServerPing): ServerPing {
        val (online, max) = PlayerCountResolver.resolve(config.playerCount, proxy::getOnlineCount) { proxy.config.playerLimit }
        val lines = MotdComposer.resolveLines(config, online, max) ?: return response

        val description = TextComponent(*TextComponent.fromLegacyText(lines.firstLine + "\n" + lines.secondLine))

        if (config.motd.item.enabled) {
            hoverItem(config.motd.item)?.let { description.hoverEvent = it }
        }

        response.setDescriptionComponent(description)
        response.players = ServerPing.Players(max, online, response.players?.sample ?: emptyArray())
        return response
    }

    /**
     * Builds an item-preview tooltip for [item] via BungeeCord's `HoverEvent.Action.SHOW_ITEM` —
     * the same trick [de.polocloud.addons.proxy.velocity.MotdRenderer.hoverItem] does through
     * Adventure. [ItemTag.ofNbt] takes a raw SNBT string directly, so unlike the Velocity side
     * (which has to hand-build a `BinaryTagHolder`-compatible NBT compound), only
     * [MotdItemBuilder.buildNbt]'s `display` tag needs manual SNBT-string quoting here — and
     * that text itself is still produced by [ComponentSerializer], never by hand. Also validates
     * [item.material][MotdItemConfig.material] the same way the Velocity side's `Key.key` does
     * (Bungee's `Item` has no such validation of its own), so a malformed id skips the tooltip
     * on both platforms instead of only on Velocity.
     */
    private fun hoverItem(item: MotdItemConfig): HoverEvent? {
        val namespaced = MotdItemBuilder.namespacedMaterial(item.material)
        if (!MotdItemBuilder.isValidNamespacedMaterial(namespaced)) return null

        val nbt = MotdItemBuilder.buildNbt(item) { text -> ComponentSerializer.toString(TextComponent(*TextComponent.fromLegacyText(text))) }
        val content = Item(namespaced, item.count.coerceAtLeast(1), ItemTag.ofNbt(nbt))
        return HoverEvent(HoverEvent.Action.SHOW_ITEM, content)
    }
}
