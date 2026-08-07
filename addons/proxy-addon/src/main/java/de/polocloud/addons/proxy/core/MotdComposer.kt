package de.polocloud.addons.proxy.core

import de.polocloud.addons.proxy.AnimationFrames
import de.polocloud.addons.proxy.ProxyConfig
import de.polocloud.addons.proxy.ProxyPlaceholders

/** Platform-neutral, fully placeholder-resolved MOTD lines, per [MotdComposer.resolveLines]. */
data class MotdLines(val firstLine: String, val secondLine: String)

/**
 * Composes the platform-neutral (plain, legacy-coded text) parts of the MOTD from the current
 * [ProxyConfig] — the animation-frame/maintenance-override text and its optional item hover
 * tooltip's SNBT. Turning that neutral output into an actual chat component (Adventure
 * `Component` on Velocity, BungeeCord `BaseComponent[]` on Waterfall) is left to each platform's
 * own `MotdRenderer`, since that's the one part that genuinely can't be shared.
 */
object MotdComposer {

    /** Resolves this tick's `%online%`/`%max%`-substituted MOTD line pair, honoring [ProxyConfig.maintenance]'s override. `null` when the MOTD feature itself is disabled. */
    fun resolveLines(config: ProxyConfig, online: Int, max: Int): MotdLines? {
        val motd = config.motd
        if (!motd.enabled) return null

        val (firstLine, secondLine) = if (config.maintenance.enabled) {
            config.maintenance.motdFirstLine to config.maintenance.motdSecondLine
        } else {
            (AnimationFrames.current(motd.firstLine, motd.tickIntervalMillis) ?: "") to
                (AnimationFrames.current(motd.secondLine, motd.tickIntervalMillis) ?: "")
        }

        return MotdLines(
            ProxyPlaceholders.resolve(firstLine, online, max),
            ProxyPlaceholders.resolve(secondLine, online, max),
        )
    }
}
