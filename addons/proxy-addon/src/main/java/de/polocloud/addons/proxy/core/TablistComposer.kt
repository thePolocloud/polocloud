package de.polocloud.addons.proxy.core

import de.polocloud.addons.proxy.AnimationFrames
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.ProxyPlaceholders

/** This tick's header/footer animation frames, as still-unresolved-per-player lines — see [TablistComposer.resolveFrames]. */
data class TablistFrames(val header: List<String>, val footer: List<String>)

/**
 * Composes the platform-neutral (plain, legacy-coded text) parts of the tab list header/footer
 * from the current [ProxyConfig]. Turning the resolved lines into an actual chat component
 * (Adventure `Component` on Velocity, BungeeCord `BaseComponent[]` on Waterfall) — including how
 * multiple lines get joined into one — is left to each platform's own `TablistRenderer`.
 */
object TablistComposer {

    /** This tick's header/footer animation frames, or `null` if the tab list feature itself is disabled. */
    fun resolveFrames(config: ProxyConfig): TablistFrames? {
        val tablist = config.tablist
        if (!tablist.enabled) return null

        return TablistFrames(
            AnimationFrames.current(tablist.header, tablist.tickIntervalMillis) ?: emptyList(),
            AnimationFrames.current(tablist.footer, tablist.tickIntervalMillis) ?: emptyList(),
        )
    }

    /** Resolves [lines]' `%online%`/`%max%`/`%player%`/`%server%` placeholders for one specific player, still as plain per-line legacy-coded text. */
    fun resolveLines(lines: List<String>, online: Int, max: Int, player: String, server: String): List<String> =
        lines.map { ProxyPlaceholders.resolve(it, online, max, player, server) }
}
