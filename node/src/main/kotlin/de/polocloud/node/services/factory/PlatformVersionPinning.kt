package de.polocloud.node.services.factory

import de.polocloud.node.group.Group
import de.polocloud.node.group.GroupVersionPin
import de.polocloud.node.group.GroupVersionPinRepository
import de.polocloud.node.services.factory.platform.PlatformVersion
import org.slf4j.LoggerFactory

/**
 * Falls a group's service start back to the last build actually confirmed online (see
 * [de.polocloud.node.services.ping.ServicePingFactory.markOnline]) when the build
 * [de.polocloud.node.services.factory.platform.PlatformService] currently resolves as
 * "latest" for [Group.version] is the same one that just made this group crash-loop (see
 * [de.polocloud.node.services.queue.CrashLoopGuard]).
 *
 * Without this, a bad upstream build (Paper/etc. publishing one that fails to boot) gets
 * retried forever on every restart — the previous jar is still sitting untouched in
 * [de.polocloud.node.services.factory.process.PlatformJarCache] (nothing ever deletes an
 * old build's cache entry), there's just nothing that ever reaches back for it.
 */
object PlatformVersionPinning {

    private val logger = LoggerFactory.getLogger(PlatformVersionPinning::class.java)

    /** Returns [resolved], or [group]'s previously known-good build if [resolved] is its quarantined one. */
    fun resolve(group: Group, resolved: PlatformVersion): PlatformVersion {
        val pin = GroupVersionPinRepository.find(group.name) ?: return resolved
        if (pin.platformVersion != group.version) return resolved
        if (pin.badBuild != resolved.build) return resolved
        if (pin.goodBuild == 0 || pin.goodBuild == resolved.build) return resolved

        logger.warn(
            "{} build {} is quarantined for group '{}' (it crash-looped last time it was started) " +
                "— falling back to previously working build {}",
            resolved.version, resolved.build, group.name, pin.goodBuild,
        )
        // downloadUrl is intentionally left blank: falling back only makes sense because
        // that build's jar is still sitting in PlatformJarCache from when it last ran, so
        // the URL is never actually needed (see PlatformProcess.cachedJar).
        return resolved.copy(build = pin.goodBuild, downloadUrl = "")
    }

    /** Records that [build] of [group]'s currently configured version came online successfully. */
    fun recordSuccess(group: Group, build: Int) {
        val pin = existingOrNew(group)
        pin.platformVersion = group.version
        pin.goodBuild = build
        // The build that just came online can't still be the quarantined one.
        if (pin.badBuild == build) pin.badBuild = 0
        GroupVersionPinRepository.save(pin)
    }

    /** Records that [build] of [group]'s currently configured version crash-looped on start. */
    fun recordFailure(group: Group, build: Int) {
        val pin = existingOrNew(group)
        // Never quarantine a build already seen coming online successfully — one that
        // worked before and is merely flaky isn't something to permanently avoid.
        if (pin.goodBuild == build) return
        pin.platformVersion = group.version
        pin.badBuild = build
        GroupVersionPinRepository.save(pin)
    }

    private fun existingOrNew(group: Group) =
        GroupVersionPinRepository.find(group.name)
            ?: GroupVersionPin(group.name, group.version, goodBuild = 0, badBuild = 0)
}
