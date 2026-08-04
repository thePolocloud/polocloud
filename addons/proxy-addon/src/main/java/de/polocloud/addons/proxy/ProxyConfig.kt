package de.polocloud.addons.proxy

import kotlinx.serialization.Serializable

@Serializable
data class ProxyConfig(
    val tablist: TablistConfig = TablistConfig(),
    val motd: MotdConfig = MotdConfig(),
    val playerCount: PlayerCountConfig = PlayerCountConfig(),
    val maintenance: MaintenanceConfig = MaintenanceConfig(),
)

/**
 * The tab list header/footer shown to every connected player. [header]/[footer] are each a
 * list of *frames* — one frame is a full set of lines shown at once — cycled every
 * [tickIntervalMillis] via [de.polocloud.addons.proxy.velocity.TablistRenderer] to animate; a
 * single frame is just a static header/footer. Lines resolve `%player%`/`%server%`/`%online%`/`%max%`
 * via [ProxyPlaceholders].
 */
@Serializable
data class TablistConfig(
    val enabled: Boolean = true,
    val header: List<List<String>> = listOf(
        listOf(
            "",
            "§b§lPOLOCLOUD",
            "§7Welcome, §f%player%§7!",
            "",
        ),
    ),
    val footer: List<List<String>> = listOf(
        listOf(
            "§7Online: §a%online%§7/§a%max%",
            "§7Server: §f%server%",
            "",
        ),
    ),
    val tickIntervalMillis: Long = 1000L,
)

/**
 * The server-list MOTD. [firstLine]/[secondLine] are each a list of animation frames (one line
 * of text per frame), cycled every [tickIntervalMillis]. [item] optionally attaches an item
 * tooltip to the MOTD text via Adventure's `HoverEvent.showItem` — hovering the server entry
 * in the multiplayer list then previews an item, the same trick some networks use as a logo.
 * Overridden entirely by [MaintenanceConfig.motdFirstLine]/[MaintenanceConfig.motdSecondLine]
 * while maintenance is active.
 */
@Serializable
data class MotdConfig(
    val enabled: Boolean = true,
    val firstLine: List<String> = listOf("§b§lPolocloud §8» §7Welcome to the network!"),
    val secondLine: List<String> = listOf("§7Online: §a%online%§8/§a%max% §8| §7Have fun!"),
    val tickIntervalMillis: Long = 1500L,
    val item: MotdItemConfig = MotdItemConfig(),
)

/** @param material `minecraft:`-namespaced (or bare, `minecraft:` is assumed) item id shown as the MOTD hover preview. */
@Serializable
data class MotdItemConfig(
    val enabled: Boolean = false,
    val material: String = "minecraft:nether_star",
    val count: Int = 1,
    val name: String = "§bPolocloud",
    val lore: List<String> = listOf("§7A cloud system for Minecraft networks."),
)

@Serializable
enum class PlayerCountMode {
    /** Only players connected to this exact proxy instance. */
    LOCAL,
    /** Summed across every currently running service in the whole cluster — the same total on every proxy, satisfying a network-wide count. */
    NETWORK,
    /** A fixed, manually configured number, ignoring real connections entirely. */
    FIXED,
}

/**
 * How the `%online%`/`%max%` placeholders (MOTD, tab list) are computed — see
 * [de.polocloud.addons.proxy.velocity.PlayerCountResolver]. [maxOverride] (when `> 0`) replaces
 * the computed maximum in [PlayerCountMode.LOCAL]/[PlayerCountMode.NETWORK] mode; leave it `0`
 * to use the real value.
 */
@Serializable
data class PlayerCountConfig(
    val mode: PlayerCountMode = PlayerCountMode.NETWORK,
    val fixedOnline: Int = 0,
    val fixedMax: Int = 0,
    val maxOverride: Int = 0,
)

/**
 * `/proxy maintenance` toggles [enabled] and persists it (see [de.polocloud.addons.proxy.config.ReloadableConfig.update]),
 * so a proxy restarted mid-maintenance stays in maintenance. A player without [bypassPermission]
 * is denied login with [kickMessage] while active — see [de.polocloud.addons.proxy.velocity.VelocityProxyBootstrap.onLogin] —
 * and already-connected non-bypass players are disconnected the moment maintenance turns on.
 * The MOTD switches to [motdFirstLine]/[motdSecondLine] instead of the normal animated one.
 */
@Serializable
data class MaintenanceConfig(
    val enabled: Boolean = false,
    val bypassPermission: String = "polocloud.addon.proxy.maintenance.bypass",
    val kickMessage: List<String> = listOf(
        "§c§lMaintenance",
        "",
        "§7The server is currently in maintenance mode.",
        "§7Please try again later.",
    ),
    val motdFirstLine: String = "§c§lMaintenance",
    val motdSecondLine: String = "§7The server is currently unreachable.",
)
