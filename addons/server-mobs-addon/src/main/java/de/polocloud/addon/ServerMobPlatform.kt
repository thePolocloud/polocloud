package de.polocloud.addon

import de.polocloud.shared.service.Service
import java.nio.file.Path

/**
 * A platform this addon can run on (Bukkit today — see [de.polocloud.addon.bukkit.BukkitServerMobPlatform];
 * further world-based platforms can be added the same way, mirroring
 * [de.polocloud.addons.sign.system.SignPlatform] in the sign-system addon). Owns no mob
 * logic itself: it only spawns/updates/removes the visual entity for a [ServerMob] and
 * exposes what the platform-agnostic [ServerMobAddon] needs to run.
 */
abstract class ServerMobPlatform {

    /** Where [ServerMobAddon] persists attached mobs across restarts. */
    abstract val dataDirectory: Path

    /** Spawns [mob]'s entity if it doesn't exist yet, and refreshes its display with [services]. */
    abstract fun render(mob: ServerMob, services: List<Service>)

    /** Removes [mob]'s visual entity, if any. */
    abstract fun remove(mob: ServerMob)

    /** Runs [task] repeatedly, roughly every [intervalTicks] (20 ticks ≈ 1s). Keeps the mob's display live. */
    abstract fun scheduleRepeating(intervalTicks: Long, task: () -> Unit)
}
