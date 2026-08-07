package de.polocloud.addons.proxy.core

import de.polocloud.addons.proxy.MotdItemConfig

/**
 * Shared half of the MOTD hover-item tooltip built by both platforms' `MotdRenderer` — hovering
 * the server entry in a client's multiplayer list previews an item, the trick some networks use
 * as a logo (Velocity via Adventure's `HoverEvent.showItem`, Waterfall via BungeeCord's
 * `HoverEvent.Action.SHOW_ITEM`). Everything platform-specific chat-component types touch stays
 * in each platform's own `MotdRenderer`; this only builds the raw `{display:{Name:...,Lore:[...]}}`
 * SNBT compound both of them need, since that string-escaping is the one part actually worth
 * getting wrong only once.
 */
object MotdItemBuilder {

    /** A resource-location-shaped `namespace:path` id — Minecraft item ids only ever use `[a-z0-9_.-]` for the namespace and `[a-z0-9_./-]` for the path. */
    private val NAMESPACED_ID_REGEX = Regex("[a-z0-9_.-]+:[a-z0-9_./-]+")

    /** Namespaces [material] as `minecraft:xxx` if it isn't already namespaced. */
    fun namespacedMaterial(material: String): String =
        if (material.contains(':')) material else "minecraft:${material.lowercase()}"

    /** Whether [namespaced] is a well-formed resource location — an invalid id should skip the hover tooltip rather than let a malformed one reach either platform's item-lookup. */
    fun isValidNamespacedMaterial(namespaced: String): Boolean = NAMESPACED_ID_REGEX.matches(namespaced)

    /**
     * Builds the SNBT `{display:{Name:...,Lore:[...]}}` compound for [item]'s hover tooltip.
     * [jsonify] turns a legacy-coded string into its JSON text-component form — supplied by
     * each platform via its own serializer (Adventure's `GsonComponentSerializer`, BungeeCord's
     * `ComponentSerializer`) — so this stays free of either chat-component API. The *only*
     * character that can still break out of a `'`-delimited SNBT string is a literal `'` (not a
     * JSON special character, so neither serializer ever escapes it), hence that's the only
     * thing escaped here, on top of [jsonify]'s own JSON escaping.
     */
    fun buildNbt(item: MotdItemConfig, jsonify: (String) -> String): String {
        fun snbtString(text: String): String = "'" + jsonify(text).replace("'", "\\'") + "'"

        return buildString {
            append("{display:{Name:").append(snbtString(item.name))
            if (item.lore.isNotEmpty()) {
                append(",Lore:[")
                append(item.lore.joinToString(",") { snbtString(it) })
                append(']')
            }
            append("}}")
        }
    }
}
